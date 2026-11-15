package com.bonsaimarket.backend.management;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SupportCaseController {
    private final SupportCaseService cases;

    public SupportCaseController(SupportCaseService cases) {
        this.cases = cases;
    }

    public record Open(@Min(1) long orderId, @NotBlank @Size(max = 1000) String reason) {
    }

    public record Response(@NotNull @Min(0) Long revision, @NotBlank @Size(max = 4000) String body,
                           @NotBlank @Size(max = 80) String requestKey) {
    }

    public record Decision(@NotNull @Min(0) Long revision, @NotBlank @Size(max = 20) String status,
                           @NotBlank @Size(max = 1000) String reason) {
    }

    @GetMapping("/account/disputes")
    public Object mine(@AuthenticationPrincipal AuthPrincipal actor, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "") String status) {
        return cases.list(actor.id(), false, page, status);
    }

    @GetMapping("/admin/disputes")
    public Object queue(@AuthenticationPrincipal AuthPrincipal actor, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "") String status) {
        return cases.list(actor.id(), true, page, status);
    }

    @PostMapping("/account/disputes")
    public Object open(@AuthenticationPrincipal AuthPrincipal actor, @Valid @RequestBody Open body) {
        return cases.open(actor.id(), body.orderId(), body.reason());
    }

    @GetMapping("/account/disputes/{id}")
    public Object detail(@AuthenticationPrincipal AuthPrincipal actor, @PathVariable long id) {
        return cases.detail(actor.id(), false, id);
    }

    @GetMapping("/admin/disputes/{id}")
    public Object adminDetail(@AuthenticationPrincipal AuthPrincipal actor, @PathVariable long id) {
        return cases.detail(actor.id(), true, id);
    }

    @PostMapping("/account/disputes/{id}/responses")
    public Object respond(@AuthenticationPrincipal AuthPrincipal actor, @PathVariable long id, @Valid @RequestBody Response body) {
        return cases.respond(actor.id(), false, id, body.revision(), body.body(), body.requestKey());
    }

    @PostMapping("/admin/disputes/{id}/responses")
    public Object adminRespond(@AuthenticationPrincipal AuthPrincipal actor, @PathVariable long id, @Valid @RequestBody Response body) {
        return cases.respond(actor.id(), true, id, body.revision(), body.body(), body.requestKey());
    }

    @PostMapping("/admin/disputes/{id}/decision")
    public Object decide(@AuthenticationPrincipal AuthPrincipal actor, @PathVariable long id, @Valid @RequestBody Decision body) {
        return cases.decide(actor.id(), id, body.revision(), body.status(), body.reason());
    }
}
