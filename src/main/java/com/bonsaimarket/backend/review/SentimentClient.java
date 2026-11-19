package com.bonsaimarket.backend.review;

import java.net.http.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.Flow;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class SentimentClient {
    static final Set<String> LABELS = Set.of("positive", "neutral", "negative", "mixed");
    public record Prediction(String label, String modelVersion, String evaluationMode, boolean demoOnly) {}
    private final SentimentSettings settings;
    private final ObjectMapper mapper;
    private final HttpClient http;

    public SentimentClient(SentimentSettings settings, ObjectMapper mapper) {
        this.settings = settings;
        this.mapper = mapper;
        http = HttpClient.newBuilder().connectTimeout(settings.timeout).followRedirects(HttpClient.Redirect.NEVER).build();
    }

    @jakarta.annotation.PreDestroy
    public void close() { http.close(); }

    public Prediction predict(String text) throws Exception {
        var request = HttpRequest.newBuilder(settings.endpoint).timeout(settings.timeout)
            .header("Content-Type", "application/json").header("Authorization", "Bearer " + settings.token)
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(Map.of("text", text)))).build();
        // Bound response while streaming; never accept unbounded/malformed inference output.
        var future = http.sendAsync(request, boundedBody());
        try {
            var response = future.get(settings.timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (response.statusCode() != 200) throw new IllegalStateException("INFERENCE_UNAVAILABLE");
            var node = mapper.reader().with(tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(response.body());
            if (!node.path("label").isString() || !LABELS.contains(node.path("label").asText())
                    || !node.path("modelVersion").asText().matches("tfidf-logreg-[a-f0-9]{64}")
                    || !node.path("evaluationMode").isString() || !node.path("demoOnly").isBoolean())
                throw new IllegalStateException("INVALID_RESPONSE");
            String mode = node.path("evaluationMode").asText();
            if (!Set.of("train_only", "independent_test", "holdout").contains(mode)) throw new IllegalStateException("INVALID_RESPONSE");
            boolean demo = node.path("demoOnly").asBoolean() || mode.equals("train_only");
            if (demo && !settings.allowDemo) throw new IllegalStateException("DEMO_NOT_ALLOWED");
            return new Prediction(node.path("label").asText(), node.path("modelVersion").asText(), mode, demo);
        } finally {
            if (!future.isDone()) future.cancel(true);
        }
    }

    private static HttpResponse.BodyHandler<byte[]> boundedBody() {
        return info -> new HttpResponse.BodySubscriber<>() {
            private final HttpResponse.BodySubscriber<byte[]> delegate = HttpResponse.BodySubscribers.ofByteArray();
            private Flow.Subscription subscription;
            private long bytes;
            private boolean ended;
            public CompletionStage<byte[]> getBody() { return delegate.getBody(); }
            public void onSubscribe(Flow.Subscription s) { subscription=s; delegate.onSubscribe(s); }
            public void onNext(List<ByteBuffer> items) {
                if (ended) return;
                for (var item : items) bytes += item.remaining();
                if (bytes > 4096) { ended=true; subscription.cancel(); delegate.onError(new java.io.IOException("INVALID_RESPONSE")); }
                else delegate.onNext(items);
            }
            public void onError(Throwable error) { if (!ended) { ended=true; delegate.onError(error); } }
            public void onComplete() { if (!ended) { ended=true; delegate.onComplete(); } }
        };
    }
}
