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

    // Default VietQR mock banking details
    private final String defaultBankCode = "MB";
    private final String defaultBankName = "MB Bank (Ngân hàng Quân Đội)";
    private final String defaultAccountNumber = "999988886666";
    private final String defaultAccountHolder = "BONSAI MARKET VN";

    public PaymentService(
            OrderRepository orders,
            TransactionRepository transactions,
            UserRepository users,
            @Value("${vnpay.tmn-code:CGXZLS0Z}") String vnpayTmnCode,
            @Value("${vnpay.hash-secret:XNBCJFAKAZQSGUTUYQBBLNETVOXAPRVN}") String vnpayHashSecret,
            @Value("${vnpay.url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}") String vnpayUrl,
            @Value("${auth.frontend-origin:http://localhost:5173}") String frontendOrigin) {
        this.orders = orders;
        this.transactions = transactions;
        this.users = users;
        this.vnpayTmnCode = vnpayTmnCode;
        this.vnpayHashSecret = vnpayHashSecret;
        this.vnpayUrl = vnpayUrl;
        this.frontendOrigin = frontendOrigin;
    }

    public PaymentDtos.PaymentInitResponse createPayment(Long buyerId, PaymentDtos.CreatePaymentRequest request, String clientIp) {
        Order order = orders.findById(request.orderId())
                .orElseThrow(MarketplaceException::missing);

        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new MarketplaceException(403, "FORBIDDEN", "Bạn không có quyền thực hiện thao tác này.");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw MarketplaceException.invalid("Không thể thanh toán cho đơn hàng đã hủy.");
        }

        String method = request.paymentMethod().toUpperCase().trim();
        String txnCode = "TXN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        var txn = new Transaction();
        txn.setOrder(order);
        txn.setTransactionCode(txnCode);
        txn.setPaymentMethod(method);
        txn.setAmount(order.getTotal());
        txn.setStatus(TransactionStatus.PENDING);
        txn.setCreatedAt(LocalDateTime.now());
        txn.setUpdatedAt(LocalDateTime.now());
        transactions.save(txn);

        String paymentUrl = null;
        Map<String, Object> extra = new HashMap<>();

        if ("VNPAY".equals(method)) {
            String returnUrl = (request.returnUrl() != null && !request.returnUrl().isBlank())
                    ? request.returnUrl()
                    : frontendOrigin + "/payment/callback";
            paymentUrl = buildVnpayPaymentUrl(txnCode, order, clientIp, request.bankCode(), returnUrl);
        } else if ("BANKING".equals(method)) {
            PaymentDtos.BankTransferInfo bankInfo = buildBankTransferInfo(order, txnCode);
            extra.put("bankInfo", bankInfo);
            paymentUrl = frontendOrigin + "/payment/banking?orderId=" + order.getId() + "&txnCode=" + txnCode;
        } else if ("MOMO".equals(method)) {
            // Simulated MoMo Sandbox checkout redirect
            paymentUrl = frontendOrigin + "/payment/callback?method=MOMO&orderId=" + order.getId() + "&txnCode=" + txnCode;
            extra.put("message", "Cổng MoMo Sandbox sẵn sàng xử lý.");
        } else if ("COD".equals(method)) {
            txn.setNotes("Thanh toán tiền mặt khi nhận hàng (COD)");
            paymentUrl = frontendOrigin + "/orders/" + order.getId();
            extra.put("message", "Đã ghi nhận phương thức thanh toán COD.");
        } else {
            throw MarketplaceException.invalid("Phương thức thanh toán không hợp lệ: " + method);
        }

        return new PaymentDtos.PaymentInitResponse(txnCode, method, order.getTotal(), paymentUrl, extra);
    }

    public PaymentDtos.PaymentCallbackResult processVnpayCallback(Map<String, String> params) {
        boolean validSignature = VNPayUtil.verifyCallback(params, vnpayHashSecret);
        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo");
        String bankCode = params.get("vnp_BankCode");
        String cardType = params.get("vnp_CardType");
        String payDate = params.get("vnp_PayDate");

        Optional<Transaction> opt = transactions.findByTransactionCode(txnRef);
        if (opt.isEmpty()) {
            return new PaymentDtos.PaymentCallbackResult(
                    false, txnRef, null, null, BigDecimal.ZERO, "Giao dịch không tồn tại", responseCode, payDate
            );
        }

        Transaction txn = opt.get();
        Order order = txn.getOrder();

        if (!validSignature) {
            txn.setStatus(TransactionStatus.FAILED);
            txn.setResponseCode("INVALID_SIGNATURE");
            txn.setNotes("Chữ ký bảo mật không khớp");
            txn.setUpdatedAt(LocalDateTime.now());
            transactions.save(txn);
            return new PaymentDtos.PaymentCallbackResult(
                    false, txnRef, order.getId(), order.getCode(), txn.getAmount(), "Chữ ký bảo mật không hợp lệ", "INVALID_SIGNATURE", payDate
            );
        }

        boolean isSuccess = "00".equals(responseCode);
        txn.setGatewayTransactionNo(transactionNo);
        txn.setBankCode(bankCode);
        txn.setCardType(cardType);
        txn.setResponseCode(responseCode);
        txn.setPayDate(payDate);
        txn.setStatus(isSuccess ? TransactionStatus.SUCCESS : TransactionStatus.FAILED);
        txn.setUpdatedAt(LocalDateTime.now());
        transactions.save(txn);

        if (isSuccess && order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setUpdatedAt(LocalDateTime.now());
            orders.save(order);
        }

        String msg = isSuccess ? "Giao dịch thanh toán VNPay thành công." : "Thanh toán không thành công. Mã lỗi: " + responseCode;
        return new PaymentDtos.PaymentCallbackResult(
                isSuccess, txnRef, order.getId(), order.getCode(), txn.getAmount(), msg, responseCode, payDate
        );
    }

    public PaymentDtos.PaymentCallbackResult mockConfirm(Long buyerId, String txnCode) {
        Transaction txn = transactions.findByTransactionCode(txnCode)
                .orElseThrow(MarketplaceException::missing);

        Order order = txn.getOrder();
        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new MarketplaceException(403, "FORBIDDEN", "Bạn không có quyền thực hiện thao tác này.");
        }

        txn.setStatus(TransactionStatus.SUCCESS);
        txn.setGatewayTransactionNo("MOCK-" + System.currentTimeMillis());
        txn.setResponseCode("00");
        txn.setPayDate(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        txn.setUpdatedAt(LocalDateTime.now());
        transactions.save(txn);

        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setUpdatedAt(LocalDateTime.now());
            orders.save(order);
        }

        return new PaymentDtos.PaymentCallbackResult(
                true, txnCode, order.getId(), order.getCode(), txn.getAmount(), "Xác nhận thanh toán thành công (Môi trường thử nghiệm).", "00", txn.getPayDate()
        );
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
                        t.getCreatedAt().toString()
                )).toList();
    }

    @Transactional(readOnly = true)
    public PaymentDtos.BankTransferInfo getBankTransferInfo(Long buyerId, Long orderId) {
        Order order = orders.findById(orderId).orElseThrow(MarketplaceException::missing);
        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new MarketplaceException(403, "FORBIDDEN", "Bạn không có quyền thực hiện thao tác này.");
        }
        return buildBankTransferInfo(order, order.getCode());
    }

    private PaymentDtos.BankTransferInfo buildBankTransferInfo(Order order, String refCode) {
        String content = "BSM " + order.getCode();
        long amountLong = order.getTotal().longValue();
        String encodedHolder = URLEncoder.encode(defaultAccountHolder, StandardCharsets.UTF_8);
        String encodedContent = URLEncoder.encode(content, StandardCharsets.UTF_8);
        String qrUrl = String.format(
                "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                defaultBankCode, defaultAccountNumber, amountLong, encodedContent, encodedHolder
        );

        return new PaymentDtos.BankTransferInfo(
                defaultBankName,
                defaultBankCode,
                defaultAccountNumber,
                defaultAccountHolder,
                order.getTotal(),
                content,
                qrUrl
        );
    }

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

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String createDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_CreateDate", createDate);

        cld.add(Calendar.MINUTE, 15);
        String expireDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_ExpireDate", expireDate);

        return VNPayUtil.buildQueryUrl(vnpParams, vnpayHashSecret, vnpayUrl);
    }
}
