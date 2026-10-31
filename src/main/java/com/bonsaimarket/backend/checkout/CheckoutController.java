package com.bonsaimarket.backend.checkout;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/account/checkout")
public class CheckoutController {
    private final CheckoutService service;
    private final tools.jackson.databind.ObjectMapper json;
    private final jakarta.validation.Validator validator;
    public CheckoutController(CheckoutService service, tools.jackson.databind.ObjectMapper json, jakarta.validation.Validator validator) {
        this.service = service; this.json = json; this.validator = validator;
    }
    private void validate(Object body) {
        if (body==null || !validator.validate(body).isEmpty()) throw com.bonsaimarket.backend.marketplace.MarketplaceException.invalid("Thông tin đặt hàng không hợp lệ.");
    }
    private <T> T input(tools.jackson.databind.JsonNode request, Class<T> type) {
        try {return json.treeToValue(request,type);}
        catch (RuntimeException e) {throw com.bonsaimarket.backend.marketplace.MarketplaceException.invalid("Dữ liệu đặt hàng không hợp lệ.");}
    }
    @GetMapping public ResponseEntity<Object> cart(@AuthenticationPrincipal AuthPrincipal user, @RequestParam(required=false) String view) {
        return response("shipping".equals(view)?service.preview(user.id()):service.cart(user.id()));
    }
    @PostMapping("/quote") public ResponseEntity<CheckoutDtos.Quote> quote(@AuthenticationPrincipal AuthPrincipal user,
            @Valid @RequestBody CheckoutDtos.Request request) { return response(service.quote(user.id(), request)); }
    @PostMapping("/orders") public ResponseEntity<?> submit(@AuthenticationPrincipal AuthPrincipal user,
            @RequestBody tools.jackson.databind.JsonNode request) {
        if(request==null||!request.isObject())throw com.bonsaimarket.backend.marketplace.MarketplaceException.invalid("Dữ liệu đặt hàng không hợp lệ.");
        if(request.has("idempotencyKey")){var body=input(request,CheckoutService.Place.class);validate(body);return response(service.place(user.id(),body));}
        var body=input(request,CheckoutDtos.Submit.class);validate(body);return response(service.submit(user.id(),body.quoteId().toString(),body.shippingQuoteId()==null?null:body.shippingQuoteId().toString()));
    }
    @GetMapping("/quotes/{id}") public ResponseEntity<CheckoutDtos.Quote> savedQuote(@AuthenticationPrincipal AuthPrincipal user,
            @PathVariable UUID id) { return response(service.savedQuote(user.id(), id.toString())); }
    @GetMapping("/receipts/{id}") public ResponseEntity<CheckoutDtos.Receipt> receipt(@AuthenticationPrincipal AuthPrincipal user,
            @PathVariable UUID id) { return response(service.receipt(user.id(), id.toString())); }
    @PostMapping("/quotes") public Object shippingQuote(@AuthenticationPrincipal AuthPrincipal user,@Valid @RequestBody CheckoutService.QuoteInput body){return response(service.quote(user.id(),body));}
    @GetMapping("/requests/{key}") public Object recover(@AuthenticationPrincipal AuthPrincipal user,@PathVariable String key){return response(service.recover(user.id(),key));}
    private <T> ResponseEntity<T> response(T body) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body); }
}

@RestController
@RequestMapping("/api/seller/store/checkout-packages")
class CheckoutPackageController {
    private final CheckoutService service;
    CheckoutPackageController(CheckoutService service){this.service=service;}
    @GetMapping public Object packages(@AuthenticationPrincipal AuthPrincipal user){return service.packages(user.id());}
    @PutMapping("/{id}") public Object savePackage(@AuthenticationPrincipal AuthPrincipal user,@PathVariable UUID id,@Valid @RequestBody CheckoutService.PackageWrite body){return service.savePackage(user.id(),id.toString(),body);}
}
