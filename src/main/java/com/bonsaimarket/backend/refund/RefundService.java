package com.bonsaimarket.backend.refund;

import com.bonsaimarket.backend.community.ContentAccess;
import com.bonsaimarket.backend.community.ContentAccess.ContentPage;
import com.bonsaimarket.backend.marketplace.*;
import com.bonsaimarket.backend.notification.NotificationOutbox;
import com.bonsaimarket.backend.order.Order;
import com.bonsaimarket.backend.order.OrderStatus;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Review and provider execution are separate. Only a verified result can finalize financial state. */
@Service
@Transactional(isolation=Isolation.READ_COMMITTED)
public class RefundService {
    public record Write(String reason,BigDecimal amount) {public Write(String reason){this(reason,null);}}
    public record Decision(String status,String reason,Long revision) {}
    public record Event(String status,String reason,LocalDateTime createdAt) {}
    public record Refund(long id,long orderId,String method,BigDecimal amount,String currency,String status,String reason,long revision,LocalDateTime updatedAt,List<Event> history) {}
    public record Overview(Refund refund,boolean canRequest,String blocker,Map<String,Object> sandboxWalletRefund) {
        public Overview(Refund refund,boolean canRequest,String blocker){this(refund,canRequest,blocker,null);}
    }
    private final JdbcTemplate db;
    private final EntityManager em;
    private final MarketplaceAccess access;
    private final com.bonsaimarket.backend.wallet.VndWallet wallet;
    private final com.bonsaimarket.backend.payment.VnpayGateway gateway;
    private final com.bonsaimarket.backend.shipping.ShipmentOrderGuard shipments;
    private final org.springframework.transaction.support.TransactionTemplate tx;
    public RefundService(JdbcTemplate db,EntityManager em,MarketplaceAccess access,com.bonsaimarket.backend.wallet.VndWallet wallet,com.bonsaimarket.backend.payment.VnpayGateway gateway,com.bonsaimarket.backend.shipping.ShipmentOrderGuard shipments,org.springframework.transaction.PlatformTransactionManager manager){this.db=db;this.em=em;this.access=access;this.wallet=wallet;this.gateway=gateway;this.shipments=shipments;this.tx=new org.springframework.transaction.support.TransactionTemplate(manager);this.tx.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);}
    @Transactional(propagation=Propagation.MANDATORY)
    public void latePayment(Order order,long transactionId){
        var recipients=new LinkedHashMap<Long,String>();recipients.put(order.getBuyer().getId(),order.getBuyer().getEmail());
        for(var row:db.queryForList("select id,email from users where role='ADMIN' and is_active=true"))recipients.put(((Number)row.get("id")).longValue(),row.get("email").toString());
        recipients.forEach((id,email)->NotificationOutbox.enqueue(em,access,order.getId(),"PAYMENT_AFTER_CANCELLATION",email,"LATE_PAYMENT:"+transactionId+":"+id,"BonsaiMarket — Giao dịch cần đối soát","Đơn "+order.getCode()+" có kết quả thanh toán đến sau khi hủy. Cần xem xét hồ sơ hoàn tiền. Không tự khôi phục đơn, tồn kho hoặc xác nhận hoàn tiền."));
    }
    private Order order(Long actor,long id,String scope,boolean lock){
        if(scope.equals("admin"))access.admin(actor);else if(scope.equals("seller"))access.seller(actor);else access.actor(actor);
        var o=lock?access.lock(Order.class,id):em.find(Order.class,id);
        if(o==null||scope.equals("account")&&!o.getBuyer().getId().equals(actor)||scope.equals("seller")&&!o.getStore().getOwner().getId().equals(actor))throw MarketplaceException.missing();
        return o;
    }
    private record Funding(Long transaction,String method,BigDecimal amount) {}
    private Funding funding(Order order){
        if(db.queryForObject("select count(*) from order_payments where order_id=? and method='WALLET_SANDBOX'",Long.class,order.getId())>0){
            if(!wallet.refundable(order))throw conflict("WALLET_REFUND_VIA_CANCELLATION","Ví đã hoàn hoặc không còn nghĩa vụ đủ điều kiện. Không ghi có thêm.");
            return new Funding(null,"WALLET_SANDBOX",BigDecimal.valueOf(wallet.refundRemaining(order)));
        }
        var rows=db.queryForList("select id,payment_method,amount from transactions where order_id=? and status='SUCCESS'",order.getId());
        if(rows.size()==1){var r=rows.getFirst();var amount=new BigDecimal(r.get("amount").toString());
            if(!"VNPAY".equals(r.get("payment_method"))||amount.compareTo(order.getTotal())!=0)throw conflict("REFUND_AMOUNT_MISMATCH","Giao dịch không khớp tổng đơn hoặc phương thức hỗ trợ; cần đối soát.");
            return new Funding(((Number)r.get("id")).longValue(),"VNPAY",amount);
        }
        if(rows.size()>1)throw conflict("REFUND_RECONCILIATION_REQUIRED","Có nhiều giao dịch thành công; cần đối soát riêng trước khi lập hồ sơ.");
        if(db.queryForObject("select count(*) from order_payments where order_id=? and (collected_amount is not null or settled_amount is not null or status='REVIEW_REQUIRED' or settlement_status<>'UNKNOWN')",Long.class,order.getId())>0)
            return new Funding(null,"COD",order.getTotal());
        throw conflict("REFUND_NO_PAYMENT_EVIDENCE","Chưa có bằng chứng thanh toán cần hoàn. COD chờ thu không phải tiền đã thu.");
    }
    private void eligible(Order o){
        if(Set.of(OrderStatus.PENDING,OrderStatus.CONFIRMED,OrderStatus.CANCELLED).contains(o.getStatus())){
            var blocker=shipments.cancellationRestriction(o.getId());if(blocker!=null)throw blocker;return;
        }
        if(db.queryForObject("select count(*) from order_returns where order_id=? and status='INSPECTED'",Long.class,o.getId())==0)
            throw conflict("REFUND_RETURN_REQUIRED","Đơn sau bàn giao cần quy trình trả hàng và kiểm tra trước khi yêu cầu xem xét hoàn tiền.");
    }
    @Transactional(readOnly=true)
    public Overview overview(Long actor,long orderId,String scope){
        var o=order(actor,orderId,scope,false);var existing=byOrder(orderId);
        if(existing!=null)return new Overview(existing,false,null);
        if(db.queryForObject("select count(*) from order_payments where order_id=? and method='WALLET_SANDBOX' and status='SANDBOX_REFUNDED'",Long.class,orderId)>0){
            var rows=db.queryForList("select id,amount,status,created_at,processed_at from vnd_wallet_transactions where order_id=? and owner_id=? and type='REFUND'",orderId,o.getBuyer().getId());
            return new Overview(null,false,"Ví mô phỏng hoàn toàn bộ vào ví khi hủy đơn trước bàn giao hợp lệ. Không hoàn về ngân hàng; không có tiền thật.",rows.isEmpty()?null:rows.getFirst());
        }
        try{eligible(o);funding(o);return new Overview(null,!scope.equals("seller"),scope.equals("seller")?"Buyer hoặc Admin lập hồ sơ; Seller không có quyền xử lý tài chính.":null);}
        catch(MarketplaceException e){return new Overview(null,false,e.getMessage());}
    }
    public Overview request(Long actor,long orderId,String scope,Write input){
        if(scope.equals("seller"))throw new MarketplaceException(403,"FORBIDDEN","Seller không được lập hoặc duyệt hồ sơ tài chính.");
        if(input==null)throw MarketplaceException.invalid("Cần lý do hoàn tiền.");
        var o=order(actor,orderId,scope,true);String reason=ContentAccess.text(input.reason(),1000);
        var existing=byOrder(orderId);
        if(existing!=null){if(!existing.reason().equals(reason)||input.amount()!=null&&existing.amount().compareTo(input.amount())!=0)throw conflict("REFUND_EXISTS","Đơn đã có hồ sơ; tải lại để theo dõi, không gửi yêu cầu khác.");return new Overview(existing,false,null);}
        eligible(o);var f=funding(o);
        var amount=input.amount()==null?f.amount():input.amount();
        try{amount.longValueExact();}catch(ArithmeticException e){throw MarketplaceException.invalid("Số tiền hoàn phải nguyên VND.");}
        if(amount.signum()<=0||amount.compareTo(f.amount())>0)throw conflict("REFUND_AMOUNT_MISMATCH","Số tiền vượt nguồn thanh toán còn đủ điều kiện.");
        if(amount.compareTo(f.amount())<0&&db.queryForObject("select count(*) from order_returns where order_id=? and status='INSPECTED'",Long.class,orderId)==0)
            throw conflict("REFUND_RETURN_REQUIRED","Hoàn một phần chỉ được xem xét sau trả hàng đã kiểm tra. Trước bàn giao dùng hủy/hoàn toàn bộ.");
        db.update("insert into order_refunds(order_id,transaction_id,method,amount,currency,status,reason,requested_by,created_at,updated_at) values(?,?,?,?,'VND','REQUESTED',?,?,?,?)",orderId,f.transaction(),f.method(),amount,reason,actor,access.now(),access.now());
        var refund=byOrder(orderId);event(actor,o,refund,reason);
        return new Overview(byOrder(orderId),false,null);
    }
    public Refund decide(Long actor,long id,Decision input){
        access.admin(actor);
        Long orderId=db.query("select order_id from order_refunds where id=?",(r,n)->r.getLong(1),id).stream().findFirst().orElseThrow(MarketplaceException::missing);
        var o=order(actor,orderId,"admin",true);var current=byOrder(orderId);
        MarketplaceAccess.revision(current.revision(),ContentAccess.revision(input.revision()));
        String reason=ContentAccess.text(input.reason(),1000),target=input.status();
        var allowed=switch(current.status()){
            case "REQUESTED"->Set.of("APPROVED","DENIED");
            case "APPROVED"->Set.of("PROCESSING");
            case "PROCESSING"->Set.of("AWAITING_EXTERNAL","UNKNOWN","FAILED");
            case "AWAITING_EXTERNAL"->Set.of("UNKNOWN","FAILED");
            default->Set.<String>of();
        };
        if(target==null||!allowed.contains(target))throw conflict("REFUND_STATE_INVALID","Chuyển trạng thái không hợp lệ. Kết quả chưa rõ không được tự thử lại; chưa có contract xác minh hoàn tiền thành công.");
        if(target.equals("APPROVED")){
            if(current.method().equals("COD"))throw conflict("REFUND_COD_EVIDENCE_REQUIRED","Báo cáo COD chưa chứng minh tiền thực thu/quyết toán; cần contract đối soát tài chính trước khi duyệt.");
            var verified=funding(o);
            eligible(o);
            if(!Objects.equals(verified.transaction(),db.queryForObject("select transaction_id from order_refunds where id=?",Long.class,id))||verified.amount().compareTo(current.amount())<0)throw conflict("REFUND_AMOUNT_MISMATCH","Bằng chứng giao dịch đã thay đổi; cần đối soát.");
        }
        db.update("update order_refunds set status=?,processed_by=?,revision=revision+1,updated_at=? where id=?",target,actor,access.now(),id);
        var updated=byOrder(orderId);event(actor,o,updated,reason);return byOrder(orderId);
    }
    private record Execution(long orderId,long refundId,String requestId,String method,BigDecimal amount,String reference,String originalDate,String transactionNo,String type){}
    /** Persist the intent before external I/O. An uncertain request is never resubmitted automatically. */
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public Refund execute(Long actor,long id,long revision){
        var pending=tx.execute(s->{
            access.admin(actor);Long orderId=db.query("select order_id from order_refunds where id=?",(r,n)->r.getLong(1),id).stream().findFirst().orElseThrow(MarketplaceException::missing);
            var o=order(actor,orderId,"admin",true);var current=byOrder(orderId);
            if(Set.of("SUCCEEDED","PROCESSING","AWAITING_EXTERNAL","UNKNOWN","FAILED").contains(current.status()))return null;
            MarketplaceAccess.revision(current.revision(),revision);if(!"APPROVED".equals(current.status()))throw conflict("REFUND_STATE_INVALID","Cần duyệt hồ sơ trước khi thực thi.");
            eligible(o);var source=funding(o);if(source.amount().compareTo(current.amount())<0)throw conflict("REFUND_AMOUNT_MISMATCH","Số tiền còn đủ điều kiện đã thay đổi.");
            if("COD".equals(current.method()))throw conflict("REFUND_COD_EVIDENCE_REQUIRED","Chưa có provider chi trả COD; giữ chờ đối soát thủ công.");
            String requestId=UUID.randomUUID().toString().replace("-","");
            if("WALLET_SANDBOX".equals(current.method())){
                var result=wallet.refundApproved(o,current.amount(),"REVIEW_REQUEST_"+id);
                complete(actor,o,current,"SUCCEEDED",result.id(),"WALLET_LEDGER:"+result.id(),"Đã ghi sổ hoàn tiền mô phỏng; không có tiền thật.");return null;
            }
            if(!gateway.refundsAvailable())throw new MarketplaceException(503,"REFUND_PROVIDER_NOT_CONFIGURED","VNPAY sandbox chưa được cấp/bật quyền hoàn tiền.");
            var payment=db.queryForMap("select transaction_code,gateway_created_date,gateway_transaction_no,amount from transactions where id=? and order_id=? and status='SUCCESS'",source.transaction(),orderId);
            String date=Objects.toString(payment.get("gateway_created_date"),""),no=Objects.toString(payment.get("gateway_transaction_no"),"");
            if(!date.matches("[0-9]{14}")||!no.matches("[0-9]{1,15}"))throw conflict("REFUND_RECONCILIATION_REQUIRED","Giao dịch thiếu liên kết/thời điểm gốc; cần đối soát thủ công.");
            db.update("update order_refunds set provider_request_id=?,provider_started_at=? where id=?",requestId,access.now(),id);
            complete(actor,o,current,"PROCESSING",null,null,"Đã giữ lệnh hoàn VNPAY sandbox, chưa xác nhận hoàn tất.");
            return new Execution(orderId,id,requestId,current.method(),current.amount(),payment.get("transaction_code").toString(),date,no,current.amount().compareTo(source.amount())==0?"02":"03");
        });
        if(pending==null)return tx.execute(s->{access.admin(actor);return byId(id);});
        Map<String,String> response;
        try{response=gateway.refund(pending.requestId(),pending.reference(),pending.originalDate(),pending.transactionNo(),pending.amount(),pending.type(),actor);}
        catch(RuntimeException e){return tx.execute(s->{var o=order(actor,pending.orderId(),"admin",true);var current=byOrder(o.getId());if("PROCESSING".equals(current.status()))complete(actor,o,current,"UNKNOWN",null,null,"Kết quả provider chưa rõ. Không gửi lại lệnh hoàn; đối soát merchant với mã yêu cầu đã lưu.");return byOrder(o.getId());});}
        return tx.execute(s->{var o=order(actor,pending.orderId(),"admin",true);var current=byOrder(o.getId());
            if(!"PROCESSING".equals(current.status()))return current;
            boolean success="00".equals(response.get("vnp_ResponseCode"))&&"00".equals(response.get("vnp_TransactionStatus"));
            String state=success?"SUCCEEDED":Set.of("05","06").contains(response.getOrDefault("vnp_TransactionStatus",""))?"AWAITING_EXTERNAL":"UNKNOWN";
            String ref=response.get("vnp_TransactionNo");if(success&&(ref==null||!ref.matches("[0-9]{1,15}"))){success=false;state="UNKNOWN";}
            complete(actor,o,current,state,ref!=null&&ref.matches("[0-9]{1,15}")?ref:null,success?"VNPAY_SANDBOX:"+pending.requestId()+":"+response.get("vnp_ResponseId"):null,success?"VNPAY sandbox trả kết quả hoàn đã xác minh; không chứng minh quyết toán ngân hàng thật.":"Provider chưa xác minh hoàn tất. Giữ mã lệnh, không tự gửi lại.");
            return byOrder(o.getId());});
    }
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public Refund reconcile(Long actor,long id,long revision){
        var pending=tx.execute(s->{access.admin(actor);var current=byId(id);var o=order(actor,current.orderId(),"admin",true);current=byId(id);
            MarketplaceAccess.revision(current.revision(),revision);if(!"VNPAY".equals(current.method())||!Set.of("UNKNOWN","AWAITING_EXTERNAL","PROCESSING").contains(current.status()))throw conflict("REFUND_STATE_INVALID","Hồ sơ không ở trạng thái chờ đối soát VNPAY.");
            var r=db.queryForMap("select r.provider_reference,r.provider_request_id,r.query_count,r.query_after,t.transaction_code,t.gateway_created_date,t.amount as original_amount from order_refunds r join transactions t on t.id=r.transaction_id where r.id=?",id);
            String provider=Objects.toString(r.get("provider_reference"),"");if(!provider.matches("[0-9]{1,15}"))throw conflict("REFUND_RECONCILIATION_REQUIRED","Chưa có mã hoàn provider đã xác minh. Đối chiếu merchant bằng mã yêu cầu; không suy từ truy vấn giao dịch thu tiền.");
            var now=access.now();if(((Number)r.get("query_count")).intValue()>=5||r.get("query_after")!=null&&db.queryForObject("select count(*) from order_refunds where id=? and query_after>?",Integer.class,id,now)>0)throw new MarketplaceException(429,"REFUND_QUERY_LIMIT","Đối soát tối đa 5 lần, cách nhau 5 phút.");
            db.update("update order_refunds set query_count=query_count+1,query_after=? where id=?",now.plusMinutes(5),id);
            MarketplaceAudit.record(em,access,actor,"REFUND_QUERY","REFUND",id,"Bounded provider reconciliation");
            return new Execution(o.getId(),id,Objects.toString(r.get("provider_request_id")),"VNPAY",current.amount(),r.get("transaction_code").toString(),r.get("gateway_created_date").toString(),provider,current.amount().compareTo(new BigDecimal(r.get("original_amount").toString()))==0?"02":"03");
        });
        Map<String,String> result;try{result=gateway.queryRefund(UUID.randomUUID().toString().replace("-",""),pending.reference(),pending.originalDate(),pending.transactionNo());}catch(RuntimeException e){return tx.execute(s->{order(actor,pending.orderId(),"admin",true);return byId(id);});}
        return tx.execute(s->{var o=order(actor,pending.orderId(),"admin",true);var current=byId(id);if(!Set.of("PROCESSING","AWAITING_EXTERNAL","UNKNOWN").contains(current.status()))return current;
            if("00".equals(result.get("vnp_ResponseCode"))&&"00".equals(result.get("vnp_TransactionStatus"))&&pending.transactionNo().equals(result.get("vnp_TransactionNo"))&&pending.type().equals(result.get("vnp_TransactionType"))&&com.bonsaimarket.backend.payment.VnpayGateway.amount(pending.amount()).equals(result.get("vnp_Amount")))
                complete(actor,o,current,"SUCCEEDED",pending.transactionNo(),"VNPAY_SANDBOX_QUERY:"+result.get("vnp_ResponseId"),"QueryDr xác minh đúng mã hoàn, loại và số tiền sandbox.");
            return byId(id);});
    }
    private Refund byId(long id){Long orderId=db.query("select order_id from order_refunds where id=?",(r,n)->r.getLong(1),id).stream().findFirst().orElseThrow(MarketplaceException::missing);return byOrder(orderId);}
    @Transactional(readOnly=true) public Object operation(Long actor,long id){access.admin(actor);byId(id);return db.queryForMap("select provider_request_id,provider_reference,evidence_reference,provider_started_at,query_count,query_after from order_refunds where id=?",id);}
    private void complete(Long actor,Order o,Refund current,String state,String provider,String evidence,String reason){
        db.update("update order_refunds set status=?,provider_reference=?,evidence_reference=?,processed_by=?,revision=revision+1,updated_at=? where id=?",state,provider,evidence,actor,access.now(),current.id());
        if("SUCCEEDED".equals(state)){
            var p=em.find(com.bonsaimarket.backend.payment.OrderPayment.class,o.getId());
            if(p!=null){boolean full=current.amount().compareTo(o.getTotal())==0;p.setStatus("WALLET_SANDBOX".equals(current.method())?(full?"SANDBOX_REFUNDED":"SANDBOX_PARTIALLY_REFUNDED"):(full?"REFUNDED":"PARTIALLY_REFUNDED"));if("WALLET_SANDBOX".equals(current.method())&&full)p.setSettlementStatus("SANDBOX_REFUNDED");}
        }
        event(actor,o,byOrder(o.getId()),reason);
    }
    private void event(Long actor,Order o,Refund r,String reason){
        db.update("insert into refund_events(refund_id,actor_id,status,reason,revision,created_at) values(?,?,?,?,?,?)",r.id(),actor,r.status(),reason,r.revision(),access.now());
        MarketplaceAudit.record(em,access,actor,"REFUND_"+r.status(),"REFUND",r.id(),"Revision "+r.revision());
        var recipients=new LinkedHashMap<Long,String>();recipients.put(o.getBuyer().getId(),o.getBuyer().getEmail());
        if(r.status().equals("REQUESTED"))for(var row:db.queryForList("select id,email from users where role='ADMIN' and is_active=true"))recipients.put(((Number)row.get("id")).longValue(),row.get("email").toString());
        recipients.forEach((id,email)->NotificationOutbox.enqueue(em,access,o.getId(),"REFUND_"+r.status(),email,"REFUND:"+r.id()+":"+r.revision()+":"+id,"BonsaiMarket — Cập nhật hồ sơ hoàn tiền","Hồ sơ đơn "+o.getCode()+" được cập nhật. Mở chi tiết đơn để xem tiến độ. Hồ sơ nội bộ không xác nhận tiền đã hoàn."));
    }
    @Transactional(readOnly=true)
    public ContentPage<Refund> queue(Long actor,int page){access.admin(actor);ContentAccess.page(page);long count=db.queryForObject("select count(*) from order_refunds",Long.class);
        var ids=db.query("select order_id from order_refunds order by updated_at desc,id desc limit 10 offset ?",(r,n)->r.getLong(1),page*10);return ContentPage.of(ids.stream().map(this::byOrder).toList(),page,count);}
    private Refund byOrder(long id){return db.query("select * from order_refunds where order_id=?",(r,n)->{
        long refundId=r.getLong("id");var events=db.query("select status,reason,created_at from refund_events where refund_id=? order by revision",(e,i)->new Event(e.getString(1),e.getString(2),e.getObject(3,LocalDateTime.class)),refundId);
        return new Refund(refundId,id,r.getString("method"),r.getBigDecimal("amount"),r.getString("currency"),r.getString("status"),r.getString("reason"),r.getLong("revision"),r.getObject("updated_at",LocalDateTime.class),events);
    },id).stream().findFirst().orElse(null);}
    private static MarketplaceException conflict(String code,String message){return new MarketplaceException(409,code,message);}
}
