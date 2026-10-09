package com.bonsaimarket.backend.ai.dto;

import java.util.List;

public record AiChatResponse(String answer, List<AiSourceDto> sources) {}
