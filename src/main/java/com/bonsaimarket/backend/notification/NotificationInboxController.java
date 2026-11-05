package com.bonsaimarket.backend.notification;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/{scope:admin|account}/notifications")
public class NotificationInboxController {
    private final NotificationInbox inbox;

    public NotificationInboxController(NotificationInbox inbox) {
        this.inbox = inbox;
    }

    @GetMapping
    public Object list(@AuthenticationPrincipal AuthPrincipal actor, @PathVariable String scope, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "") String unread) {
        return inbox.list(actor.id(), scope.equals("admin"), page, unread);
    }

    @GetMapping("/count")
    public Object count(@AuthenticationPrincipal AuthPrincipal actor, @PathVariable String scope) {
        return inbox.count(actor.id(), scope.equals("admin"));
    }

    @PostMapping("/{id}/read")
    public Object read(@AuthenticationPrincipal AuthPrincipal actor, @PathVariable String scope, @PathVariable String id) {
        return inbox.read(actor.id(), scope.equals("admin"), id);
    }
}
