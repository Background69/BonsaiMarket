package com.bonsaimarket.backend.auth;

import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;

public class AuthResponseFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        if(request.getRequestURI().startsWith(request.getContextPath()+"/api/auth") || request.getRequestURI().contains("/oauth2/")) {
            response.setHeader("Cache-Control","no-store"); response.setHeader("Pragma","no-cache");
            response.setHeader("Referrer-Policy","no-referrer");
        }
        chain.doFilter(request,response);
    }
}
