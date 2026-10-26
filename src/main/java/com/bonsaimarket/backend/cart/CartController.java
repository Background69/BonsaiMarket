package com.bonsaimarket.backend.cart;
import com.bonsaimarket.backend.auth.AuthPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/account/cart")
public class CartController {
    private final CartService cart;
    public CartController(CartService cart){this.cart=cart;}
    public record Add(@NotNull @Positive Long productId,@Min(1) @Max(1000000) int quantity) {}
    public record Quantity(@Min(0) @Max(1000000) int quantity) {}
    @GetMapping public Object get(@AuthenticationPrincipal AuthPrincipal a){return cart.view(a.id());}
    @PostMapping("/items") public Object add(@AuthenticationPrincipal AuthPrincipal a,@Valid @RequestBody Add b){return cart.add(a.id(),b.productId(),b.quantity());}
    @PutMapping("/items/{id}") public Object update(@AuthenticationPrincipal AuthPrincipal a,@PathVariable Long id,@Valid @RequestBody Quantity b){return cart.update(a.id(),id,b.quantity());}
    @DeleteMapping("/items/{id}") public Object remove(@AuthenticationPrincipal AuthPrincipal a,@PathVariable Long id){return cart.update(a.id(),id,0);}
}
