package com.bonsaimarket.backend.wallet;
import com.bonsaimarket.backend.auth.AuthPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api")
public class WalletController {
    private final WalletCredits wallet;
    public WalletController(WalletCredits wallet){this.wallet=wallet;}
    public record Grant(@NotNull @Positive Long ownerId,@NotNull BigDecimal credits,@NotNull UUID requestKey,@NotBlank @Size(min=8,max=1000) String reason) {}
    @GetMapping({"/account/wallet","/account/banner-credits"}) public Object balance(@AuthenticationPrincipal AuthPrincipal actor){return wallet.balance(actor.id());}
    @GetMapping({"/account/wallet/transactions","/account/banner-credits/transactions"}) public Object history(@AuthenticationPrincipal AuthPrincipal actor,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="") String type,@RequestParam(defaultValue="") String status){return wallet.history(actor.id(),page,type,status);}
    @GetMapping({"/account/wallet/transactions/{id}","/account/banner-credits/transactions/{id}"}) public Object detail(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable String id){return wallet.detail(actor.id(),id);}
    @PostMapping("/admin/wallet/grants") public Object grant(@AuthenticationPrincipal AuthPrincipal actor,@Valid @RequestBody Grant body){return wallet.grant(actor.id(),body.ownerId(),body.credits(),body.requestKey(),body.reason());}
    @GetMapping("/admin/wallet/reconciliation") public Object reconcile(@AuthenticationPrincipal AuthPrincipal actor){return wallet.reconcile(actor.id());}
}
