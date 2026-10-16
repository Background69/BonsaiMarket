package com.bonsaimarket.backend.auth;

import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Set;

public class OAuthAvailabilityFilter extends OncePerRequestFilter {
    private final OAuthRegistrations providers;
    public OAuthAvailabilityFilter(OAuthRegistrations providers) {this.providers=providers;}
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"GET".equals(request.getMethod()) || !request.getRequestURI().startsWith(request.getContextPath()+"/oauth2/authorization/");
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        String provider=request.getRequestURI().substring((request.getContextPath()+"/oauth2/authorization/").length());
        if(!providers.configured(provider)) {
            response.setStatus(Set.of("google","facebook").contains(provider)?503:404);
            response.setHeader("Cache-Control","no-store"); response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"OAUTH_NOT_CONFIGURED\",\"message\":\"Nhà cung cấp đăng nhập chưa được cấu hình.\"}");return;
        }
        chain.doFilter(request,response);
    }
}
