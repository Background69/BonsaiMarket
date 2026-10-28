package com.bonsaimarket.backend.auth;

import org.springframework.stereotype.Component;
import org.springframework.security.web.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Clock;

@Component
public class OAuthHandlers implements AuthenticationSuccessHandler,AuthenticationFailureHandler {
    private final OAuthAccounts accounts;
    private final AuthSession sessions;
    private final AuthSettings settings;
    private final Clock clock;
    private final AuthFlowState flows;
    public OAuthHandlers(OAuthAccounts accounts,AuthSession sessions,AuthSettings settings,Clock clock,AuthFlowState flows) {
        this.accounts=accounts;this.sessions=sessions;this.settings=settings;this.clock=clock;
        this.flows=flows;
    }
    @Override public void onAuthenticationSuccess(HttpServletRequest request,HttpServletResponse response,Authentication authentication) throws IOException {
        try {
            var token=(OAuth2AuthenticationToken)authentication;
            String provider=token.getAuthorizedClientRegistrationId();
            var principal=token.getPrincipal();
            String subject="google".equals(provider)?principal.getAttribute("sub"):principal.getAttribute("id");
            // Trường email của Facebook chưa chứng minh email đã được xác minh.
            boolean verified="google".equals(provider)&&principal instanceof OidcUser oidc&&Boolean.TRUE.equals(oidc.getEmailVerified());
            var external=new OAuthAccounts.External(provider,subject,principal.getAttribute("name"),principal.getAttribute("email"),verified,clock.millis()+600_000);
            var link=(OAuthAccounts.LinkIntent)request.getSession().getAttribute("OAUTH_LINK");
            request.getSession().removeAttribute("OAUTH_LINK");
            var user=accounts.resolve(external,link);
            if(user==null) {
                // Đăng nhập qua nhà cung cấp chưa đủ để cấp session cho danh tính chưa xác minh.
                SecurityContextHolder.clearContext();
                request.getSession().removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
                request.changeSessionId(); request.getSession().setAttribute("OAUTH_PENDING",external);
                response.sendRedirect(settings.frontendOrigin+"/oauth/complete"); return;
            }
            sessions.login(user,request,response);
            response.sendRedirect(settings.frontendOrigin+"/auth/callback");
        } catch(OAuthAccounts.PendingVerification e) {
            SecurityContextHolder.clearContext();
            request.getSession().removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            flows.beginVerification(e.pending,request);
            response.sendRedirect(settings.frontendOrigin+"/verify-email");
        } catch(AuthException e) {fail(request,response,e.code);}
        catch(DataIntegrityViolationException e) {fail(request,response,"OAUTH_EMAIL_COLLISION");}
        catch(Exception e) {fail(request,response,"OAUTH_FAILED");}
    }
    @Override public void onAuthenticationFailure(HttpServletRequest request,HttpServletResponse response,AuthenticationException exception) throws IOException {
        fail(request,response,"OAUTH_FAILED");
    }
    private void fail(HttpServletRequest request,HttpServletResponse response,String code) throws IOException {
        sessions.clear(request,response); response.sendRedirect(settings.frontendOrigin+"/login?authError="+code);
    }
}
