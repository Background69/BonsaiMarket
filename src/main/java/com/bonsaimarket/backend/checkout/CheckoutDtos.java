package com.bonsaimarket.backend.checkout;

import com.bonsaimarket.backend.address.AddressDtos;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public final class CheckoutDtos {
    private CheckoutDtos() { }
    public enum Source { CART, BUY_NOW }
    public record Request(@NotNull Source source, @NotNull @Positive Long addressId,
                          @Positive Long productId, @Min(1) @Max(1000000) Integer quantity,
                          @Size(max = 1000) String notes) { }
    public record Submit(@NotNull java.util.UUID quoteId) { }
    public record Item(Long cartItemId, Long productId, String name, String imageUrl,
                       Long storeId, String storeName, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) { }
    public record StoreTotal(Long storeId, String storeName, BigDecimal subtotal, BigDecimal shippingFee, BigDecimal total) { }
    public record Review(AddressDtos.ShippingSnapshot address, List<Item> items, List<StoreTotal> stores,
                         BigDecimal subtotal, BigDecimal shippingFee, BigDecimal total, String notes) { }
    public record Quote(String quoteId, String expiresAt, Review review) { }
    public record CartView(List<Item> items, List<AddressDtos.View> addresses, BigDecimal shippingFeePerStore) { }
    public record CreatedOrder(Long id, String code, Long storeId, String storeName, String status,
                               BigDecimal subtotal, BigDecimal shippingFee, BigDecimal total) { }
    public record Receipt(String quoteId, List<CreatedOrder> orders, BigDecimal total, String createdAt) { }
}
