package com.bonsaimarket.backend.management;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import com.bonsaimarket.backend.marketplace.ManagementPage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin statistics (10, 11), admin users (12a), admin orders (12c) and seller orders + revenue (13b). */
@RestController
@RequestMapping("/api")
public class ManagementController {
    private final StatisticsService statistics;
    private final UserAdminService users;
    private final OrderManagementService orders;

    public ManagementController(StatisticsService statistics, UserAdminService users, OrderManagementService orders) {
        this.statistics = statistics;
        this.users = users;
        this.orders = orders;
    }

    public record ActiveBody(@NotNull(message = "Chọn trạng thái tài khoản.") Boolean active,
                             @Size(max = 500, message = "Lý do tối đa 500 ký tự.") String reason) { }

    public record StatusBody(@NotBlank(message = "Chọn trạng thái mới.") @Size(max = 20, message = "Trạng thái không hợp lệ.") String status) { }

    @GetMapping("/admin/statistics")
    public StatisticsService.Overview adminStatistics(@AuthenticationPrincipal AuthPrincipal actor,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        return statistics.admin(actor.id(), from, to);
    }

    @GetMapping("/seller/statistics")
    public StatisticsService.Overview sellerStatistics(@AuthenticationPrincipal AuthPrincipal actor,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        return statistics.seller(actor.id(), from, to);
    }

    @GetMapping("/admin/users")
    public ManagementPage<UserAdminService.UserRow> users(@AuthenticationPrincipal AuthPrincipal actor, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "") String role, @RequestParam(defaultValue = "") String active) {
        return users.list(actor.id(), page, search, role, active);
    }

    @GetMapping("/admin/users/{id}")
    public UserAdminService.UserDetail user(@AuthenticationPrincipal AuthPrincipal actor, @PathVariable Long id) {
        return users.detail(actor.id(), id);
    }

    @PostMapping("/admin/users/{id}/active")
    public UserAdminService.UserRow setActive(@AuthenticationPrincipal AuthPrincipal actor, @PathVariable Long id, @Valid @RequestBody ActiveBody body) {
        return users.setActive(actor.id(), id, body.active(), body.reason());
    }

    @GetMapping("/admin/orders")
    public ManagementPage<OrderManagementService.OrderRow> adminOrders(@AuthenticationPrincipal AuthPrincipal actor, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String from, @RequestParam(defaultValue = "") String to) {
        return orders.list(actor.id(), true, page, search, status, from, to);
    }

    @GetMapping("/admin/orders/{id}")
    public OrderManagementService.OrderDetail adminOrder(@AuthenticationPrincipal AuthPrincipal actor, @PathVariable Long id) {
        return orders.detail(actor.id(), true, id);
    }

    @PostMapping("/admin/orders/{id}/status")
    public OrderManagementService.OrderDetail adminOrderStatus(@AuthenticationPrincipal AuthPrincipal actor, @PathVariable Long id, @Valid @RequestBody StatusBody body) {
        return orders.transition(actor.id(), true, id, body.status());
    }

    @GetMapping("/seller/orders")
    public ManagementPage<OrderManagementService.OrderRow> sellerOrders(@AuthenticationPrincipal AuthPrincipal actor, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String from, @RequestParam(defaultValue = "") String to) {
        return orders.list(actor.id(), false, page, search, status, from, to);
    }

    @GetMapping("/seller/orders/{id}")
    public OrderManagementService.OrderDetail sellerOrder(@AuthenticationPrincipal AuthPrincipal actor, @PathVariable Long id) {
        return orders.detail(actor.id(), false, id);
    }

    @PostMapping("/seller/orders/{id}/status")
    public OrderManagementService.OrderDetail sellerOrderStatus(@AuthenticationPrincipal AuthPrincipal actor, @PathVariable Long id, @Valid @RequestBody StatusBody body) {
        return orders.transition(actor.id(), false, id, body.status());
    }
}
