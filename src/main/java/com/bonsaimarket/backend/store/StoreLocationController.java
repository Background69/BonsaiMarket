package com.bonsaimarket.backend.store;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class StoreLocationController {
    private final StoreLocations locations;
    public StoreLocationController(StoreLocations locations) {this.locations=locations;}
    @GetMapping("/api/seller/store/location") public Object settings(@AuthenticationPrincipal AuthPrincipal a){return locations.settings(a.id());}
    @PutMapping("/api/seller/store/location") public Object save(@AuthenticationPrincipal AuthPrincipal a,@Valid @RequestBody StoreLocations.Write b){return locations.save(a.id(),b);}
    @GetMapping("/api/stores/locations") public Object list(@RequestParam(required=false) String search,@RequestParam(required=false) String region,
        @RequestParam(required=false) Long categoryId,@RequestParam(required=false) Long storeId,@RequestParam(defaultValue="0") int page){return locations.discover(search,region,categoryId,storeId,page);}
}
