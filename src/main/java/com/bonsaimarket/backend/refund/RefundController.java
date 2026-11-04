package com.bonsaimarket.backend.refund;
import com.bonsaimarket.backend.auth.AuthPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api")
public class RefundController {
    private final RefundService service;
    public RefundController(RefundService service){this.service=service;}
    @GetMapping({"/{scope:account|seller|admin}/orders/{id}/refund"})
    public Object overview(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable String scope,@PathVariable long id){return service.overview(actor.id(),id,scope);}
    @PostMapping({"/{scope:account|admin}/orders/{id}/refund"})
    public Object request(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable String scope,@PathVariable long id,@RequestBody RefundService.Write input){return service.request(actor.id(),id,scope,input);}
    @GetMapping("/admin/refunds") public Object queue(@AuthenticationPrincipal AuthPrincipal actor,@RequestParam(defaultValue="0") int page){return service.queue(actor.id(),page);}
    @PostMapping("/admin/refunds/{id}/status") public Object decide(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable long id,@RequestBody RefundService.Decision input){return service.decide(actor.id(),id,input);}
}
