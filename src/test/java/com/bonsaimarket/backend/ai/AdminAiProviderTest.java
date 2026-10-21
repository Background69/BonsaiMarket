package com.bonsaimarket.backend.ai;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Actual shared Gemini transport with an inert HTTP double, never a provider call. */
class AdminAiProviderTest {
    final ObjectMapper mapper=new ObjectMapper();
    final HttpClient http=mock(HttpClient.class);
    final AiProviderClient client=new AiProviderClient(new AiSettings(true,"fixture-placeholder","gemini-fixture"),mapper,http);
    HttpResponse<String> response(String text,int status) {
        var response=(HttpResponse<String>)mock(HttpResponse.class);when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(mapper.writeValueAsString(Map.of("candidates",List.of(Map.of("finishReason","STOP","content",Map.of("role","model","parts",List.of(Map.of("text",text))))))));return response;
    }
    @Test void adminUsesSameTransportWithSeparatePromptAndSelectionSchema() throws Exception {
        var success=response("{\"findingIds\":[]}",200);
        when(http.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(success);
        assertEquals("{\"findingIds\":[]}",client.selectAdminFindings("OVERVIEW","{}","ADMIN INSTRUCTIONS"));
        var capture=ArgumentCaptor.forClass(HttpRequest.class);verify(http).send(capture.capture(),any(HttpResponse.BodyHandler.class));
        var bytes=new java.io.ByteArrayOutputStream();var done=new CompletableFuture<Void>();
        capture.getValue().bodyPublisher().orElseThrow().subscribe(new java.util.concurrent.Flow.Subscriber<java.nio.ByteBuffer>() {
            public void onSubscribe(java.util.concurrent.Flow.Subscription s) {s.request(Long.MAX_VALUE);}
            public void onNext(java.nio.ByteBuffer buffer) {byte[] b=new byte[buffer.remaining()];buffer.get(b);bytes.writeBytes(b);}
            public void onError(Throwable t) {done.completeExceptionally(t);}public void onComplete() {done.complete(null);}
        });done.get(2,TimeUnit.SECONDS);
        var body=mapper.readTree(bytes.toString(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals("ADMIN INSTRUCTIONS",body.path("systemInstruction").path("parts").get(0).path("text").asText());
        assertTrue(body.path("generationConfig").path("responseFormat").path("text").path("schema").path("properties").has("findingIds"));
        assertEquals(1200,body.path("generationConfig").path("maxOutputTokens").asInt());assertEquals(25,capture.getValue().timeout().orElseThrow().toSeconds());
    }
    @Test void adminTimeoutQuotaAndMissingConfigUseExistingSafeErrorsWithoutRetry() throws Exception {
        when(http.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenThrow(new HttpTimeoutException("private"));
        assertEquals("AI_TIMEOUT",assertThrows(AiException.class,()->client.selectAdminFindings("OVERVIEW","{}","admin")).code());
        reset(http);var quota=response("private",429);when(http.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(quota);
        assertEquals("AI_PROVIDER_RATE_LIMIT",assertThrows(AiException.class,()->client.selectAdminFindings("OVERVIEW","{}","admin")).code());verify(http,times(1)).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
        reset(http);var missing=new AiProviderClient(new AiSettings(true,"fixture-placeholder",""),mapper,http);
        assertEquals("AI_NOT_CONFIGURED",assertThrows(AiException.class,()->missing.selectAdminFindings("OVERVIEW","{}","admin")).code());verifyNoInteractions(http);
        assertEquals("CONTEXT_TOO_LARGE",assertThrows(AiException.class,()->client.selectAdminFindings("OVERVIEW","x".repeat(16001),"admin")).code());
    }
    @Test void publicAndAdminShareGlobalFourProviderSlots() throws Exception {
        var entered=new CountDownLatch(4);var release=new CountDownLatch(1);
        when(http.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenAnswer(inv->{entered.countDown();assertTrue(release.await(5,TimeUnit.SECONDS));return response("{\"findingIds\":[]}",200);});
        try(var executor=Executors.newFixedThreadPool(4)) {
            var tasks=new ArrayList<Future<String>>();
            try {
                for(int i=0;i<4;i++) {boolean admin=i%2==0;tasks.add(executor.submit(()->admin?client.selectAdminFindings("OVERVIEW","{}","admin"):client.answer("public","{}",false)));}
                assertTrue(entered.await(3,TimeUnit.SECONDS));
                assertEquals("AI_BUSY",assertThrows(AiException.class,()->client.selectAdminFindings("OVERVIEW","{}","admin")).code());
                assertEquals("AI_BUSY",assertThrows(AiException.class,()->client.answer("public","{}",false)).code());
            } finally {release.countDown();}
            for(var future:tasks)assertNotNull(future.get(3,TimeUnit.SECONDS));
        }
        verify(http,times(4)).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
    }
}
