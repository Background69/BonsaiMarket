package com.bonsaimarket.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
import com.bonsaimarket.backend.ai.AiRequestGuard;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import com.bonsaimarket.backend.auth.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.core.Authentication;
import jakarta.servlet.http.*;
import org.springframework.web.cors.*;
import java.time.Clock;
import java.util.List;

@Configuration
public class SecurityConfig {
    @Bean Clock authClock() {return Clock.systemUTC();}
    @Bean PasswordEncoder passwordEncoder() {return new BCryptPasswordEncoder(12);}
    @Bean(name="authMailExecutor") org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor authMailExecutor() {
        var executor=new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2); executor.setMaxPoolSize(2); executor.setQueueCapacity(64);
        executor.setThreadNamePrefix("auth-mail-"); executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15); return executor;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AiRequestGuard aiGuard,
        AuthService authService, AuthSession sessions, AuthSettings settings, OAuthRegistrations providers, OAuthHandlers handlers) throws Exception {
        RequestMatcher aiChat = request -> "POST".equals(request.getMethod())
                && (request.getContextPath() + "/api/ai/chat").equals(request.getRequestURI());
        var cors=new CorsConfiguration(); cors.setAllowedOrigins(List.of(settings.frontendOrigin));
        cors.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS")); cors.setAllowedHeaders(List.of("Content-Type","X-CSRF-TOKEN","Accept"));
        cors.setExposedHeaders(List.of("Retry-After")); cors.setAllowCredentials(true); cors.setMaxAge(600L);
        var source=new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/api/**",cors);
        http
                .cors(c -> c.configurationSource(source))
                .csrf(csrf -> csrf.ignoringRequestMatchers(aiChat))
                .addFilterAfter(new AuthBodyFilter(), org.springframework.security.web.csrf.CsrfFilter.class)
                .addFilterAfter(new com.bonsaimarket.backend.marketplace.MarketplaceBodyFilter(), org.springframework.security.web.csrf.CsrfFilter.class)
                .addFilterBefore(new AuthResponseFilter(), SecurityContextHolderFilter.class)
                .addFilterBefore(new OAuthAvailabilityFilter(providers), SecurityContextHolderFilter.class)
                .addFilterBefore(aiGuard, AuthorizationFilter.class)
                .addFilterBefore(new SessionGuard(authService,sessions), AuthorizationFilter.class)
                .addFilterAfter(new com.bonsaimarket.backend.adminai.AdminAiBodyFilter(), AuthorizationFilter.class)
                .requestCache(cache -> cache.disable())
                .securityContext(context -> context.requireExplicitSave(true))
                .sessionManagement(session -> session.sessionFixation(fixation -> fixation.changeSessionId()))
                .exceptionHandling(errors -> errors
                    .authenticationEntryPoint((request,response,error) -> {
                        boolean privateApi=request.getRequestURI().matches(".*/api/(auth/me|auth/oauth/link|auth/change-password|account(?:/.*)?|admin(?:/.*)?|seller(?:/.*)?)");
                        response.setStatus(privateApi?401:403); response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write(privateApi?"{\"code\":\"SESSION_EXPIRED\",\"message\":\"Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.\"}":"{\"code\":\"FORBIDDEN\",\"message\":\"Bạn không có quyền truy cập.\"}");
                    })
                    .accessDeniedHandler((request,response,error) -> {
                        response.setStatus(403); response.setContentType("application/json;charset=UTF-8");
                        boolean csrf=error instanceof org.springframework.security.web.csrf.CsrfException;
                        response.getWriter().write(csrf?"{\"code\":\"CSRF_INVALID\",\"message\":\"Phiên biểu mẫu đã hết hạn. Vui lòng thử lại.\"}":"{\"code\":\"FORBIDDEN\",\"message\":\"Bạn không có quyền truy cập.\"}");
                    }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/auth/csrf","/api/auth/providers","/api/auth/oauth/pending","/api/auth/verification-context","/oauth2/authorization/**","/login/oauth2/code/**").permitAll()
                        .requestMatchers(HttpMethod.POST,"/api/auth/register","/api/auth/login","/api/auth/logout","/api/auth/verify-email","/api/auth/resend-verification","/api/auth/forgot-password","/api/auth/verify-reset-code","/api/auth/resend-reset-code","/api/auth/reset-password","/api/auth/oauth/complete").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/auth/me").authenticated()
                        .requestMatchers(HttpMethod.POST,"/api/auth/oauth/link","/api/auth/change-password").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/account/**").authenticated()
                        .requestMatchers("/api/seller/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/ai/chat").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/ai/knowledge").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/community/posts", "/api/community/posts/**", "/api/categories", "/api/categories/**", "/api/products", "/api/products/**", "/api/stores", "/api/stores/**").permitAll()
                        .anyRequest().denyAll())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable());
        if(providers.any()) http.oauth2Login(oauth -> oauth
            .clientRegistrationRepository(providers)
            .authorizedClientRepository(new OAuth2AuthorizedClientRepository() {
                @Override public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String id,Authentication principal,HttpServletRequest request) {return null;}
                @Override public void saveAuthorizedClient(OAuth2AuthorizedClient client,Authentication principal,HttpServletRequest request,HttpServletResponse response) {}
                @Override public void removeAuthorizedClient(String id,Authentication principal,HttpServletRequest request,HttpServletResponse response) {}
            })
            .successHandler(handlers).failureHandler(handlers));
        return http.build();
    }
}
