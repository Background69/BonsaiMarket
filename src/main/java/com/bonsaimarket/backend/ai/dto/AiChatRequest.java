package com.bonsaimarket.backend.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiChatRequest(@NotBlank @Size(max = 2000) String message) {
    public AiChatRequest {
        if (message != null) message = message.strip();
    }
}
