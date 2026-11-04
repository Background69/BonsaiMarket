package com.bonsaimarket.backend.messaging;
import com.bonsaimarket.backend.auth.AuthPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/account/conversations")
public class PrivateMessageController {
    private final PrivateMessaging service;
    public PrivateMessageController(PrivateMessaging service){this.service=service;}
    @GetMapping public Object list(@AuthenticationPrincipal AuthPrincipal actor,@RequestParam(defaultValue="0") int page){return service.list(actor.id(),page);}
    @GetMapping("/unread") public Object unread(@AuthenticationPrincipal AuthPrincipal actor){return java.util.Map.of("count",service.unread(actor.id()));}
    @PostMapping public Object start(@AuthenticationPrincipal AuthPrincipal actor,@RequestBody PrivateMessaging.Start input){return service.start(actor.id(),input);}
    @GetMapping("/{id}") public Object detail(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable long id){return service.detail(actor.id(),id);}
    @GetMapping("/{id}/messages") public Object messages(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable long id,@RequestParam(required=false) Long after,@RequestParam(required=false) Long before){return service.messages(actor.id(),id,after,before);}
    @PostMapping("/{id}/messages") public Object send(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable long id,@RequestBody PrivateMessaging.Send input){return service.send(actor.id(),id,input);}
    @PostMapping("/{id}/read") public void read(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable long id,@RequestBody PrivateMessaging.Read input){service.markRead(actor.id(),id,input);}
}
