package com.bonsaimarket.backend.community;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api")
public class CommunityInteractionController {
    private final CommunityInteractions interactions;
    public CommunityInteractionController(CommunityInteractions interactions){this.interactions=interactions;}
    @GetMapping("/community/posts/{id}/activity") public Object activity(@PathVariable long id,@AuthenticationPrincipal AuthPrincipal actor){return interactions.activity(id,actor==null?null:actor.id());}
    @GetMapping("/account/community/posts") public Object personal(@AuthenticationPrincipal AuthPrincipal actor,@RequestParam(defaultValue="MINE") String scope,@RequestParam(defaultValue="") String search,@RequestParam(defaultValue="") String topic,@RequestParam(defaultValue="") String type,@RequestParam(defaultValue="0") int page){return interactions.personal(actor.id(),scope,search,topic,type,page);}
    @RequestMapping(value="/account/community/posts/{id}/like",method={RequestMethod.PUT,RequestMethod.DELETE}) public Object like(@PathVariable long id,@AuthenticationPrincipal AuthPrincipal actor,jakarta.servlet.http.HttpServletRequest request){return interactions.reaction(actor.id(),id,"like",request.getMethod().equals("PUT"));}
    @RequestMapping(value="/account/community/posts/{id}/bookmark",method={RequestMethod.PUT,RequestMethod.DELETE}) public Object bookmark(@PathVariable long id,@AuthenticationPrincipal AuthPrincipal actor,jakarta.servlet.http.HttpServletRequest request){return interactions.reaction(actor.id(),id,"bookmark",request.getMethod().equals("PUT"));}
    @PutMapping("/account/community/posts/{id}/answer") public Object answer(@PathVariable long id,@AuthenticationPrincipal AuthPrincipal actor,@RequestBody CommunityInteractions.AnswerWrite body){return interactions.answer(actor.id(),id,body);}
    @PutMapping("/account/community/posts/{id}/help-status") public Object status(@PathVariable long id,@AuthenticationPrincipal AuthPrincipal actor,@RequestBody CommunityInteractions.StatusWrite body){return interactions.status(actor.id(),id,body);}
    @GetMapping("/account/community/products") public Object products(@AuthenticationPrincipal AuthPrincipal actor,@RequestParam(defaultValue="0") int page){return interactions.products(actor.id(),page);}
    @PutMapping("/account/community/posts/{id}/products") public Object links(@PathVariable long id,@AuthenticationPrincipal AuthPrincipal actor,@RequestBody CommunityInteractions.LinksWrite body){return interactions.links(actor.id(),id,body);}
    @GetMapping("/account/community/notifications") public Object notifications(@AuthenticationPrincipal AuthPrincipal actor,@RequestParam(defaultValue="0") int page){return interactions.notifications(actor.id(),page);}
}
