package com.bonsaimarket.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
import com.bonsaimarket.backend.ai.AiRequestGuard;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AiRequestGuard aiGuard) throws Exception {
        RequestMatcher aiChat = request -> "POST".equals(request.getMethod())
                && (request.getContextPath() + "/api/ai/chat").equals(request.getRequestURI());
        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers(aiChat))
                .addFilterBefore(aiGuard, AuthorizationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/seller/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/ai/chat").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/categories/**", "/api/products", "/api/products/**", "/api/stores", "/api/stores/**").permitAll()
                        .anyRequest().denyAll())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .build();
    }
}
