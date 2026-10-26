package com.bonsaimarket.backend.auth;

import com.bonsaimarket.backend.user.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.http.HttpStatus;
import java.time.Clock;
import java.util.*;
import org.springframework.web.util.WebUtils;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final AuthSession sessions;
    private final AuthRateLimiter limits;
    private final OAuthRegistrations providers;
    private final OAuthAccounts accounts;
    private final Clock clock;
    private final PasswordResetRequests resetRequests;
    private final AuthFlowState flows;
    public AuthController(AuthService auth,AuthSession sessions,AuthRateLimiter limits,OAuthRegistrations providers,OAuthAccounts accounts,Clock clock,PasswordResetRequests resetRequests,AuthFlowState flows) {
        this.auth=auth;this.sessions=sessions;this.limits=limits;this.providers=providers;this.accounts=accounts;this.clock=clock;
        this.resetRequests=resetRequests;
        this.flows=flows;
    }
    @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token) {
        return Map.of("token",token.getToken(),"headerName",token.getHeaderName());
    }
    @GetMapping("/providers") public Map<String,Boolean> providers() {return Map.of("google",providers.configured("google"),"facebook",providers.configured("facebook"));}
    private void limit(String operation,String email,HttpServletRequest request,int max,int seconds) {
        limits.check(operation,AuthService.emailKey(email),request.getRemoteAddr(),max,max*5,seconds);
    }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public AuthDtos.Message register(@Valid @RequestBody AuthDtos.Register body,HttpServletRequest request) {
        limit("mail",body.email(),request,5,3600);
        try {flows.beginVerification(auth.register(body),request);}
        catch(AuthService.PendingDeliveryFailure e) {flows.beginVerification(e.pending,request); throw e;}
        return new AuthDtos.Message("Mã xác thực đã được gửi. Vui lòng kiểm tra email.",AuthService.RESEND_SECONDS);
    }
    @GetMapping("/verification-context") public AuthDtos.VerificationContext verificationContext(HttpServletRequest request) {
        var session=AuthFlowState.requireSession(request,true);
        synchronized(WebUtils.getSessionMutex(session)) {return auth.verificationContext(flows.verification(session));}
    }
    @PostMapping("/verify-email") public AuthDtos.UserView verify(@Valid @RequestBody AuthDtos.ContextCode body,HttpServletRequest request,HttpServletResponse response) {
        var session=AuthFlowState.requireSession(request,true);
        synchronized(WebUtils.getSessionMutex(session)) {
            var context=flows.verification(session,body.contextId()); limit("code",context.email(),request,20,600);
            User user=auth.verifyPending(context,body.code()); sessions.login(user,request,response); return AuthDtos.UserView.of(user);
        }
    }
    @PostMapping("/resend-verification") public AuthDtos.Message resend(@Valid @RequestBody AuthDtos.ContextRequest body,HttpServletRequest request) {
        var session=AuthFlowState.requireSession(request,true);
        synchronized(WebUtils.getSessionMutex(session)) {
            var context=flows.verification(session,body.contextId()); limit("mail",context.email(),request,5,3600); auth.resendPending(context);
            return new AuthDtos.Message("Mã mới đã được gửi đến email của bạn.",AuthService.RESEND_SECONDS);
        }
    }
    @PostMapping("/login") public AuthDtos.UserView login(@Valid @RequestBody AuthDtos.Login body,HttpServletRequest request,HttpServletResponse response) {
        limit("login",body.email(),request,10,900); User user;
        try {user=auth.authenticate(body.email(),body.password());}
        catch(AuthException e) {
            // Abandoned registration recovery requires the correct account password, never an email alone.
            if("EMAIL_NOT_VERIFIED".equals(e.code)) flows.beginVerification(auth.pendingAfterPasswordProof(body.email(),body.password()),request);
            throw e;
        }
        sessions.login(user,request,response); return AuthDtos.UserView.of(user);
    }
    @PostMapping("/logout") public AuthDtos.Message logout(HttpServletRequest request,HttpServletResponse response) {
        sessions.clear(request,response); return new AuthDtos.Message("Bạn đã đăng xuất.",0);
    }
    @GetMapping("/me") public AuthDtos.UserView me(@AuthenticationPrincipal AuthPrincipal principal) {
        if(principal==null) throw new AuthException(401,"SESSION_EXPIRED","Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
        return AuthDtos.UserView.of(auth.current(principal));
    }
    @PostMapping("/change-password") public AuthDtos.Message changePassword(@AuthenticationPrincipal AuthPrincipal principal,
        @Valid @RequestBody AuthDtos.ChangePassword body,HttpServletRequest request,HttpServletResponse response) {
        if(principal==null) throw new AuthException(401,"SESSION_EXPIRED","Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
        User current=auth.current(principal); limit("change-password",current.getEmail(),request,10,900);
        User user=auth.changePassword(principal,body);
        // The old session id/principal carry the previous auth_version; re-issue them so this device stays signed in.
        sessions.login(user,request,response);
        return new AuthDtos.Message("Đổi mật khẩu thành công. Các thiết bị khác đã được đăng xuất.",0);
    }
    @PostMapping("/forgot-password") @ResponseStatus(HttpStatus.ACCEPTED)
    public AuthDtos.Message forgot(@Valid @RequestBody AuthDtos.EmailRequest body,HttpServletRequest request) {
        var session=request.getSession();
        synchronized(WebUtils.getSessionMutex(session)) {
            limit("reset-mail",body.email(),request,5,3600); resetRequests.request(body.email()); flows.beginReset(body.email(),body.flowId(),session);
            return resetAccepted();
        }
    }
    private AuthDtos.Message resetAccepted() {return new AuthDtos.Message("Nếu tài khoản đủ điều kiện, bạn sẽ nhận được mã đặt lại mật khẩu qua email.",AuthService.RESEND_SECONDS);}
    @PostMapping("/resend-reset-code") @ResponseStatus(HttpStatus.ACCEPTED)
    public AuthDtos.Message resendReset(@Valid @RequestBody AuthDtos.FlowRequest body,HttpServletRequest request) {
        var session=AuthFlowState.requireSession(request,false);
        synchronized(WebUtils.getSessionMutex(session)) {
            var flow=flows.reset(session,body.flowId()); limit("reset-mail",flow.email(),request,5,3600);
            int remaining=flows.remaining(flow.resendAt());
            if(remaining>0) throw new AuthException(429,"RESEND_COOLDOWN","Vui lòng chờ trước khi gửi lại mã.",remaining);
            resetRequests.request(flow.email()); flows.beginReset(flow.email(),flow.flowId(),session); return resetAccepted();
        }
    }
    @PostMapping("/verify-reset-code") public AuthDtos.Message verifyReset(@Valid @RequestBody AuthDtos.Otp body,HttpServletRequest request) {
        var session=AuthFlowState.requireSession(request,false);
        synchronized(WebUtils.getSessionMutex(session)) {
            var flow=flows.reset(session,body.flowId()); limit("reset-code",flow.email(),request,20,600);
            try {flows.authorize(flow,auth.verifyResetCode(flow.email(),body.code()),session);}
            catch(AuthException e) {
                // An email draft is not proof of ownership. Keep all unsuccessful OTP outcomes uniform.
                if(Set.of("INVALID_CODE","CODE_EXPIRED","CODE_ATTEMPTS_EXCEEDED").contains(e.code))
                    throw new AuthException(400,"INVALID_CODE","Mã không hợp lệ, đã hết hạn hoặc không còn dùng được. Vui lòng gửi lại mã.");
                throw e;
            }
            return new AuthDtos.Message("Mã đã được xác minh. Bạn có thể tạo mật khẩu mới.",0);
        }
    }
    @PostMapping("/reset-password") public AuthDtos.Message reset(@Valid @RequestBody AuthDtos.Reset body,HttpServletRequest request,HttpServletResponse response) {
        var session=AuthFlowState.requireSession(request,false);
        synchronized(WebUtils.getSessionMutex(session)) {
            var flow=flows.reset(session,body.flowId()); var permit=flows.permit(session); limit("reset-finish",flow.email(),request,10,600);
            auth.reset(permit,body); session.removeAttribute(AuthFlowState.RESET); sessions.clear(request,response);
            return new AuthDtos.Message("Mật khẩu đã được cập nhật. Vui lòng đăng nhập lại.",0);
        }
    }
    @PostMapping("/oauth/link") public Map<String,String> link(@AuthenticationPrincipal AuthPrincipal principal,
        @Valid @RequestBody AuthDtos.LinkStart body,HttpServletRequest request) {
        if(principal==null) throw new AuthException(401,"SESSION_EXPIRED","Vui lòng đăng nhập lại.");
        if(!providers.configured(body.provider())) throw new AuthException(503,"OAUTH_NOT_CONFIGURED","Nhà cung cấp đăng nhập chưa được cấu hình.");
        User user=auth.current(principal); limit("login",user.getEmail(),request,10,900);
        auth.authenticate(user.getEmail(),body.password());
        request.getSession().setAttribute("OAUTH_LINK",new OAuthAccounts.LinkIntent(user.getId(),user.getAuthVersion(),body.provider(),clock.millis()+300_000));
        return Map.of("authorizationUrl","/oauth2/authorization/"+body.provider());
    }
    @GetMapping("/oauth/pending") public Map<String,String> pending(HttpServletRequest request) {
        var external=pendingIdentity(request); return Map.of("provider",external.provider(),"name",external.name()==null?"":external.name());
    }
    private OAuthAccounts.External pendingIdentity(HttpServletRequest request) {
        var session=request.getSession(false);
        if(session==null || !(session.getAttribute("OAUTH_PENDING") instanceof OAuthAccounts.External external) || external.expiresAt()<clock.millis())
            throw new AuthException(400,"OAUTH_SESSION_EXPIRED","Phiên đăng nhập mạng xã hội đã hết hạn. Vui lòng bắt đầu lại.");
        return external;
    }
    @PostMapping("/oauth/complete") public AuthDtos.Message complete(@Valid @RequestBody AuthDtos.Complete body,HttpServletRequest request) {
        var external=pendingIdentity(request); limit("mail",body.email(),request,5,3600);
        try {flows.beginVerification(accounts.complete(external,body),request);}
        catch(AuthService.PendingDeliveryFailure e) {flows.beginVerification(e.pending,request); request.getSession().removeAttribute("OAUTH_PENDING"); throw e;}
        request.getSession().removeAttribute("OAUTH_PENDING");
        return new AuthDtos.Message("Mã xác thực đã được gửi. Vui lòng xác thực email để hoàn tất tài khoản.",AuthService.RESEND_SECONDS);
    }
}
