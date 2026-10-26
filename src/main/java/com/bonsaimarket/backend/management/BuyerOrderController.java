package com.bonsaimarket.backend.management;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Buyer projection and cancellation reuse the existing order service and inventory transaction. */
@RestController @RequestMapping("/api/account/orders")
public class BuyerOrderController {
    private final OrderManagementService orders;
    public BuyerOrderController(OrderManagementService orders) {this.orders=orders;}
    @GetMapping public Object list(@AuthenticationPrincipal AuthPrincipal actor,@RequestParam(defaultValue="0") int page,
        @RequestParam(required=false) String search,@RequestParam(required=false) String status,
        @RequestParam(required=false) String from,@RequestParam(required=false) String to) {
        return orders.buyerList(actor.id(),page,search,status,from,to);
    }
    @GetMapping("/{id}") public Object detail(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable Long id) {return orders.buyerDetail(actor.id(),id);}
    @PostMapping("/{id}/cancel") public Object cancel(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable Long id) {return orders.cancelByBuyer(actor.id(),id);}
}
