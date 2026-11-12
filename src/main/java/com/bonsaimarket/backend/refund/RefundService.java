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

/** Internal review only. There is deliberately no financial success mutation. */
@Service
@Transactional(isolation=Isolation.READ_COMMITTED)
public class RefundService {
    public record Write(String reason) {}
    public record Decision(String status,String reason,Long revision) {}
    public record Event(String status,String reason,LocalDateTime createdAt) {}
    public record Refund(long id,long orderId,String method,BigDecimal amount,String currency,String status,String reason,long revision,LocalDateTime updatedAt,List<Event> history) {}
    public record Overview(Refund refund,boolean canRequest,String blocker,Map<String,Object> sandboxWalletRefund) {
        public Overview(Refund refund,boolean canRequest,String blocker){this(refund,canRequest,blocker,null);}
    }
    private final JdbcTemplate db;
    private final EntityManager em;
    private final MarketplaceAccess access;
    public RefundService(JdbcTemplate db,EntityManager em,MarketplaceAccess access){this.db=db;this.em=em;this.access=access;}
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
        if(db.queryForObject("select count(*) from order_payments where order_id=? and method='WALLET_SANDBOX'",Long.class,order.getId())>0)
            throw conflict("WALLET_REFUND_VIA_CANCELLATION","Ví mô phỏng hoàn toàn bộ khi hủy đơn trước bàn giao hợp lệ. Không lập hồ sơ hoàn tiền ngân hàng cho giao dịch này.");
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
        if(Set.of(OrderStatus.PENDING,OrderStatus.CONFIRMED,OrderStatus.CANCELLED).contains(o.getStatus()))return;
        if(db.queryForObject("select count(*) from order_returns where order_id=? and status='INSPECTED'",Long.class,o.getId())==0)
            throw conflict("REFUND_RETURN_REQUIRED","Đơn sau bàn giao cần quy trình trả hàng và kiểm tra trước khi yêu cầu xem xét hoàn tiền.");
    }
    @Transactional(readOnly=true)
    public Overview overview(Long actor,long orderId,String scope){
        var o=order(actor,orderId,scope,false);var existing=byOrder(orderId);
        if(existing!=null)return new Overview(existing,false,null);
        if(db.queryForObject("select count(*) from order_payments where order_id=? and method='WALLET_SANDBOX'",Long.class,orderId)>0){
            var rows=db.queryForList("select id,amount,status,created_at,processed_at from vnd_wallet_transactions where order_id=? and owner_id=? and type='REFUND'",orderId,o.getBuyer().getId());
            return new Overview(null,false,"Ví mô phỏng hoàn toàn bộ vào ví khi hủy đơn trước bàn giao hợp lệ. Không hoàn về ngân hàng; không có tiền thật.",rows.isEmpty()?null:rows.getFirst());
        }
        try{eligible(o);funding(o);return new Overview(null,!scope.equals("seller"),scope.equals("seller")?"Buyer hoặc Admin lập hồ sơ; Seller không có quyền xử lý tài chính.":null);}
        catch(MarketplaceException e){return new Overview(null,false,e.getMessage());}
    }
    public Overview request(Long actor,long orderId,String scope,Write input){
        if(scope.equals("seller"))throw new MarketplaceException(403,"FORBIDDEN","Seller không được lập hoặc duyệt hồ sơ tài chính.");
        var o=order(actor,orderId,scope,true);String reason=ContentAccess.text(input.reason(),1000);
        var existing=byOrder(orderId);
        if(existing!=null){if(!existing.reason().equals(reason))throw conflict("REFUND_EXISTS","Đơn đã có hồ sơ; tải lại để theo dõi, không gửi yêu cầu khác.");return new Overview(existing,false,null);}
        eligible(o);var f=funding(o);
        db.update("insert into order_refunds(order_id,transaction_id,method,amount,currency,status,reason,requested_by,created_at,updated_at) values(?,?,?,?,'VND','REQUESTED',?,?,?,?)",orderId,f.transaction(),f.method(),f.amount(),reason,actor,access.now(),access.now());
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
            if(!Objects.equals(verified.transaction(),db.queryForObject("select transaction_id from order_refunds where id=?",Long.class,id))||verified.amount().compareTo(current.amount())!=0)throw conflict("REFUND_AMOUNT_MISMATCH","Bằng chứng giao dịch đã thay đổi; cần đối soát.");
        }
        db.update("update order_refunds set status=?,processed_by=?,revision=revision+1,updated_at=? where id=?",target,actor,access.now(),id);
        var updated=byOrder(orderId);event(actor,o,updated,reason);return byOrder(orderId);
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
