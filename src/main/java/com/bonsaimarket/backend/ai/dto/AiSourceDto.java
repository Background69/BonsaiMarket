package com.bonsaimarket.backend.ai.dto;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record AiSourceDto(String type, Long id, String name, String url, String sourceCheckedOn) {
    public AiSourceDto(String type, Long id, String name) { this(type, id, name, null, null); }
}
