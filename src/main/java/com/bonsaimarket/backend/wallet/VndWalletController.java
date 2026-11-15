package com.bonsaimarket.backend.wallet;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api")
public class VndWalletController {
    private final VndWallet wallet;
    public VndWalletController(VndWallet wallet){this.wallet=wallet;}
    public record Request(String type,BigDecimal amount,String requestKey,String password) {}
    public record Simulation(String outcome) {}
    private Object response(Object body){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);}
    @GetMapping("/account/vnd-wallet") public Object balance(@AuthenticationPrincipal AuthPrincipal actor){return response(wallet.balance(actor.id()));}
    @PostMapping("/account/vnd-wallet/test-bank") public Object bank(@AuthenticationPrincipal AuthPrincipal actor){return response(wallet.linkBank(actor.id()));}
    @PostMapping("/account/vnd-wallet/requests") public Object request(@AuthenticationPrincipal AuthPrincipal actor,@RequestBody Request b){return response(wallet.request(actor.id(),b.type(),b.amount(),b.requestKey(),b.password()));}
    @GetMapping("/account/vnd-wallet/requests/{key}") public Object recover(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable String key){return response(wallet.recovery(actor.id(),key));}
    @PostMapping("/account/vnd-wallet/transactions/{id}/simulate") public Object simulate(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable String id,@RequestBody Simulation b){return response(wallet.simulate(actor.id(),id,b.outcome()));}
    @GetMapping("/account/vnd-wallet/transactions") public Object history(@AuthenticationPrincipal AuthPrincipal actor,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="") String type,@RequestParam(defaultValue="") String status,@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to){return response(wallet.history(actor.id(),page,type,status,from,to));}
    @GetMapping("/account/vnd-wallet/transactions/{id}") public Object detail(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable String id){return response(wallet.detail(actor.id(),id));}
    @GetMapping("/admin/vnd-wallet/reconciliation") public Object reconciliation(@AuthenticationPrincipal AuthPrincipal actor){return response(wallet.reconcile(actor.id()));}
}
