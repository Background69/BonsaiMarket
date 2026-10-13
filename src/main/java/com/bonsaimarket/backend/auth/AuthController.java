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

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final AuthSession sessions;
    private final AuthRateLimiter limits;
    private final OAuthRegistrations providers;
    private final OAuthAccounts accounts;
    private final Clock clock;
    private final PasswordResetRequests resetRequests;
    public AuthController(AuthService auth,AuthSession sessions,AuthRateLimiter limits,OAuthRegistrations providers,OAuthAccounts accounts,Clock clock,PasswordResetRequests resetRequests) {
        this.auth=auth;this.sessions=sessions;this.limits=limits;this.providers=providers;this.accounts=accounts;this.clock=clock;
        this.resetRequests=resetRequests;
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
        limit("mail",body.email(),request,5,3600); auth.register(body); return new AuthDtos.Message("Mã xác thực đã được gửi. Vui lòng kiểm tra email.",60);
    }
    @PostMapping("/verify-email") public AuthDtos.Message verify(@Valid @RequestBody AuthDtos.CodeRequest body,HttpServletRequest request) {
        limit("code",body.email(),request,20,600); auth.verify(body.email(),body.code()); return new AuthDtos.Message("Email đã được xác thực. Bạn có thể đăng nhập.",0);
    }
    @PostMapping("/resend-verification") public AuthDtos.Message resend(@Valid @RequestBody AuthDtos.EmailRequest body,HttpServletRequest request) {
        limit("mail",body.email(),request,5,3600); auth.resend(body.email()); return new AuthDtos.Message("Mã mới đã được gửi đến email của bạn.",60);
    }
    @PostMapping("/login") public AuthDtos.UserView login(@Valid @RequestBody AuthDtos.Login body,HttpServletRequest request,HttpServletResponse response) {
        limit("login",body.email(),request,10,900); User user=auth.authenticate(body.email(),body.password());
        sessions.login(user,request,response); return AuthDtos.UserView.of(user);
    }
    @PostMapping("/logout") public AuthDtos.Message logout(HttpServletRequest request,HttpServletResponse response) {
        sessions.clear(request,response); return new AuthDtos.Message("Bạn đã đăng xuất.",0);
    }
    @GetMapping("/me") public AuthDtos.UserView me(@AuthenticationPrincipal AuthPrincipal principal) {
        if(principal==null) throw new AuthException(401,"SESSION_EXPIRED","Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
        return AuthDtos.UserView.of(auth.current(principal));
    }
    @PostMapping("/forgot-password") @ResponseStatus(HttpStatus.ACCEPTED)
    public AuthDtos.Message forgot(@Valid @RequestBody AuthDtos.EmailRequest body,HttpServletRequest request) {
        limit("reset-mail",body.email(),request,5,3600); resetRequests.request(body.email());
        return new AuthDtos.Message("Nếu tài khoản đủ điều kiện, bạn sẽ nhận được mã đặt lại mật khẩu qua email.",60);
    }
    @PostMapping("/reset-password") public AuthDtos.Message reset(@Valid @RequestBody AuthDtos.Reset body,HttpServletRequest request) {
        limit("reset-code",body.email(),request,20,600); auth.reset(body); return new AuthDtos.Message("Mật khẩu đã được cập nhật. Vui lòng đăng nhập lại.",0);
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
        accounts.complete(external,body); request.getSession().removeAttribute("OAUTH_PENDING");
        return new AuthDtos.Message("Mã xác thực đã được gửi. Vui lòng xác thực email để hoàn tất tài khoản.",60);
    }
}
