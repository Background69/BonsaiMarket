package com.bonsaimarket.backend.product;
import com.bonsaimarket.backend.auth.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/seller/products")
public class SellerProductController {
    private final SellerProducts products;
    public SellerProductController(SellerProducts products){this.products=products;}
    @PostMapping public SellerProducts.View create(@AuthenticationPrincipal AuthPrincipal actor,@Valid @RequestBody SellerProducts.Create body){return products.create(actor.id(),body);}
    @GetMapping("/{id}") public SellerProducts.View get(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable long id){return products.get(actor.id(),id);}
    @GetMapping("/{id}/moderation") public java.util.List<SellerProducts.Moderation> moderation(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable long id){return products.moderation(actor.id(),id);}
    @PutMapping("/{id}") public SellerProducts.View edit(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable long id,@Valid @RequestBody SellerProducts.Edit body){return products.edit(actor.id(),id,body);}
    @PostMapping("/{id}/submit") public SellerProducts.View submit(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable long id,@Valid @RequestBody SellerProducts.Revision body){return products.submit(actor.id(),id,body.revision());}
}
