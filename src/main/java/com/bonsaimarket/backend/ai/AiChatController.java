package com.bonsaimarket.backend.ai;

import com.bonsaimarket.backend.ai.dto.AiChatRequest;
import com.bonsaimarket.backend.ai.dto.AiChatResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {
    private final AiChatService service;

    public AiChatController(AiChatService service) { this.service = service; }

    @PostMapping(value = "/chat", consumes = "application/json", produces = "application/json")
    public AiChatResponse chat(@Valid @RequestBody AiChatRequest request) {
        return service.chat(request.message());
    }
}
