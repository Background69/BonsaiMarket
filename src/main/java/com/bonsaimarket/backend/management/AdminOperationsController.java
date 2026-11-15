package com.bonsaimarket.backend.management;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminOperationsController {
    private final AdminOverviewService overview;
    private final AdminCatalogService catalog;
    private final AdminPaymentReadService payments;
    private final AdminHistory history;

    public AdminOperationsController(AdminOverviewService overview, AdminCatalogService catalog, AdminPaymentReadService payments, AdminHistory history) {
        this.overview = overview;
        this.catalog = catalog;
        this.payments = payments;
        this.history = history;
    }

    public record Decision(@NotBlank @Size(max = 20) String action, @NotNull @Min(0) Long revision,
                           @NotBlank @Size(max = 1000) String reason) {
    }

    @GetMapping("/overview")
    public Object overview(@AuthenticationPrincipal AuthPrincipal actor, @RequestParam(defaultValue = "") String from, @RequestParam(defaultValue = "") String to) {
        return overview.get(actor.id(), from, to);
    }

    @GetMapping("/products")
    public Object products(@AuthenticationPrincipal AuthPrincipal actor, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "") String status, @RequestParam(required = false) Long category) {
        return catalog.products(actor.id(), page, search, status, category);
    }

    @GetMapping("/products/{id}")
    public Object product(@AuthenticationPrincipal AuthPrincipal actor, @PathVariable long id) {
        return catalog.product(actor.id(), id);
    }

    @PostMapping("/products/{id}/moderation")
    public Object moderate(@AuthenticationPrincipal AuthPrincipal actor, @PathVariable long id, @Valid @RequestBody Decision body) {
        return catalog.moderate(actor.id(), id, body.action(), body.revision(), body.reason());
    }

    @GetMapping("/stores")
    public Object stores(@AuthenticationPrincipal AuthPrincipal actor, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "") String status) {
        return catalog.stores(actor.id(), page, search, status);
    }

    @GetMapping("/stores/{id}")
    public Object store(@AuthenticationPrincipal AuthPrincipal actor, @PathVariable long id) {
        return catalog.store(actor.id(), id);
    }

    @PostMapping("/stores/{id}/status")
    public Object lifecycle(@AuthenticationPrincipal AuthPrincipal actor, @PathVariable long id, @Valid @RequestBody Decision body) {
        return catalog.lifecycle(actor.id(), id, body.action(), body.revision(), body.reason());
    }

    @GetMapping("/payment-records")
    public Object payments(@AuthenticationPrincipal AuthPrincipal actor, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "") String method) {
        return payments.list(actor.id(), page, search, method);
    }

    @GetMapping("/payment-records/summary")
    public Object paymentSummary(@AuthenticationPrincipal AuthPrincipal actor) {
        return payments.summary(actor.id());
    }

    @GetMapping("/users/{id}/history")
    public Object userHistory(@AuthenticationPrincipal AuthPrincipal actor, @PathVariable long id) {
        return history.target(actor.id(), "USER", id);
    }

    @GetMapping("/audit")
    public Object history(@AuthenticationPrincipal AuthPrincipal actor, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "") String type, @RequestParam(defaultValue = "") String action, @RequestParam(defaultValue = "") String from, @RequestParam(defaultValue = "") String to) {
        return history.list(actor.id(), page, search, type, action, from, to);
    }
}
