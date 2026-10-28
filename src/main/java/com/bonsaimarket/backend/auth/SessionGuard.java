package com.bonsaimarket.backend.auth;

import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

public class SessionGuard extends OncePerRequestFilter {
    private final AuthService service;
    private final AuthSession sessions;
    public SessionGuard(AuthService service,AuthSession sessions) {this.service=service;this.sessions=sessions;}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        var authentication=SecurityContextHolder.getContext().getAuthentication();
        if(authentication!=null && authentication.getPrincipal() instanceof AuthPrincipal principal) {
            try {
                var user=service.current(principal);
                // Đổi vai trò hoặc khóa tài khoản có hiệu lực ngay ở request kế tiếp.
                SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal,null,
                    List.of(new SimpleGrantedAuthority("ROLE_"+user.getRole().name()))));
            } catch(AuthException e) {sessions.clear(request,response);}
        }
        chain.doFilter(request,response);
    }
}
