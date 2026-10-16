package com.bonsaimarket.backend.seller.application;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import com.bonsaimarket.backend.marketplace.ManagementPage;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import static com.bonsaimarket.backend.seller.application.SellerApplicationDtos.*;

@RestController @RequestMapping("/api")
public class SellerApplicationController {
    private final SellerApplicationService applications;
    public SellerApplicationController(SellerApplicationService applications) { this.applications=applications; }
    @GetMapping("/account/seller-application") public ResponseEntity<ApplicationView> application(@AuthenticationPrincipal AuthPrincipal actor) {var result=applications.current(actor.id());return result==null?ResponseEntity.noContent().build():ResponseEntity.ok(result);}
    @PostMapping("/account/seller-application") @ResponseStatus(HttpStatus.CREATED) public ApplicationView submit(@AuthenticationPrincipal AuthPrincipal actor,@Valid @RequestBody ApplicationWrite body) {return applications.submit(actor.id(),body);}
    @GetMapping("/admin/store-applications") public ManagementPage<ApplicationView> applications(@AuthenticationPrincipal AuthPrincipal actor,@RequestParam(defaultValue="PENDING") String status,@RequestParam(defaultValue="0") int page) {return applications.list(actor.id(),status,page);}
    @PatchMapping("/admin/store-applications/{id}") public ApplicationView decide(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable Long id,@Valid @RequestBody Decision body) {return applications.decide(actor.id(),id,body);}
}
