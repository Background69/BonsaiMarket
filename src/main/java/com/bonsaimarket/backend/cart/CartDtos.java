package com.bonsaimarket.backend.cart;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public final class CartDtos {
    private CartDtos() { }

    /** Same upper bound as checkout (CheckoutDtos.Request.quantity). */
    public static final int MAX_QUANTITY = 1_000_000;
    /** Same bound as checkout: at most 100 distinct products per cart. */
    public static final int MAX_LINES = 100;

    public record AddRequest(@NotNull @Positive Long productId,
                             @NotNull @Min(1) @Max(MAX_QUANTITY) Integer quantity) { }

    public record UpdateRequest(@NotNull @Min(1) @Max(MAX_QUANTITY) Integer quantity) { }

    /**
     * One cart line. {@code stock} is null when the seller has not set it; {@code available} is false when the product,
     * its store or its category can no longer be sold, or when stock is zero.
     */
    public record Item(Long cartItemId, Long productId, String name, String imageUrl,
                       Long storeId, String storeName, BigDecimal unitPrice, int quantity, BigDecimal lineTotal,
                       Integer stock, boolean available) {
        // Keep existing checkout/browser consumers while exposing cartItemId to the Cart API.
        public Long id() { return cartItemId; }
        public Long getId() { return cartItemId; }
    }

    public record View(List<Item> items, BigDecimal subtotal) {
        public int quantity() { return items.stream().mapToInt(Item::quantity).sum(); }
        public int getQuantity() { return quantity(); }
    }
}
