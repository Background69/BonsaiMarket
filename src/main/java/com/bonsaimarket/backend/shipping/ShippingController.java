package com.bonsaimarket.backend.shipping;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import com.bonsaimarket.backend.marketplace.MarketplaceException;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import static com.bonsaimarket.backend.shipping.ShippingService.Scope.*;

@RestController
public class ShippingController {
    private final ShippingService shipping;private final ShippingSettings settings;
    public ShippingController(ShippingService shipping,ShippingSettings settings){this.shipping=shipping;this.settings=settings;}
    public record Selection(@NotBlank @Pattern(regexp="[a-f0-9-]{36}") String quoteId) {}
    @GetMapping("/api/account/orders/{id}/shipping") public Object buyer(@AuthenticationPrincipal AuthPrincipal a,@PathVariable Long id){return shipping.overview(a.id(),id,BUYER);}
    @GetMapping("/api/seller/orders/{id}/shipping") public Object seller(@AuthenticationPrincipal AuthPrincipal a,@PathVariable Long id){return shipping.overview(a.id(),id,SELLER);}
    @GetMapping("/api/admin/orders/{id}/shipping") public Object admin(@AuthenticationPrincipal AuthPrincipal a,@PathVariable Long id){return shipping.overview(a.id(),id,ADMIN);}
    @PutMapping("/api/seller/orders/{id}/shipping/parcel") public Object parcel(@AuthenticationPrincipal AuthPrincipal a,@PathVariable Long id,@Valid @RequestBody ShippingService.ParcelWrite b){return shipping.saveParcel(a.id(),id,b);}
    @PostMapping("/api/account/orders/{id}/shipping/quotes") public Object quotes(@AuthenticationPrincipal AuthPrincipal a,@PathVariable Long id){return shipping.quotes(a.id(),id);}
    @PostMapping("/api/account/orders/{id}/shipping/selection") public Object select(@AuthenticationPrincipal AuthPrincipal a,@PathVariable Long id,@Valid @RequestBody Selection b){return shipping.select(a.id(),id,b.quoteId());}
    @PostMapping("/api/seller/orders/{id}/shipping/shipments") public Object create(@AuthenticationPrincipal AuthPrincipal a,@PathVariable Long id){return shipping.create(a.id(),id);}
    @PostMapping("/api/seller/orders/{id}/shipping/sync") public ResponseEntity<Void> sync(@AuthenticationPrincipal AuthPrincipal a,@PathVariable Long id){shipping.synchronize(a.id(),id,SELLER);return ResponseEntity.noContent().build();}
    @PostMapping("/api/admin/orders/{id}/shipping/sync") public ResponseEntity<Void> adminSync(@AuthenticationPrincipal AuthPrincipal a,@PathVariable Long id){shipping.synchronize(a.id(),id,ADMIN);return ResponseEntity.noContent().build();}
    @PostMapping("/api/seller/orders/{id}/shipping/cancel") public ResponseEntity<Void> cancel(@AuthenticationPrincipal AuthPrincipal a,@PathVariable Long id){shipping.cancel(a.id(),id);return ResponseEntity.noContent().build();}
    public record Callback(@JsonProperty("ShopID") @Min(1) int shopId,
        @JsonProperty("OrderCode") @NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,50}") String code,
        @JsonProperty("ClientOrderCode") @NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,50}") String reference,
        @JsonProperty("Type") @NotBlank @Size(max=40) String type,@JsonProperty("Status") @NotNull @Size(max=40) String status,
        @JsonProperty("Time") @NotNull Instant time,
        @JsonProperty("CODAmount") @Min(0) @Max(50000000) Integer codAmount,
        @JsonProperty("CODTransferDate") Instant transferAt,
        @JsonProperty("TotalFee") @DecimalMin("0") @DecimalMax("50000000") java.math.BigDecimal fee,
        @JsonProperty("PaymentType") Integer feePayer) {}
    @PostMapping("/api/shipping/webhooks/ghn") public ResponseEntity<Void> webhook(@RequestHeader(value="X-GHN-Webhook-Secret",required=false) String secret,
        HttpServletRequest request,@Valid @RequestBody Callback b){
        String expected=settings.getWebhookSecret();
        if(!settings.isEnabled()||expected.length()<32||secret==null||secret.length()>256||request.getHeader("Origin")!=null||request.getHeader("Sec-Fetch-Mode")!=null
            ||!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),secret.getBytes(StandardCharsets.UTF_8)))
            throw new MarketplaceException(403,"WEBHOOK_UNAUTHORIZED","Callback không được xác thực.");
        if(b.type().equals("cod"))shipping.codReport(b.shopId(),b.code(),b.reference(),b.time(),b.codAmount(),b.transferAt(),b.fee(),b.feePayer());
        else shipping.webhook(b.shopId(),b.code(),b.reference(),b.type(),b.status(),b.time());return ResponseEntity.noContent().build();
    }
}
