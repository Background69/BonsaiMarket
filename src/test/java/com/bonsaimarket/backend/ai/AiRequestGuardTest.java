package com.bonsaimarket.backend.ai;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import java.time.*;
import java.util.ArrayList;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class AiRequestGuardTest {
    private MockHttpServletRequest request(String peer) {
        var r = new MockHttpServletRequest("POST", "/api/ai/chat");
        r.setRemoteAddr(peer); r.setContent("{}".getBytes()); return r;
    }

    @Test void fifthConcurrentRequestGets429AndPermitsAreReleased() throws Exception {
        var guard = new AiRequestGuard();
        var entered = new CountDownLatch(4);
        var release = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(4);
        var futures = new ArrayList<Future<?>>();
        try {
            for (int i = 0; i < 4; i++) {
                final int peer = i;
                futures.add(executor.submit(() -> {
                    try {
                        guard.doFilter(request("peer" + peer), new MockHttpServletResponse(), (a, b) -> {
                            entered.countDown();
                            try { if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("Guard test timed out"); }
                            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
                        });
                    } catch (Exception e) { throw new AssertionError(e); }
                }));
            }
            assertTrue(entered.await(3, TimeUnit.SECONDS));
            var blocked = new MockHttpServletResponse();
            guard.doFilter(request("fifth-peer"), blocked, (a, b) -> fail("Concurrency limit bypassed"));
            assertEquals(429, blocked.getStatus());
            release.countDown();
            for (var future : futures) future.get(3, TimeUnit.SECONDS);
            var success = new MockHttpServletResponse();
            guard.doFilter(request("next-peer"), success, (a, b) -> ((MockHttpServletResponse)b).setStatus(200));
            assertEquals(200, success.getStatus());
        } finally { release.countDown(); executor.shutdownNow(); }
    }

    @Test void globalLimitBlocksRotatingPeersAndWindowExpires() throws Exception {
        class TestClock extends Clock {
            long now;
            public ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(ZoneId zone) { return this; }
            public Instant instant() { return Instant.ofEpochMilli(now); }
        }
        var clock = new TestClock(); var guard = new AiRequestGuard(clock);
        for (int i = 0; i < 60; i++) {
            var response = new MockHttpServletResponse();
            guard.doFilter(request("peer" + i), response, (a,b) -> {});
            assertEquals(200, response.getStatus());
        }
        var blocked = new MockHttpServletResponse();
        guard.doFilter(request("rotating-peer"), blocked, (a,b) -> fail());
        assertEquals(429, blocked.getStatus());
        clock.now = 60001;
        var recovered = new MockHttpServletResponse();
        guard.doFilter(request("peer0"), recovered, (a,b) -> {});
        assertEquals(200, recovered.getStatus());
    }
}
