package com.bonsaimarket.backend.review;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.mock.env.MockEnvironment;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

class SentimentClientTest {
    HttpServer server;
    java.util.concurrent.ExecutorService pool;
    final String token="isolated-sentiment-test-token-32-chars";
    final AtomicReference<String> body=new AtomicReference<>();
    SentimentClient client(String response,int delay,boolean stallBody) throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        pool=Executors.newCachedThreadPool();server.setExecutor(pool);
        server.createContext("/predict",exchange->{
            body.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            assertEquals("Bearer "+token,exchange.getRequestHeaders().getFirst("Authorization"));
            try {
                if(!stallBody)Thread.sleep(delay);
                byte[] raw=response.getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,raw.length);
                if(stallBody)Thread.sleep(delay);
                exchange.getResponseBody().write(raw);
            } catch(Exception ignored) {} finally {exchange.close();}
        });server.start();
        return new SentimentClient(new SentimentSettings(true,true,"http://127.0.0.1:"+server.getAddress().getPort()+"/predict",token,200,new MockEnvironment()),new ObjectMapper());
    }
    String valid() {return "{\"label\":\"mixed\",\"modelVersion\":\"tfidf-logreg-"+"a".repeat(64)+"\",\"evaluationMode\":\"train_only\",\"demoOnly\":true}";}
    @AfterEach void stop() {if(server!=null)server.stop(0);if(pool!=null)pool.shutdownNow();}
    @Test void strictResponseAndOnlyReviewTextSent() throws Exception {
        var result=client(valid(),0,false).predict("Cây đẹp nhưng giao chậm");
        assertEquals("mixed",result.label());assertTrue(result.demoOnly());
        var sent=new ObjectMapper().readTree(body.get());assertEquals(1,sent.size());assertEquals("Cây đẹp nhưng giao chậm",sent.path("text").asText());
    }
    @Test void invalidLabelIsFailure() throws Exception {var c=client(valid().replace("mixed","unknown"),0,false);assertThrows(Exception.class,()->c.predict("QA"));}
    @Test void oversizedResponseIsFailure() throws Exception {var c=client("x".repeat(10000),0,false);assertThrows(Exception.class,()->c.predict("QA"));}
    @Test void malformedJsonIsFailure() throws Exception {var c=client("{bad-json",0,false);assertThrows(Exception.class,()->c.predict("QA"));}
    @Test void unavailableServiceIsFailure() throws Exception {var c=client(valid(),0,false);server.stop(0);assertThrows(Exception.class,()->c.predict("QA"));}
    @Test void timeoutBeforeHeadersIsBounded() throws Exception {var c=client(valid(),2000,false);long start=System.nanoTime();assertThrows(Exception.class,()->c.predict("QA"));assertTrue(System.nanoTime()-start<1_500_000_000L);}
    @Test void stalledResponseBodyIsAlsoBounded() throws Exception {var c=client(valid(),2000,true);long start=System.nanoTime();assertThrows(Exception.class,()->c.predict("QA"));assertTrue(System.nanoTime()-start<1_500_000_000L);}
    @Test void externalEndpointAndProductionDemoAreRejected() {
        assertThrows(IllegalArgumentException.class,()->new SentimentSettings(true,true,"http://example.com/predict",token,200,new MockEnvironment()));
        var production=new MockEnvironment();production.setActiveProfiles("prod");
        assertThrows(IllegalArgumentException.class,()->new SentimentSettings(true,true,"http://127.0.0.1:8091/predict",token,200,production));
    }
    @Test void disabledWorkerDoesNotTouchDatabaseOrModel() {
        var jdbc=org.mockito.Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class);
        var access=org.mockito.Mockito.mock(com.bonsaimarket.backend.marketplace.MarketplaceAccess.class);
        var model=org.mockito.Mockito.mock(SentimentClient.class);
        var manager=org.mockito.Mockito.mock(org.springframework.transaction.PlatformTransactionManager.class);
        var settings=new SentimentSettings(false,false,"http://127.0.0.1:8091/predict","",200,new MockEnvironment());
        var jobs=new ReviewSentiments(jdbc,access,settings,model,manager);
        jobs.enqueue(1,"Disabled");jobs.tick();assertFalse(jobs.processOnce());
        org.mockito.Mockito.verifyNoInteractions(jdbc,access,model,manager);
    }
}
