package com.bonsaimarket.backend.returns;
import com.bonsaimarket.backend.auth.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController
public class OrderReturnController {
    private final OrderReturnService returns;
    public OrderReturnController(OrderReturnService returns){this.returns=returns;}
    @GetMapping("/api/account/orders/{id}/return") public Object buyer(@AuthenticationPrincipal AuthPrincipal a,@PathVariable Long id){return returns.overview(a.id(),id,"account");}
    @GetMapping("/api/seller/orders/{id}/return") public Object seller(@AuthenticationPrincipal AuthPrincipal a,@PathVariable Long id){return returns.overview(a.id(),id,"seller");}
    @GetMapping("/api/admin/orders/{id}/return") public Object admin(@AuthenticationPrincipal AuthPrincipal a,@PathVariable Long id){return returns.overview(a.id(),id,"admin");}
    @PostMapping("/api/account/orders/{id}/return") public Object request(@AuthenticationPrincipal AuthPrincipal a,@PathVariable Long id,@Valid @RequestBody OrderReturnService.Request b){return returns.request(a.id(),id,b);}
    @PostMapping("/api/seller/orders/{id}/return/approve") public Object approve(@AuthenticationPrincipal AuthPrincipal a,@PathVariable Long id){return returns.approve(a.id(),id);}
    @PostMapping("/api/seller/orders/{id}/return/inspection") public Object inspect(@AuthenticationPrincipal AuthPrincipal a,@PathVariable Long id,@Valid @RequestBody OrderReturnService.Inspection b){return returns.inspect(a.id(),id,b);}
    @PostMapping("/api/seller/orders/{id}/return/carrier") public Object carrier(@AuthenticationPrincipal AuthPrincipal a,@PathVariable Long id){return returns.carrierReturn(a.id(),id);}
}
