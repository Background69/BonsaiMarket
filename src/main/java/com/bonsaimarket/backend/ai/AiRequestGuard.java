package com.bonsaimarket.backend.ai;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.*;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

/** Single-instance demo limits; remoteAddr is the socket peer, never a client IP header. */
@Component
public class AiRequestGuard extends OncePerRequestFilter {
    static final int MAX_BODY_BYTES = 16384;
    private final Semaphore concurrent = new Semaphore(4);
    private final Map<String, Window> peers = new HashMap<>();
    private final Clock clock;
    private long globalStart;
    private int globalCount;
    private record Window(long start, int count) {}

    public AiRequestGuard() { this(Clock.systemUTC()); }
    AiRequestGuard(Clock clock) { this.clock = clock; this.globalStart = clock.millis(); }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod())
                || !(request.getContextPath() + "/api/ai/chat").equals(request.getRequestURI());
    }

    private synchronized boolean admit(String peer) {
        long now = clock.millis();
        peers.entrySet().removeIf(entry -> now - entry.getValue().start() >= 60000);
        if (now - globalStart >= 60000) { globalStart = now; globalCount = 0; }
        if (++globalCount > 60) return false;
        Window previous = peers.get(peer);
        if (previous == null && peers.size() >= 4096) return false;
        int count = previous == null ? 1 : previous.count() + 1;
        peers.put(peer, new Window(previous == null ? now : previous.start(), count));
        return count <= 10;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        if (!admit(request.getRemoteAddr()) || !concurrent.tryAcquire()) {
            response.setHeader("Retry-After", "60");
            error(response, 429, "RATE_LIMITED", "Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau ít phút.");
            return;
        }
        try {
            if (request.getContentLengthLong() > MAX_BODY_BYTES) {
                error(response, 413, "REQUEST_TOO_LARGE", "Yêu cầu vượt quá kích thước cho phép.");
                return;
            }
            byte[] body = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
            if (body.length > MAX_BODY_BYTES) {
                error(response, 413, "REQUEST_TOO_LARGE", "Yêu cầu vượt quá kích thước cho phép.");
                return;
            }
            chain.doFilter(new HttpServletRequestWrapper(request) {
                @Override public ServletInputStream getInputStream() {
                    var input = new ByteArrayInputStream(body);
                    return new ServletInputStream() {
                        @Override public int read() { return input.read(); }
                        @Override public int read(byte[] bytes, int offset, int length) { return input.read(bytes, offset, length); }
                        @Override public boolean isFinished() { return input.available() == 0; }
                        @Override public boolean isReady() { return true; }
                        @Override public void setReadListener(ReadListener listener) { throw new UnsupportedOperationException(); }
                    };
                }
                @Override public BufferedReader getReader() {
                    return new BufferedReader(new InputStreamReader(getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
                }
            }, response);
        } finally {
            concurrent.release();
        }
    }

    private static void error(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"code\":\"" + code + "\",\"error\":\"" + message + "\"}");
    }
}
