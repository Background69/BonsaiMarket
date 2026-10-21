package com.bonsaimarket.backend.adminai;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/admin/ai")
public class AdminAiController {
    private final AdminAiService service;
    public AdminAiController(AdminAiService service) {this.service=service;}
    @GetMapping("/capabilities")
    public ResponseEntity<AdminAiDtos.Capabilities> capabilities(@AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok().header("Cache-Control","no-store").body(service.capabilities(principal));
    }
    @PostMapping("/chat")
    public ResponseEntity<AdminAiDtos.Response> chat(@AuthenticationPrincipal AuthPrincipal principal,@RequestBody JsonNode body) {
        return ResponseEntity.ok().header("Cache-Control","no-store").body(service.chat(principal,body));
    }
}
