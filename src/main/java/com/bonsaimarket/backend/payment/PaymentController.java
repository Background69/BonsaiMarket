package com.bonsaimarket.backend.payment;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/account/payments")
public class PaymentController {
    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping("/init")
    public ResponseEntity<PaymentDtos.PaymentInitResponse> initPayment(
            @AuthenticationPrincipal AuthPrincipal user,
            @Valid @RequestBody PaymentDtos.CreatePaymentRequest request,
            HttpServletRequest servletRequest) {
        String clientIp = servletRequest.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = servletRequest.getRemoteAddr();
        } else {
            clientIp = clientIp.split(",")[0].trim();
        }
        return response(service.createPayment(user.id(), request, clientIp));
    }

    @PostMapping("/vnpay-callback")
    public ResponseEntity<PaymentDtos.PaymentCallbackResult> handleVnpayCallback(
            @AuthenticationPrincipal AuthPrincipal user,
            @RequestBody Map<String, String> params) {
        return response(service.processVnpayCallback(params));
    }

    @PostMapping("/mock-confirm/{txnCode}")
    public ResponseEntity<PaymentDtos.PaymentCallbackResult> mockConfirm(
            @AuthenticationPrincipal AuthPrincipal user,
            @PathVariable String txnCode) {
        return response(service.mockConfirm(user.id(), txnCode));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentDtos.TransactionView>> getOrderTransactions(
            @AuthenticationPrincipal AuthPrincipal user,
            @PathVariable Long orderId) {
        return response(service.getTransactionsForOrder(user.id(), orderId));
    }

    @GetMapping("/banking/{orderId}")
    public ResponseEntity<PaymentDtos.BankTransferInfo> getBankTransferInfo(
            @AuthenticationPrincipal AuthPrincipal user,
            @PathVariable Long orderId) {
        return response(service.getBankTransferInfo(user.id(), orderId));
    }

    private <T> ResponseEntity<T> response(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}
