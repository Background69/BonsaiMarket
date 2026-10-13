package com.bonsaimarket.backend.auth;

import com.bonsaimarket.backend.user.User;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.*;
import java.util.List;

@Component
public class AuthSession {
    private final HttpSessionSecurityContextRepository repository=new HttpSessionSecurityContextRepository();
    public void login(User user,HttpServletRequest request,HttpServletResponse response) {
        if(request.getSession(false)!=null) request.changeSessionId(); else request.getSession(true);
        request.getSession().removeAttribute("OAUTH_PENDING"); request.getSession().removeAttribute("OAUTH_LINK");
        CsrfTokenRepository csrf=new HttpSessionCsrfTokenRepository(); csrf.saveToken(null,request,response);
        var context=SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(AuthPrincipal.of(user),null,
            List.of(new SimpleGrantedAuthority("ROLE_"+user.getRole().name()))));
        SecurityContextHolder.setContext(context); repository.saveContext(context,request,response);
    }
    public void clear(HttpServletRequest request,HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        if(request.getSession(false)!=null) request.getSession(false).invalidate();
        Cookie cookie=new Cookie("JSESSIONID",""); cookie.setHttpOnly(true); cookie.setSecure(request.isSecure());
        cookie.setPath("/"); cookie.setMaxAge(0); cookie.setAttribute("SameSite","Lax"); response.addCookie(cookie);
    }
}
