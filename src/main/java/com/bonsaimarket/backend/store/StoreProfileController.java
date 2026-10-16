package com.bonsaimarket.backend.store;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import com.bonsaimarket.backend.marketplace.ManagementPage;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import static com.bonsaimarket.backend.store.StoreProfileDtos.*;

@RestController @RequestMapping("/api")
public class StoreProfileController {
    private final StoreProfileService stores;
    public StoreProfileController(StoreProfileService stores) { this.stores=stores; }
    @GetMapping("/seller/store") public StoreView store(@AuthenticationPrincipal AuthPrincipal actor) {return stores.get(actor.id());}
    @PutMapping("/seller/store") public StoreView store(@AuthenticationPrincipal AuthPrincipal actor,@Valid @RequestBody StoreWrite body) {return stores.update(actor.id(),body);}
}
