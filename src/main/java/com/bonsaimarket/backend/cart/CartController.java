package com.bonsaimarket.backend.cart;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Every endpoint acts on the signed-in user's own cart; /api/account/** already requires authentication. */
@RestController
@RequestMapping("/api/account/cart")
public class CartController {
    private final CartService service;

    public CartController(CartService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<CartDtos.View> cart(@AuthenticationPrincipal AuthPrincipal user) {
        return response(service.view(user.id()));
    }

    @PostMapping("/items")
    public ResponseEntity<CartDtos.View> add(@AuthenticationPrincipal AuthPrincipal user,
                                             @Valid @RequestBody CartDtos.AddRequest request) {
        return response(service.add(user.id(), request));
    }

    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<CartDtos.View> update(@AuthenticationPrincipal AuthPrincipal user,
                                                @PathVariable Long cartItemId, @Valid @RequestBody CartDtos.UpdateRequest request) {
        return response(service.updateQuantity(user.id(), cartItemId, request.quantity()));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<CartDtos.View> remove(@AuthenticationPrincipal AuthPrincipal user,
                                                @PathVariable Long cartItemId) {
        return response(service.remove(user.id(), cartItemId));
    }

    private <T> ResponseEntity<T> response(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}