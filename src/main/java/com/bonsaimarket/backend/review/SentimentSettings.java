package com.bonsaimarket.backend.review;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class SentimentSettings {
    final boolean enabled;
    final boolean allowDemo;
    final URI endpoint;
    final String token;
    final Duration timeout;

    public SentimentSettings(@Value("${sentiment.enabled:false}") boolean enabled,
            @Value("${sentiment.allow-demo:false}") boolean allowDemo,
            @Value("${sentiment.endpoint:http://127.0.0.1:8091/predict}") String endpoint,
            @Value("${sentiment.internal-token:}") String token,
            @Value("${sentiment.timeout-ms:2000}") long timeoutMs, Environment environment) {
        this.enabled = enabled;
        this.allowDemo = allowDemo;
        this.endpoint = URI.create(endpoint);
        this.token = token;
        this.timeout = Duration.ofMillis(Math.clamp(timeoutMs, 100, 10000));
        if (enabled) {
            if (!"http".equals(this.endpoint.getScheme()) || !"127.0.0.1".equals(this.endpoint.getHost())
                    || !"/predict".equals(this.endpoint.getPath()) || this.endpoint.getUserInfo() != null
                    || this.endpoint.getQuery() != null || this.endpoint.getFragment() != null)
                throw new IllegalArgumentException("Sentiment endpoint must be loopback /predict");
            if (token.length() < 32) throw new IllegalArgumentException("Sentiment internal token requires 32 characters");
            if (allowDemo && Arrays.stream(environment.getActiveProfiles()).anyMatch(p -> p.equals("prod") || p.equals("production")))
                throw new IllegalArgumentException("Demo sentiment is forbidden in production profiles");
        }
    }
}
