package com.bonsaimarket.backend.order;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/account/orders")
public class AccountOrderController {
    private final AccountOrderService service;

    public AccountOrderController(AccountOrderService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listOrders(
            @AuthenticationPrincipal AuthPrincipal user,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return response(service.listOrders(user.id(), status, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountOrderDtos.OrderDetailPage> getOrderDetail(
            @AuthenticationPrincipal AuthPrincipal user,
            @PathVariable Long id) {
        return response(service.getOrderDetail(user.id(), id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<AccountOrderDtos.OrderDetailPage> cancelOrder(
            @AuthenticationPrincipal AuthPrincipal user,
            @PathVariable Long id,
            @RequestBody(required = false) AccountOrderDtos.CancelOrderRequest request) {
        String reason = request != null ? request.reason() : null;
        return response(service.cancelOrder(user.id(), id, reason));
    }

    private <T> ResponseEntity<T> response(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}
