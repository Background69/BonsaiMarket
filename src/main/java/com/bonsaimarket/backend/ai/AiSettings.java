package com.bonsaimarket.backend.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai")
public record AiSettings(boolean enabled, String apiKey, String model) {
    public boolean ready() {
        return enabled && apiKey != null && !apiKey.isBlank() && model != null && !model.isBlank();
    }

    @Override
    public String toString() {
        return "AiSettings[enabled=" + enabled + ", ready=" + ready() + "]";
    }
}
