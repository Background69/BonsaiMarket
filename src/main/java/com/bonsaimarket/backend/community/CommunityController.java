package com.bonsaimarket.backend.community;
import com.bonsaimarket.backend.auth.AuthPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api")
public class CommunityController {
    private final CommunityService service;
    public CommunityController(CommunityService service){this.service=service;}
    @GetMapping("/community/posts") public Object list(@RequestParam(defaultValue="") String search,@RequestParam(defaultValue="") String topic,@RequestParam(defaultValue="0") int page){return service.list(search,topic,page);}
    @GetMapping("/community/posts/{id}") public Object post(@PathVariable long id){return service.post(id);}
    @GetMapping("/community/posts/{id}/comments") public Object comments(@PathVariable long id,@RequestParam(defaultValue="0") int page){return service.comments(id,page);}
    @PostMapping("/account/community/posts") public Object create(@AuthenticationPrincipal AuthPrincipal actor,@RequestBody CommunityService.PostWrite body){return service.create(actor.id(),body);}
    @PutMapping("/account/community/posts/{id}") public Object edit(@PathVariable long id,@AuthenticationPrincipal AuthPrincipal actor,@RequestBody CommunityService.PostWrite body){return service.edit(id,actor.id(),body);}
    @DeleteMapping("/account/community/posts/{id}") public void remove(@PathVariable long id,@AuthenticationPrincipal AuthPrincipal actor,@RequestParam long revision){service.removePost(id,actor.id(),revision);}
    @PostMapping("/account/community/posts/{id}/comments") public Object comment(@PathVariable long id,@AuthenticationPrincipal AuthPrincipal actor,@RequestBody CommunityService.CommentWrite body){return service.comment(id,actor.id(),body);}
    @PutMapping("/account/community/comments/{id}") public Object editComment(@PathVariable long id,@AuthenticationPrincipal AuthPrincipal actor,@RequestBody CommunityService.CommentWrite body){return service.editComment(id,actor.id(),body);}
    @DeleteMapping("/account/community/comments/{id}") public void removeComment(@PathVariable long id,@AuthenticationPrincipal AuthPrincipal actor,@RequestParam long revision){service.removeComment(id,actor.id(),revision);}
}
