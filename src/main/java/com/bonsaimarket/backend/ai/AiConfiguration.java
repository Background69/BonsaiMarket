package com.bonsaimarket.backend.ai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiSettings.class)
public class AiConfiguration {
    @Bean
    FilterRegistrationBean<AiRequestGuard> aiGuardRegistration(AiRequestGuard guard) {
        var registration = new FilterRegistrationBean<>(guard);
        registration.setEnabled(false); // Runs once inside Spring Security, not twice as a servlet filter.
        return registration;
    }
}
