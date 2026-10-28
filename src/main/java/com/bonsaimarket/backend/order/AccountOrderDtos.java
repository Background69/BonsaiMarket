package com.bonsaimarket.backend.order;

import java.math.BigDecimal;
import java.util.List;

public final class AccountOrderDtos {
    private AccountOrderDtos() {}

    public record ItemSummary(
        Long id,
        Long productId,
        String productName,
        String imageUrl,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal
    ) { public String getName(){return productName;} }

    public record OrderSummary(
        Long id,
        String code,
        String status,
        Long storeId,
        String storeName,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal total,
        String createdAt,
        int itemCount,
        int totalQuantity,
        List<ItemSummary> items
    ) { public int getQuantity(){return totalQuantity;} }

    public record OrderDetailPage(
        Long id,
        String code,
        String status,
        Long storeId,
        String storeName,
        String shippingAddressJson,
        String shippingMethod,
        BigDecimal shippingFee,
        BigDecimal subtotal,
        BigDecimal total,
        String notes,
        String createdAt,
        String updatedAt,
        boolean canCancel,
        boolean canPay,
        List<ItemSummary> items, List<String> actions, com.bonsaimarket.backend.payment.OrderPayment payment,
        String shippingAddress, com.bonsaimarket.backend.management.OrderManagementService.Party buyer
    ) {}

    public record CancelOrderRequest(
        String reason
    ) {}
}
