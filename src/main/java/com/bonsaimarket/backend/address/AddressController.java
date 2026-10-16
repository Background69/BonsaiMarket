package com.bonsaimarket.backend.address;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/account/addresses")
public class AddressController {
    private final AddressService service;
    public AddressController(AddressService service) {this.service=service;}
    @GetMapping public ResponseEntity<List<AddressDtos.View>> list(@AuthenticationPrincipal AuthPrincipal user) {return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.list(user.id()));}
    @PostMapping public ResponseEntity<AddressDtos.View> create(@AuthenticationPrincipal AuthPrincipal user,@Valid @RequestBody AddressDtos.Write body) {
        var result=service.create(user.id(),body);return ResponseEntity.created(java.net.URI.create("/api/account/addresses/"+result.id())).cacheControl(CacheControl.noStore()).body(result);
    }
    @PutMapping("/{id}") public AddressDtos.View update(@AuthenticationPrincipal AuthPrincipal user,@PathVariable Long id,@Valid @RequestBody AddressDtos.Write body) {return service.update(user.id(),id,body);}
    @PatchMapping("/{id}/default") public AddressDtos.View makeDefault(@AuthenticationPrincipal AuthPrincipal user,@PathVariable Long id) {return service.makeDefault(user.id(),id);}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@AuthenticationPrincipal AuthPrincipal user,@PathVariable Long id) {service.delete(user.id(),id);}
}
