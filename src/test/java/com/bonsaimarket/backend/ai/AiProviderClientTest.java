package com.bonsaimarket.backend.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

class AiProviderClientTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = mock(HttpClient.class);
    // Placeholder is an inert test value, never a real credential; nothing prints it.
    private final AiProviderClient client = new AiProviderClient(new AiSettings(true, "test-placeholder", "gemini-fixture"), mapper, http);

    @Test void missingKeyModelOrDisabledIsSafe() {
        for (AiSettings config : new AiSettings[]{new AiSettings(true, "", "gemini-fixture"), new AiSettings(true, "placeholder", ""), new AiSettings(true, null, null), new AiSettings(false, "placeholder", "gemini-fixture")}) {
            var error = assertThrows(AiException.class, () -> new AiProviderClient(config, mapper, http).answer("Hỏi", "{}", false));
            assertEquals(503, error.status());
            assertEquals(config.enabled() ? "AI_NOT_CONFIGURED" : "AI_DISABLED", error.code());
            assertFalse(error.getMessage().contains("placeholder"));
            assertFalse(config.toString().contains("placeholder"));
        }
        verifyNoInteractions(http);
    }

    @ParameterizedTest @ValueSource(ints = {400, 401, 403, 404, 422, 429, 500, 502, 503, 504})
    void providerErrorsNeverEchoBodiesAndNeverRetry(int status) throws Exception {
        respond(status, "private provider body: test-placeholder");
        var error = assertThrows(AiException.class, () -> client.answer("Hỏi", "{}", true));
        assertEquals(status == 429 ? 429 : status >= 500 ? 502 : 503, error.status());
        assertFalse(error.getMessage().contains("private"));
        assertFalse(error.getMessage().contains("test-placeholder"));
        verify(http, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @ParameterizedTest @ValueSource(strings = {"", "not json", "{}", "null", "[]", "{\"candidates\":[]}", "{\"candidates\":null}", "{\"candidates\":{}}", "{\"candidates\":[{}]}", "{\"candidates\":[{\"finishReason\":\"MAX_TOKENS\",\"content\":{\"role\":\"model\",\"parts\":[{\"text\":\"partial\"}]}}]}"})
    void emptyMalformedOrIncompleteResponseFailsSafely(String body) {
        assertEquals(502, assertThrows(AiException.class, () -> client.parse(200, body)).status());
    }

    @Test void extractsTextPartsWithoutThoughtsOrAlternativeCandidates() {
        assertEquals("Một Hai", client.parse(200, """
                {"candidates":[
                  {"finishReason":"STOP","content":{"role":"user","parts":[{"text":"Không lấy"}]}},
                  {"finishReason":"STOP","content":{"role":"model","parts":[
                    {"thought":true,"text":"private reasoning"},{"text":"Một "},{"text":"Hai"}]}},
                  {"finishReason":"STOP","content":{"role":"model","parts":[{"text":"Không ghép"}]}}]}
                """));
    }

    @ParameterizedTest @ValueSource(strings = {"SAFETY", "RECITATION", "BLOCKLIST", "PROHIBITED_CONTENT", "SPII", "IMAGE_SAFETY", "MODEL_ARMOR"})
    void refusalIsSafe(String reason) {
        assertEquals("AI_REFUSED", assertThrows(AiException.class, () -> client.parse(200,
                "{\"candidates\":[{\"finishReason\":\"" + reason + "\",\"content\":{\"role\":\"model\",\"parts\":[{\"text\":\"private\"}]}}]}")).code());
    }

    @Test void timeoutAndNetworkErrorsDoNotRetry() throws Exception {
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(new HttpTimeoutException("private"));
        assertEquals(504, assertThrows(AiException.class, () -> client.answer("Hỏi", "{}", false)).status());
        verify(http, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        reset(http);
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(new IOException("private"));
        assertEquals("AI_NETWORK_ERROR", assertThrows(AiException.class, () -> client.answer("Hỏi", "{}", false)).code());
        verify(http, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test void sendsGeminiSchemaWithSeparatedInstructionsAndLimits() throws Exception {
        String selection = "{\"productIds\":[1,999],\"storeIds\":[7],\"faqIds\":[1]}";
        respond(200, envelope(selection));
        assertEquals(selection, client.answer("Hỏi", "{\"catalogMode\":true}", true));
        var captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(captor.capture(), any(HttpResponse.BodyHandler.class));
        var request = captor.getValue();
        assertEquals("https://generativelanguage.googleapis.com/v1beta/models/gemini-fixture:generateContent", request.uri().toString());
        assertNull(request.uri().getQuery());
        assertFalse(request.uri().toString().contains("test-placeholder"));
        assertTrue(request.headers().firstValue("x-goog-api-key").orElseThrow().equals("test-placeholder"));
        assertTrue(request.headers().firstValue("Authorization").isEmpty());
        assertEquals("POST", request.method());
        assertEquals("application/json", request.headers().firstValue("Content-Type").orElseThrow());
        assertEquals(25, request.timeout().orElseThrow().toSeconds());
        var body = requestBody(request);
        assertEquals(AiProviderClient.INSTRUCTIONS, body.path("systemInstruction").path("parts").get(0).path("text").asText());
        for (String instruction : java.util.List.of("Trợ lý AI BonsaiMarket", "tiếng Việt", "không tin cậy", "fine-tune"))
            assertTrue(AiProviderClient.INSTRUCTIONS.contains(instruction));
        var user = body.path("contents").get(0);
        assertEquals("user", user.path("role").asText());
        assertEquals(2, user.path("parts").size());
        assertTrue(user.path("parts").get(0).path("text").asText().contains("catalogMode"));
        assertEquals("Hỏi", user.path("parts").get(1).path("text").asText());
        var generation = body.path("generationConfig");
        assertEquals(1200, generation.path("maxOutputTokens").asInt());
        assertEquals(1, generation.path("candidateCount").asInt());
        var format = generation.path("responseFormat").path("text");
        assertEquals("APPLICATION_JSON", format.path("mimeType").asText());
        var schema = format.path("schema");
        assertEquals("object", schema.path("type").asText());
        assertFalse(schema.path("additionalProperties").asBoolean());
        assertEquals(3, schema.path("required").size());
        for (String field : java.util.List.of("productIds", "storeIds", "faqIds")) {
            var property = schema.path("properties").path(field);
            assertEquals("array", property.path("type").asText());
            assertEquals("integer", property.path("items").path("type").asText());
            assertEquals(1, property.path("items").path("minimum").asInt());
            assertEquals(field.equals("faqIds") ? 4 : 10, property.path("maxItems").asInt());
        }
        for (String forbidden : java.util.List.of("tools", "model", "instructions", "input", "text", "store", "max_output_tokens"))
            assertTrue(body.path(forbidden).isMissingNode());
    }

    private tools.jackson.databind.JsonNode requestBody(HttpRequest request) {
        var bytes = new java.io.ByteArrayOutputStream();
        request.bodyPublisher().orElseThrow().subscribe(new java.util.concurrent.Flow.Subscriber<java.nio.ByteBuffer>() {
            public void onSubscribe(java.util.concurrent.Flow.Subscription s) { s.request(Long.MAX_VALUE); }
            public void onNext(java.nio.ByteBuffer buffer) { byte[] b = new byte[buffer.remaining()]; buffer.get(b); bytes.writeBytes(b); }
            public void onError(Throwable t) { throw new AssertionError("Request publisher failed"); }
            public void onComplete() {}
        });
        return mapper.readTree(bytes.toByteArray());
    }

    @Test void contextTooLargeCannotSend() {
        assertEquals("CONTEXT_TOO_LARGE", assertThrows(AiException.class, () -> client.answer("Hỏi", "x".repeat(16001), false)).code());
        verifyNoInteractions(http);
    }

    @ParameterizedTest @ValueSource(strings = {"https://example.test", "gemini-test?key=private", "gemini-../other",
            "gemini-test#private", "models/gemini-test", "other-model", " gemini-test", "gemini-test\n"})
    void invalidModelCannotChangeHostPathOrLeakConfiguration(String model) {
        var configured = new AiProviderClient(new AiSettings(true, "test-placeholder", model), mapper, http);
        var error = assertThrows(AiException.class, () -> configured.answer("Hỏi", "{}", false));
        assertEquals("AI_PROVIDER_CONFIG", error.code());
        assertEquals(503, error.status());
        assertFalse(error.getMessage().contains(model));
        verifyNoInteractions(http);
    }

    @Test void promptBlockAndSafetyRatingsAreSafeEvenWithoutCandidates() {
        for (String body : java.util.List.of("{\"promptFeedback\":{\"blockReason\":\"SAFETY\",\"blockReasonMessage\":\"private\"}}",
                "{\"promptFeedback\":{\"safetyRatings\":[{\"blocked\":true}]}}",
                "{\"candidates\":[{\"finishReason\":\"STOP\",\"safetyRatings\":[{\"blocked\":true}]}]}")) {
            var error = assertThrows(AiException.class, () -> client.parse(200, body));
            assertEquals(422, error.status());
            assertFalse(error.getMessage().contains("private"));
        }
    }

    @Test void interruptionRestoresFlagAndDoesNotRetry() throws Exception {
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(new InterruptedException("private"));
        try {
            assertEquals("AI_UNAVAILABLE", assertThrows(AiException.class, () -> client.answer("Hỏi", "{}", false)).code());
            assertTrue(Thread.currentThread().isInterrupted());
            verify(http, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        } finally { Thread.interrupted(); }
    }

    @Test void careUsesTextGenerationWithoutCatalogSchema() throws Exception {
        respond(200, envelope("Kiểm tra độ ẩm trước khi tưới."));
        assertEquals("Kiểm tra độ ẩm trước khi tưới.", client.answer("Cách tưới bonsai", "{\"faq\":[]}", false));
        var captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(captor.capture(), any(HttpResponse.BodyHandler.class));
        assertTrue(requestBody(captor.getValue()).path("generationConfig").path("responseFormat").isMissingNode());
    }

    @Test void structuredTextPartsPreserveJsonWithoutInsertedSeparators() {
        assertEquals("{\"productIds\":[]}", client.parse(200, """
                {"candidates":[{"finishReason":"STOP","content":{"role":"model","parts":[
                  {"text":"{\\\"productIds\\\":"},{"text":"[]}"}]}}]}
                """));
    }

    @ParameterizedTest @ValueSource(strings = {"not JSON; hàng giả giá 1 đồng", "null", "[]", "{}",
            "{\"productIds\":[\"1\"],\"storeIds\":[],\"faqIds\":[]}",
            "{\"productIds\":[0],\"storeIds\":[],\"faqIds\":[]}",
            "{\"productIds\":[],\"storeIds\":[],\"faqIds\":[],\"answer\":\"giá giả\"}",
            "{\"productIds\":[],\"storeIds\":[],\"faqIds\":[]} prose",
            "{\"productIds\":[],\"storeIds\":[],\"faqIds\":[1,2,3,4,5]}"})
    void malformedCatalogJsonCannotBecomeProse(String output) throws Exception {
        respond(200, envelope(output));
        assertEquals("AI_INVALID_RESPONSE", assertThrows(AiException.class, () -> client.answer("Mua cây", "{}", true)).code());
        verify(http, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test void outputAndEnvelopeLimitsApply() {
        assertEquals(502, assertThrows(AiException.class, () -> client.parse(200, envelope("x".repeat(12001)))).status());
        assertEquals(502, assertThrows(AiException.class, () -> client.parse(200, "x".repeat(131073))).status());
        assertEquals(502, assertThrows(AiException.class, () -> client.parse(200, envelope("text") + " trailing")).status());
    }

    private String envelope(String text) {
        return mapper.writeValueAsString(java.util.Map.of("candidates", java.util.List.of(java.util.Map.of("finishReason", "STOP",
                "content", java.util.Map.of("role", "model", "parts", java.util.List.of(java.util.Map.of("text", text)))))));
    }

    private void respond(int status, String body) throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    }
}
