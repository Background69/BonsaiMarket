package com.bonsaimarket.backend.checkout;
import com.bonsaimarket.backend.auth.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class CheckoutController {
    private final CheckoutService checkout;
    public CheckoutController(CheckoutService checkout){this.checkout=checkout;}
    @GetMapping("/api/account/checkout") public Object preview(@AuthenticationPrincipal AuthPrincipal a){return checkout.preview(a.id());}
    @PostMapping("/api/account/checkout/quotes") public Object quotes(@AuthenticationPrincipal AuthPrincipal a,@Valid @RequestBody CheckoutService.QuoteInput b){return checkout.quote(a.id(),b);}
    @PostMapping("/api/account/checkout/orders") public Object place(@AuthenticationPrincipal AuthPrincipal a,@Valid @RequestBody CheckoutService.Place b){return checkout.place(a.id(),b);}
    @GetMapping("/api/account/checkout/requests/{key}") public Object recover(@AuthenticationPrincipal AuthPrincipal a,@PathVariable String key){return checkout.recover(a.id(),key);}
    @GetMapping("/api/seller/store/checkout-packages") public Object packages(@AuthenticationPrincipal AuthPrincipal a){return checkout.packages(a.id());}
    @PutMapping("/api/seller/store/checkout-packages/{id:[a-f0-9-]{36}}") public Object savePackage(@AuthenticationPrincipal AuthPrincipal a,@PathVariable String id,@Valid @RequestBody CheckoutService.PackageWrite b){return checkout.savePackage(a.id(),id,b);}
}
