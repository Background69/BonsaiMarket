package com.bonsaimarket.backend.checkout;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/account/checkout")
public class CheckoutController {
    private final CheckoutService service;
    public CheckoutController(CheckoutService service) { this.service = service; }
    @GetMapping public ResponseEntity<CheckoutDtos.CartView> cart(@AuthenticationPrincipal AuthPrincipal user) {
        return response(service.cart(user.id()));
    }
    @PostMapping("/quote") public ResponseEntity<CheckoutDtos.Quote> quote(@AuthenticationPrincipal AuthPrincipal user,
            @Valid @RequestBody CheckoutDtos.Request request) { return response(service.quote(user.id(), request)); }
    @PostMapping("/orders") public ResponseEntity<CheckoutDtos.Receipt> submit(@AuthenticationPrincipal AuthPrincipal user,
            @Valid @RequestBody CheckoutDtos.Submit request) { return response(service.submit(user.id(), request.quoteId().toString())); }
    @GetMapping("/quotes/{id}") public ResponseEntity<CheckoutDtos.Quote> savedQuote(@AuthenticationPrincipal AuthPrincipal user,
            @PathVariable UUID id) { return response(service.savedQuote(user.id(), id.toString())); }
    @GetMapping("/receipts/{id}") public ResponseEntity<CheckoutDtos.Receipt> receipt(@AuthenticationPrincipal AuthPrincipal user,
            @PathVariable UUID id) { return response(service.receipt(user.id(), id.toString())); }
    private <T> ResponseEntity<T> response(T body) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body); }
}
