package com.bonsaimarket.backend.product;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/seller/products/{id}/content")
public class ProductContentController {
    private final ProductContentService service;
    public ProductContentController(ProductContentService service) {this.service=service;}
    @GetMapping public ProductContentService.View get(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable Long id) {return service.get(actor.id(),id);}
    @PutMapping public ProductContentService.View update(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable Long id,@Valid @RequestBody ProductContentService.Write body) {return service.update(actor.id(),id,body);}
}
