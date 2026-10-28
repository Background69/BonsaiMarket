package com.bonsaimarket.backend.payment;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class PaymentDtos {
    private PaymentDtos() {}

    public record CreatePaymentRequest(
        @NotNull @jakarta.validation.constraints.Positive Long orderId,
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Pattern(regexp="VNPAY|MOMO|BANKING|COD") String paymentMethod, // VNPAY, MOMO, BANKING, COD
        @jakarta.validation.constraints.Pattern(regexp="[A-Za-z0-9]{1,30}") String bankCode,
        @jakarta.validation.constraints.Size(max=2048) String returnUrl
    ) {}

    public record PaymentInitResponse(
        String transactionCode,
        String paymentMethod,
        BigDecimal amount,
        String paymentUrl,
        Map<String, Object> extraData
    ) {}

    public record PaymentCallbackResult(
        boolean success,
        String transactionCode,
        Long orderId,
        String orderCode,
        BigDecimal amount,
        String message,
        String responseCode,
        String payDate
    ) {}

    public record TransactionView(
        Long id,
        String transactionCode,
        String paymentMethod,
        BigDecimal amount,
        String status,
        String gatewayTransactionNo,
        String bankCode,
        String responseCode,
        String createdAt
    ) {}

    public record BankTransferInfo(
        String bankName,
        String bankCode,
        String accountNumber,
        String accountHolder,
        BigDecimal amount,
        String transferContent,
        String qrUrl
    ) {}
}
