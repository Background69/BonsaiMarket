package com.bonsaimarket.backend.banner;
import com.bonsaimarket.backend.auth.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api")
public class BannerController {
    private final BannerCampaigns campaigns;
    public BannerController(BannerCampaigns campaigns){this.campaigns=campaigns;}
    @GetMapping("/products/banner-winners") public Object winners(){return campaigns.winners();}
    @GetMapping("/seller/banner-slots") public Object slots(@AuthenticationPrincipal AuthPrincipal actor){return campaigns.slots(actor.id());}
    @GetMapping("/admin/banner-slots") public Object adminSlots(@AuthenticationPrincipal AuthPrincipal actor){return campaigns.slots(actor.id());}
    @PostMapping("/admin/banner-slots") public Object slot(@AuthenticationPrincipal AuthPrincipal actor,@Valid @RequestBody BannerCampaigns.SlotWrite body){return campaigns.createSlot(actor.id(),body.name());}
    @GetMapping("/{scope:seller|admin}/banner-campaigns") public Object list(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable String scope,@RequestParam(defaultValue="0") int page){return campaigns.list(actor.id(),page,scope.equals("admin"));}
    @GetMapping("/{scope:seller|admin}/banner-campaigns/{id}") public Object detail(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable String scope,@PathVariable long id){return campaigns.detail(actor.id(),id,scope.equals("admin"));}
    @PostMapping("/admin/banner-campaigns") public Object create(@AuthenticationPrincipal AuthPrincipal actor,@Valid @RequestBody BannerCampaigns.Create body){return campaigns.create(actor.id(),body);}
    @PostMapping("/admin/banner-campaigns/expire") public Object expire(@AuthenticationPrincipal AuthPrincipal actor){campaigns.expire(actor.id());return java.util.Map.of("updated",true);}
    @PostMapping("/seller/banner-campaigns/{id}/bids") public Object bid(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable long id,@Valid @RequestBody BannerCampaigns.BidWrite body){return campaigns.bid(actor.id(),id,body);}
    @PostMapping("/seller/banner-campaigns/{id}/bids/{bid}/withdraw") public Object withdraw(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable long id,@PathVariable long bid){return campaigns.withdraw(actor.id(),id,bid);}
    @PostMapping("/admin/banner-campaigns/{id}/bids/{bid}/{decision:approve|reject}") public Object moderate(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable long id,@PathVariable long bid,@PathVariable String decision,@Valid @RequestBody BannerCampaigns.Decision body){return campaigns.moderate(actor.id(),id,bid,decision.equals("approve"),body);}
    @PostMapping("/admin/banner-campaigns/{id}/{operation:settle|cancel}") public Object finish(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable long id,@PathVariable String operation,@Valid @RequestBody BannerCampaigns.Decision body){return operation.equals("settle")?campaigns.settle(actor.id(),id,body):campaigns.cancel(actor.id(),id,body);}
}
