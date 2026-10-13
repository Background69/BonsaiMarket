package com.bonsaimarket.backend.auth;

import com.bonsaimarket.backend.user.User;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import java.io.Serializable;
import java.time.Clock;
import java.util.*;

@Component
public class AuthFlowState {
    static final String VERIFICATION = "AUTH_PENDING_VERIFICATION", RESET = "AUTH_PASSWORD_RESET";
    static final long CONTEXT_SECONDS = 1800, PERMIT_SECONDS = 300;
    public record Verification(String contextId, Long userId, String email, long version, long expiresAt) implements Serializable {}
    public record ResetPermit(Long userId, long version, String nonce, long expiresAt) implements Serializable {}
    public record ResetFlow(String flowId, String email, long expiresAt, long resendAt, ResetPermit permit) implements Serializable {}
    private final Clock clock;
    public AuthFlowState(Clock clock) {this.clock = clock;}

    public void beginVerification(User user, HttpServletRequest request) {
        HttpSession session = request.getSession();
        request.changeSessionId();
        session.setAttribute(VERIFICATION, new Verification(UUID.randomUUID().toString(), user.getId(), user.getEmail(), user.getAuthVersion(), clock.millis() + CONTEXT_SECONDS * 1000));
    }
    public Verification verification(HttpSession session) {
        if (!(session.getAttribute(VERIFICATION) instanceof Verification context) || context.expiresAt() <= clock.millis()) {
            session.removeAttribute(VERIFICATION); throw verificationExpired();
        }
        return context;
    }
    public Verification verification(HttpSession session,String contextId) {
        Verification context=verification(session);
        if(!Objects.equals(context.contextId(),contextId)) throw verificationExpired();
        return context;
    }
    public void beginReset(String email, String flowId, HttpSession session) {
        session.setAttribute(RESET, new ResetFlow(flowId==null?UUID.randomUUID().toString():flowId, AuthService.emailKey(email), clock.millis() + CONTEXT_SECONDS * 1000,
            clock.millis() + AuthService.RESEND_SECONDS * 1000, null));
    }
    public ResetFlow reset(HttpSession session) {
        if (!(session.getAttribute(RESET) instanceof ResetFlow flow) || flow.expiresAt() <= clock.millis()) {
            session.removeAttribute(RESET); throw resetExpired();
        }
        return flow;
    }
    public void authorize(ResetFlow flow, ResetPermit permit, HttpSession session) {
        session.setAttribute(RESET, new ResetFlow(flow.flowId(), flow.email(), flow.expiresAt(), flow.resendAt(), permit));
    }
    public ResetFlow reset(HttpSession session,String flowId) {
        ResetFlow flow=reset(session);
        if(!Objects.equals(flow.flowId(),flowId)) throw resetExpired();
        return flow;
    }
    public ResetPermit permit(HttpSession session) {
        ResetPermit permit = reset(session).permit();
        if (permit == null || permit.expiresAt() <= clock.millis()) {
            session.removeAttribute(RESET); throw permitExpired();
        }
        return permit;
    }
    public int remaining(long until) {return (int)Math.max(0, Math.ceil((until - clock.millis()) / 1000.0));}
    public static HttpSession requireSession(HttpServletRequest request, boolean verification) {
        HttpSession session = request.getSession(false);
        if (session == null) throw verification ? verificationExpired() : resetExpired();
        return session;
    }
    public static void clear(HttpSession session) {session.removeAttribute(VERIFICATION); session.removeAttribute(RESET);}
    public static AuthException verificationExpired() {return new AuthException(409, "VERIFICATION_CONTEXT_EXPIRED", "Phiên xác thực đã hết hạn. Hãy đăng nhập hoặc đăng ký để tiếp tục.");}
    public static AuthException resetExpired() {return new AuthException(409, "RESET_FLOW_EXPIRED", "Phiên khôi phục đã hết hạn. Vui lòng bắt đầu lại với email của bạn.");}
    public static AuthException permitExpired() {return new AuthException(409, "RESET_AUTHORIZATION_EXPIRED", "Quyền đặt lại mật khẩu đã hết hạn hoặc đã được sử dụng. Vui lòng bắt đầu lại.");}
}
