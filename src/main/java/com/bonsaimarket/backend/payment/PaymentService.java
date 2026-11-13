package com.bonsaimarket.backend.payment;

import com.bonsaimarket.backend.marketplace.MarketplaceException;
import com.bonsaimarket.backend.order.Order;
import com.bonsaimarket.backend.order.OrderRepository;
import com.bonsaimarket.backend.order.OrderStatus;
import com.bonsaimarket.backend.user.User;
import com.bonsaimarket.backend.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class PaymentService {
    private final OrderRepository orders;
    private final TransactionRepository transactions;
    private final UserRepository users;

    private final String vnpayTmnCode;
    private final String vnpayHashSecret;
    private final String vnpayUrl;
    private final String frontendOrigin;

    private final jakarta.persistence.EntityManager em;
    private final com.bonsaimarket.backend.inventory.InventoryService inventory;
    private final boolean enabled;
    private final com.bonsaimarket.backend.refund.RefundService refunds;
    private final VnpayGateway gateway;
    private final org.springframework.transaction.support.TransactionTemplate tx;

    public PaymentService(
            OrderRepository orders,
            TransactionRepository transactions,
            UserRepository users, jakarta.persistence.EntityManager em, com.bonsaimarket.backend.inventory.InventoryService inventory, com.bonsaimarket.backend.refund.RefundService refunds, VnpayGateway gateway,org.springframework.transaction.PlatformTransactionManager manager,
            @Value("${vnpay.enabled:false}") boolean enabled,
            @Value("${vnpay.tmn-code:}") String vnpayTmnCode,
            @Value("${vnpay.hash-secret:}") String vnpayHashSecret,
            @Value("${vnpay.url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}") String vnpayUrl,
            @Value("${auth.frontend-origin:http://localhost:5173}") String frontendOrigin) {
        this.em=em;this.inventory=inventory;this.enabled=enabled;this.refunds=refunds;this.gateway=gateway;
        this.tx=new org.springframework.transaction.support.TransactionTemplate(manager);this.tx.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
        this.orders = orders;
        this.transactions = transactions;
        this.users = users;
        this.vnpayTmnCode = vnpayTmnCode;
        this.vnpayHashSecret = vnpayHashSecret;
        this.vnpayUrl = vnpayUrl;
        this.frontendOrigin = frontendOrigin;
    }

    public PaymentDtos.PaymentInitResponse createPayment(Long buyerId, PaymentDtos.CreatePaymentRequest request, String clientIp) {
        Order order = owned(buyerId,request.orderId(),true);
        String method=request.paymentMethod().strip().toUpperCase(Locale.ROOT);
        if(!Set.of(OrderStatus.PENDING,OrderStatus.CONFIRMED).contains(order.getStatus()))throw new MarketplaceException(409,"ORDER_STATE_INVALID","Chỉ thanh toán đơn trước bàn giao.");
        var intent=em.find(OrderPayment.class,order.getId());
        if(intent!=null&&!"VNPAY".equals(intent.getMethod())){
            if("COD".equals(method)&&"COD".equals(intent.getMethod())&&"AWAITING_COLLECTION".equals(intent.getStatus()))
                return new PaymentDtos.PaymentInitResponse(null,"COD",order.getTotal(),frontendOrigin+"/orders/"+order.getId(),Map.of("message","Đơn đã ghi nhận COD. Chưa có xác nhận thu tiền."));
            throw new MarketplaceException(409,"PAYMENT_METHOD_LOCKED","Đơn đã xác nhận COD; chưa hỗ trợ đổi phương thức sau khi đặt.");
        }
        if(!"VNPAY".equals(method))throw new MarketplaceException(409,"PAYMENT_METHOD_UNSUPPORTED","Phương thức này chưa có tích hợp xác thực thanh toán.");
        configured();
        if(transactions.findByOrderIdOrderByCreatedAtDesc(order.getId()).stream().anyMatch(t->t.getStatus()==TransactionStatus.SUCCESS))throw new MarketplaceException(409,"ALREADY_PAID","Đơn đã có giao dịch thành công, cần đối chiếu trước khi thanh toán tiếp.");
        var pending=transactions.findByOrderIdOrderByCreatedAtDesc(order.getId()).stream().filter(t->Set.of(TransactionStatus.PENDING,TransactionStatus.UNKNOWN,TransactionStatus.EXPIRED).contains(t.getStatus())).findFirst();
        if(pending.isPresent())throw new MarketplaceException(409,"PAYMENT_PENDING","Đơn đang có giao dịch cần đối chiếu. Không tạo thêm yêu cầu thu tiền.");
        if(transactions.findByOrderIdOrderByCreatedAtDesc(order.getId()).size()>=5)throw new MarketplaceException(409,"PAYMENT_ATTEMPT_LIMIT","Đã đạt giới hạn 5 lần thử; cần hỗ trợ đối soát.");
        VnpayGateway.amount(order.getTotal());
        if(order.getStatus()==OrderStatus.PENDING)inventory.debitPlacedOrder(order.getId());else inventory.debitConfirmedOrder(order.getId());
        String txnCode = UUID.randomUUID().toString().replace("-","");

        var txn = new Transaction();
        txn.setOrder(order);
        txn.setTransactionCode(txnCode);
        txn.setPaymentMethod(method);
        txn.setAmount(order.getTotal());
        txn.setStatus(TransactionStatus.PENDING);
        txn.setCreatedAt(LocalDateTime.now(java.time.ZoneOffset.UTC));
        txn.setUpdatedAt(LocalDateTime.now(java.time.ZoneOffset.UTC));
        txn.setCurrency("VND");txn.setExpiresAt(txn.getCreatedAt().plusMinutes(15));txn.setGatewayCreatedDate(VnpayGateway.date(txn.getCreatedAt()));
        transactions.save(txn);
        if(intent!=null)intent.setStatus("PENDING");
        audit(txn,"INIT","PENDING",txnCode);

        String paymentUrl = null;
        Map<String, Object> extra = new HashMap<>();

        paymentUrl=buildVnpayPaymentUrl(txn,order,clientIp,request.bankCode(),frontendOrigin+"/payment/callback");
        return new PaymentDtos.PaymentInitResponse(txnCode, method, order.getTotal(), paymentUrl, extra);
    }

    public PaymentDtos.PaymentCallbackResult processVnpayCallback(Map<String,String> params){return processVnpayCallback(null,params);}
    public Map<String,String> ipn(Map<String,String> params){
        configured();if(!VNPayUtil.verifyCallback(params,vnpayHashSecret))throw new MarketplaceException(400,"INVALID_SIGNATURE","Chữ ký thanh toán không hợp lệ.");
        var t=transactions.findByTransactionCode(params.get("vnp_TxnRef")).orElseThrow(MarketplaceException::missing);orders.lockById(t.getOrder().getId()).orElseThrow(MarketplaceException::missing);em.refresh(t,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        boolean completed=Set.of(TransactionStatus.SUCCESS,TransactionStatus.FAILED,TransactionStatus.CANCELLED).contains(t.getStatus());
        var result=apply(params,null,"IPN",false);String code=Set.of("PENDING","UNKNOWN","EXPIRED").contains(result.status())?"99":completed?"02":"00";
        return Map.of("RspCode",code,"Message",code.equals("00")?"Confirm Success":code.equals("02")?"Order already confirmed":"Reconciliation required");
    }
    public PaymentDtos.PaymentCallbackResult processVnpayCallback(Long buyerId,Map<String,String> params){
        // Compatibility service entry for server IPN only. Browser controller calls inspectReturn instead.
        return apply(params,buyerId,"IPN",false);
    }
    private PaymentDtos.PaymentCallbackResult apply(Map<String,String> params,Long buyerId,String source,boolean verifiedQuery){
        configured();
        if(!verifiedQuery&&!VNPayUtil.verifyCallback(params,vnpayHashSecret))throw new MarketplaceException(400,"INVALID_SIGNATURE","Chữ ký thanh toán không hợp lệ.");
        var found=transactions.findByTransactionCode(params.get("vnp_TxnRef")).orElseThrow(MarketplaceException::missing);
        Order order=orders.lockById(found.getOrder().getId()).orElseThrow(MarketplaceException::missing);
        if(buyerId!=null&&!order.getBuyer().getId().equals(buyerId))throw MarketplaceException.missing();
        em.refresh(found,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if(!"VNPAY".equals(found.getPaymentMethod())||!vnpayTmnCode.equals(params.get("vnp_TmnCode"))||!VnpayGateway.amount(found.getAmount()).equals(params.get("vnp_Amount"))||(params.containsKey("vnp_CurrCode")&&!"VND".equals(params.get("vnp_CurrCode"))))
            throw new MarketplaceException(400,"PAYMENT_AMOUNT_MISMATCH","Thông tin merchant hoặc số tiền không khớp giao dịch.");
        if(!params.getOrDefault("vnp_ResponseCode","").matches("[0-9]{2}")||!params.getOrDefault("vnp_TransactionStatus","").matches("[0-9]{2}")||!params.getOrDefault("vnp_TransactionNo","").matches("[0-9]{1,15}"))throw new MarketplaceException(400,"PAYMENT_RESULT_INVALID","Kết quả thanh toán thiếu dữ liệu hợp lệ.");
        boolean success="00".equals(params.get("vnp_ResponseCode"))&&"00".equals(params.get("vnp_TransactionStatus"));
        var now=LocalDateTime.now(java.time.ZoneOffset.UTC);
        boolean finalState=Set.of(TransactionStatus.SUCCESS,TransactionStatus.FAILED,TransactionStatus.CANCELLED).contains(found.getStatus());
        if(finalState){audit(found,source,success== (found.getStatus()==TransactionStatus.SUCCESS)?"DUPLICATE":"CONFLICT",params.get("vnp_TransactionNo")+params.get("vnp_TransactionStatus"));return result(found);}
        boolean expired=found.getExpiresAt()!=null&&!found.getExpiresAt().isAfter(now);
        if(!verifiedQuery&&(expired||found.getStatus()!=TransactionStatus.PENDING)){
            found.setStatus(TransactionStatus.UNKNOWN);found.setUpdatedAt(now);found.setNotes("Callback muộn hoặc chưa rõ; cần QueryDr.");audit(found,source,"UNKNOWN",params.get("vnp_TransactionNo"));updateIntent(found);return result(found);
        }
        String providerState=params.get("vnp_TransactionStatus");
        boolean failed=Set.of("02").contains(providerState)||!verifiedQuery&&Set.of("07","09","10","11","12","13","24","51","65","75","79").contains(params.get("vnp_ResponseCode"));
        if(!success&&!failed){found.setStatus(expired?TransactionStatus.EXPIRED:TransactionStatus.UNKNOWN);found.setUpdatedAt(now);audit(found,source,found.getStatus().name(),providerState);updateIntent(found);return result(found);}
        {
            found.setGatewayTransactionNo(params.get("vnp_TransactionNo"));found.setBankCode(params.get("vnp_BankCode"));found.setCardType(params.get("vnp_CardType"));found.setPayDate(params.get("vnp_PayDate"));found.setResponseCode(params.get("vnp_ResponseCode"));found.setStatus(success?TransactionStatus.SUCCESS:TransactionStatus.FAILED);found.setUpdatedAt(LocalDateTime.now(java.time.ZoneOffset.UTC));
            // Kết quả cổng thanh toán không thay quyền xác nhận đơn của người bán.
            if(success&&order.getStatus()==OrderStatus.CANCELLED){found.setNotes("Tiền về sau khi hủy đơn; cần đối chiếu và xử lý hoàn tiền.");refunds.latePayment(order,found.getId());}
        }
        audit(found,source,found.getStatus().name(),params.get("vnp_TransactionNo"));updateIntent(found);return result(found);
    }
    private void updateIntent(Transaction t){var p=em.find(OrderPayment.class,t.getOrder().getId());if(p!=null&&"VNPAY".equals(p.getMethod())&&!Set.of("REFUNDED","PARTIALLY_REFUNDED").contains(p.getStatus()))p.setStatus(t.getStatus().name());}
    private void audit(Transaction t,String source,String outcome,String identity){
        String key=com.bonsaimarket.backend.wallet.WalletCredits.digest(t.getTransactionCode()+"|"+source+"|"+outcome+"|"+identity);
        if(em.createQuery("select count(e) from PaymentEvent e where e.dedupKey=:key",Long.class).setParameter("key",key).getSingleResult()==0)em.persist(new PaymentEvent(t.getId(),key,source,outcome,LocalDateTime.now(java.time.ZoneOffset.UTC)));
    }
    private PaymentDtos.PaymentCallbackResult result(Transaction t){return new PaymentDtos.PaymentCallbackResult(t.getStatus()==TransactionStatus.SUCCESS,t.getTransactionCode(),t.getOrder().getId(),t.getOrder().getCode(),t.getAmount(),t.getStatus()==TransactionStatus.SUCCESS?"Đã xác minh thanh toán VNPAY sandbox. Chưa chứng minh tiền về tài khoản ngân hàng.":Set.of(TransactionStatus.FAILED,TransactionStatus.CANCELLED).contains(t.getStatus())?"Giao dịch không thành công.":"Chờ IPN hoặc đối soát VNPAY. Không thanh toán lại khi chưa rõ kết quả.",t.getResponseCode(),t.getPayDate(),t.getStatus().name());}
    @Transactional(readOnly=true)
    public PaymentDtos.PaymentCallbackResult inspectReturn(Long buyerId,Map<String,String> params){
        configured();if(!VNPayUtil.verifyCallback(params,vnpayHashSecret))throw new MarketplaceException(400,"INVALID_SIGNATURE","Chữ ký thanh toán không hợp lệ.");
        var t=transactions.findByTransactionCode(params.get("vnp_TxnRef")).orElseThrow(MarketplaceException::missing);owned(buyerId,t.getOrder().getId(),false);
        if(!vnpayTmnCode.equals(params.get("vnp_TmnCode"))||!VnpayGateway.amount(t.getAmount()).equals(params.get("vnp_Amount")))throw new MarketplaceException(400,"PAYMENT_AMOUNT_MISMATCH","Thông tin thanh toán không khớp.");
        return result(t);
    }
    private record Query(Long orderId,String reference,String originalDate,String requestId){}
    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public PaymentDtos.PaymentCallbackResult reconcile(Long buyerId,String code){
        Query q=tx.execute(s->{var found=transactions.findByTransactionCode(code).orElseThrow(MarketplaceException::missing);owned(buyerId,found.getOrder().getId(),true);em.refresh(found,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            if(Set.of(TransactionStatus.SUCCESS,TransactionStatus.FAILED,TransactionStatus.CANCELLED).contains(found.getStatus()))return null;
            var now=LocalDateTime.now(java.time.ZoneOffset.UTC);if(found.getQueryCount()>=5||found.getQueryAfter()!=null&&found.getQueryAfter().isAfter(now))throw new MarketplaceException(429,"PAYMENT_QUERY_LIMIT","Đối soát tối đa 5 lần, cách nhau ít nhất 5 phút; liên hệ hỗ trợ khi vượt giới hạn.");
            configured();if(found.getGatewayCreatedDate()==null)throw new MarketplaceException(409,"PAYMENT_RECONCILIATION_REQUIRED","Giao dịch cũ thiếu ngày khởi tạo gốc; cần đối soát thủ công.");
            found.setQueryCount(found.getQueryCount()+1);found.setQueryAfter(now.plusMinutes(5));String id=UUID.randomUUID().toString().replace("-","");audit(found,"QUERY_REQUEST","PENDING",id);return new Query(found.getOrder().getId(),code,found.getGatewayCreatedDate(),id);});
        if(q==null)return tx.execute(s->result(transactions.findByTransactionCode(code).orElseThrow(MarketplaceException::missing)));
        try{var response=gateway.query(q.requestId(),q.reference(),q.originalDate());
            return tx.execute(s->{if(!"00".equals(response.get("vnp_ResponseCode"))||!"01".equals(response.get("vnp_TransactionType")))return unknown(q,buyerId);return apply(response,buyerId,"QUERY",true);});
        }catch(MarketplaceException e){return tx.execute(s->unknown(q,buyerId));}
    }
    private PaymentDtos.PaymentCallbackResult unknown(Query q,Long buyerId){owned(buyerId,q.orderId(),true);var t=transactions.findByTransactionCode(q.reference()).orElseThrow(MarketplaceException::missing);em.refresh(t,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);if(!Set.of(TransactionStatus.SUCCESS,TransactionStatus.FAILED,TransactionStatus.CANCELLED).contains(t.getStatus())){t.setStatus(TransactionStatus.UNKNOWN);t.setUpdatedAt(LocalDateTime.now(java.time.ZoneOffset.UTC));updateIntent(t);}audit(t,"QUERY","UNKNOWN",q.requestId());return result(t);}
    public boolean available(){return gateway.available();}
    public PaymentDtos.PaymentCallbackResult mockConfirm(Long buyerId,String code){throw new MarketplaceException(403,"PAYMENT_MOCK_DISABLED","Không hỗ trợ xác nhận thanh toán giả lập.");}
    private void configured(){if(!enabled||vnpayTmnCode.isBlank()||vnpayHashSecret.isBlank()||!"https://sandbox.vnpayment.vn/paymentv2/vpcpay.html".equals(vnpayUrl))throw new MarketplaceException(503,"PAYMENT_NOT_CONFIGURED","Thanh toán trực tuyến qua VNPAY hiện chưa khả dụng.");}
    private Order owned(Long buyerId,Long orderId,boolean lock){
        var order=(lock?orders.lockById(orderId):orders.findById(orderId)).orElseThrow(MarketplaceException::missing);
        if(!order.getBuyer().getId().equals(buyerId))throw MarketplaceException.missing();return order;
    }

    @Transactional(readOnly = true)
    public List<PaymentDtos.TransactionView> getTransactionsForOrder(Long buyerId, Long orderId) {
        Order order = orders.findById(orderId).orElseThrow(MarketplaceException::missing);
        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new MarketplaceException(403, "FORBIDDEN", "Bạn không có quyền thực hiện thao tác này.");
        }

        return transactions.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(t -> new PaymentDtos.TransactionView(
                        t.getId(),
                        t.getTransactionCode(),
                        t.getPaymentMethod(),
                        t.getAmount(),
                        t.getStatus().name(),
                        t.getGatewayTransactionNo(),
                        t.getBankCode(),
                        t.getResponseCode(),
                        t.getCreatedAt().atOffset(java.time.ZoneOffset.UTC).toString()
                )).toList();
    }

    @Transactional(readOnly = true)
    public PaymentDtos.BankTransferInfo getBankTransferInfo(Long buyerId,Long orderId){owned(buyerId,orderId,false);throw new MarketplaceException(503,"BANKING_NOT_CONFIGURED","Chuyển khoản chưa có thông tin tài khoản và quy trình đối chiếu được xác nhận.");}

    private String buildVnpayPaymentUrl(Transaction txn, Order order, String ipAddress, String bankCode, String returnUrl) {
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnpayTmnCode);

        vnpParams.put("vnp_Amount", VnpayGateway.amount(txn.getAmount()));
        vnpParams.put("vnp_CurrCode", "VND");

        if (bankCode != null && !bankCode.isBlank()) {
            vnpParams.put("vnp_BankCode", bankCode);
        }

        vnpParams.put("vnp_TxnRef", txn.getTransactionCode());
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang BonsaiMarket " + order.getCode());
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", returnUrl);
        vnpParams.put("vnp_IpAddr", (ipAddress == null || ipAddress.isBlank()) ? "127.0.0.1" : ipAddress);

        vnpParams.put("vnp_CreateDate",txn.getGatewayCreatedDate());
        vnpParams.put("vnp_ExpireDate",VnpayGateway.date(txn.getExpiresAt()));

        return VNPayUtil.buildQueryUrl(vnpParams, vnpayHashSecret, vnpayUrl);
    }
}
