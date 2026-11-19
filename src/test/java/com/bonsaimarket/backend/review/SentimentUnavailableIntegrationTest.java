package com.bonsaimarket.backend.review;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** H2/MockMvc with the REAL HTTP adapter and a stalled/offline transport, no model prediction double. */
class SentimentUnavailableIntegrationTest extends SentimentTestSupport {
    static HttpServer stalled;
    static ExecutorService executor;
    static AtomicInteger calls=new AtomicInteger();

    @DynamicPropertySource static void endpoint(DynamicPropertyRegistry props) throws Exception {
        stalled=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        executor=Executors.newCachedThreadPool();stalled.setExecutor(executor);
        stalled.createContext("/predict",exchange->{
            calls.incrementAndGet();
            try {exchange.getRequestBody().readAllBytes();Thread.sleep(3000);exchange.sendResponseHeaders(503,-1);}
            catch(Exception ignored) {} finally {exchange.close();}
        });stalled.start();
        String endpoint="http://127.0.0.1:"+stalled.getAddress().getPort()+"/predict";
        props.add("sentiment.endpoint",()->endpoint);props.add("sentiment.timeout-ms",()->200);
        props.add("spring.datasource.url",()->"jdbc:h2:mem:sentiment-unavailable;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
    }

    @AfterAll void stopTransport() {stalled.stop(0);executor.shutdownNow();}

    @Test void reviewCommitsBeforeTimeoutAndOfflineInferenceFailures() throws Exception {
        long first=submit("Cây héo, tôi không hài lòng.");assertEquals(0,calls.get(),"No inference in review request");
        assertTrue(worker.processOnce());assertEquals(1,calls.get());
        assertEquals("PENDING",db.queryForObject("select status from review_sentiment where review_id=?",String.class,first));
        assertNull(db.queryForObject("select label from review_sentiment where review_id=?",String.class,first));
        stalled.stop(0);
        item=line();long second=submit("Chậu bị vỡ khi nhận hàng.");assertTrue(worker.processOnce());
        assertEquals("PENDING",db.queryForObject("select status from review_sentiment where review_id=?",String.class,second));
        assertEquals(2,db.queryForObject("select count(*) from reviews where product_id=?",Integer.class,product.getId()));
        mvc.perform(get("/api/products/"+product.getId()+"/reviews")).andExpect(status().isOk()).andExpect(jsonPath("$.ratingCount").value(2));
    }
}
