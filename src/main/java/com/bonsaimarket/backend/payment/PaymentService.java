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

    public PaymentService(
            OrderRepository orders,
            TransactionRepository transactions,
            UserRepository users, jakarta.persistence.EntityManager em, com.bonsaimarket.backend.inventory.InventoryService inventory,
            @Value("${vnpay.enabled:false}") boolean enabled,
            @Value("${vnpay.tmn-code:}") String vnpayTmnCode,
            @Value("${vnpay.hash-secret:}") String vnpayHashSecret,
            @Value("${vnpay.url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}") String vnpayUrl,
            @Value("${auth.frontend-origin:http://localhost:5173}") String frontendOrigin) {
        this.em=em;this.inventory=inventory;this.enabled=enabled;
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
        if(order.getStatus()!=OrderStatus.PENDING)throw new MarketplaceException(409,"ORDER_STATE_INVALID","Chỉ thanh toán đơn đang chờ xử lý.");
        var intent=em.find(OrderPayment.class,order.getId());
        if(intent!=null){
            if("COD".equals(method)&&"COD".equals(intent.getMethod())&&"AWAITING_COLLECTION".equals(intent.getStatus()))
                return new PaymentDtos.PaymentInitResponse(null,"COD",order.getTotal(),frontendOrigin+"/orders/"+order.getId(),Map.of("message","Đơn đã ghi nhận COD. Chưa có xác nhận thu tiền."));
            throw new MarketplaceException(409,"PAYMENT_METHOD_LOCKED","Đơn đã xác nhận COD; chưa hỗ trợ đổi phương thức sau khi đặt.");
        }
        if(!"VNPAY".equals(method))throw new MarketplaceException(409,"PAYMENT_METHOD_UNSUPPORTED","Phương thức này chưa có tích hợp xác thực thanh toán.");
        configured();
        if(transactions.findByOrderIdOrderByCreatedAtDesc(order.getId()).stream().anyMatch(t->t.getStatus()==TransactionStatus.SUCCESS))throw new MarketplaceException(409,"ALREADY_PAID","Đơn đã có giao dịch thành công, cần đối chiếu trước khi thanh toán tiếp.");
        var pending=transactions.findByOrderIdOrderByCreatedAtDesc(order.getId()).stream().filter(t->t.getStatus()==TransactionStatus.PENDING).findFirst();
        if(pending.isPresent())throw new MarketplaceException(409,"PAYMENT_PENDING","Đơn đang có giao dịch cần đối chiếu. Không tạo thêm yêu cầu thu tiền.");
        inventory.debitPlacedOrder(order.getId());
        String txnCode = "TXN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        var txn = new Transaction();
        txn.setOrder(order);
        txn.setTransactionCode(txnCode);
        txn.setPaymentMethod(method);
        txn.setAmount(order.getTotal());
        txn.setStatus(TransactionStatus.PENDING);
        txn.setCreatedAt(LocalDateTime.now(java.time.ZoneOffset.UTC));
        txn.setUpdatedAt(LocalDateTime.now(java.time.ZoneOffset.UTC));
        transactions.save(txn);

        String paymentUrl = null;
        Map<String, Object> extra = new HashMap<>();

        paymentUrl=buildVnpayPaymentUrl(txnCode,order,clientIp,request.bankCode(),frontendOrigin+"/payment/callback");
        return new PaymentDtos.PaymentInitResponse(txnCode, method, order.getTotal(), paymentUrl, extra);
    }

    public PaymentDtos.PaymentCallbackResult processVnpayCallback(Map<String,String> params){return processVnpayCallback(null,params);}
    public PaymentDtos.PaymentCallbackResult processVnpayCallback(Long buyerId,Map<String,String> params){
        configured();
        if(!VNPayUtil.verifyCallback(params,vnpayHashSecret))throw new MarketplaceException(400,"INVALID_SIGNATURE","Chữ ký thanh toán không hợp lệ.");
        var found=transactions.findByTransactionCode(params.get("vnp_TxnRef")).orElseThrow(MarketplaceException::missing);
        Order order=orders.lockById(found.getOrder().getId()).orElseThrow(MarketplaceException::missing);
        if(buyerId!=null&&!order.getBuyer().getId().equals(buyerId))throw MarketplaceException.missing();
        em.refresh(found,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if(!"VNPAY".equals(found.getPaymentMethod())||!vnpayTmnCode.equals(params.get("vnp_TmnCode"))||!found.getAmount().multiply(new BigDecimal("100")).toBigIntegerExact().toString().equals(params.get("vnp_Amount")))
            throw new MarketplaceException(400,"PAYMENT_AMOUNT_MISMATCH","Thông tin merchant hoặc số tiền không khớp giao dịch.");
        boolean success="00".equals(params.get("vnp_ResponseCode"))&&"00".equals(params.get("vnp_TransactionStatus"));
        if(found.getStatus()==TransactionStatus.PENDING){
            found.setGatewayTransactionNo(params.get("vnp_TransactionNo"));found.setBankCode(params.get("vnp_BankCode"));found.setCardType(params.get("vnp_CardType"));found.setPayDate(params.get("vnp_PayDate"));found.setResponseCode(params.get("vnp_ResponseCode"));found.setStatus(success?TransactionStatus.SUCCESS:TransactionStatus.FAILED);found.setUpdatedAt(LocalDateTime.now(java.time.ZoneOffset.UTC));
            // Kết quả cổng thanh toán không thay quyền xác nhận đơn của người bán.
            if(success&&order.getStatus()==OrderStatus.CANCELLED)found.setNotes("Tiền về sau khi hủy đơn; cần đối chiếu và xử lý hoàn tiền.");
        }
        return new PaymentDtos.PaymentCallbackResult(found.getStatus()==TransactionStatus.SUCCESS,found.getTransactionCode(),order.getId(),order.getCode(),found.getAmount(),found.getStatus()==TransactionStatus.SUCCESS?"Đã xác minh giao dịch sandbox; chưa chứng minh tiền về tài khoản.":"Giao dịch không thành công.",found.getResponseCode(),found.getPayDate());
    }
    public PaymentDtos.PaymentCallbackResult mockConfirm(Long buyerId,String code){throw new MarketplaceException(403,"PAYMENT_MOCK_DISABLED","Không hỗ trợ xác nhận thanh toán giả lập.");}
    private void configured(){if(!enabled||vnpayTmnCode.isBlank()||vnpayHashSecret.isBlank()||!"https://sandbox.vnpayment.vn/paymentv2/vpcpay.html".equals(vnpayUrl))throw new MarketplaceException(503,"PAYMENT_NOT_CONFIGURED","VNPAY sandbox chưa được cấu hình.");}
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

    private String buildVnpayPaymentUrl(String txnRef, Order order, String ipAddress, String bankCode, String returnUrl) {
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnpayTmnCode);

        long amount = order.getTotal().multiply(new BigDecimal(100)).longValue();
        vnpParams.put("vnp_Amount", String.valueOf(amount));
        vnpParams.put("vnp_CurrCode", "VND");

        if (bankCode != null && !bankCode.isBlank()) {
            vnpParams.put("vnp_BankCode", bankCode);
        }

        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang BonsaiMarket " + order.getCode());
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", returnUrl);
        vnpParams.put("vnp_IpAddr", (ipAddress == null || ipAddress.isBlank()) ? "127.0.0.1" : ipAddress);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(cld.getTimeZone());
        String createDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_CreateDate", createDate);

        cld.add(Calendar.MINUTE, 15);
        String expireDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_ExpireDate", expireDate);

        return VNPayUtil.buildQueryUrl(vnpParams, vnpayHashSecret, vnpayUrl);
    }
}
