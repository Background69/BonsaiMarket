package com.bonsaimarket.backend.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Báo khả năng gửi email khi khởi động, không ghi giá trị bí mật. */
@Component
public class AuthStartupCheck {
    private static final Logger log=LoggerFactory.getLogger(AuthStartupCheck.class);
    private final AuthSettings settings;
    private final Environment env;
    public AuthStartupCheck(AuthSettings settings,Environment env) {this.settings=settings;this.env=env;}
    @EventListener(ApplicationReadyEvent.class) public void report() {
        String host=env.getProperty("spring.mail.host","");
        boolean auth=Boolean.parseBoolean(env.getProperty("spring.mail.properties.mail.smtp.auth","true"));
        if(settings.otpKey.length<32) log.warn("Auth: AUTH_OTP_HMAC_KEY is missing - registration and password reset will return 503. Generate one with: openssl rand -base64 32");
        else if(host.isBlank()) log.warn("Auth: MAIL_HOST is empty - verification emails cannot be sent.");
        else if(settings.mailFrom.isBlank()) log.warn("Auth: MAIL_FROM is empty - verification emails cannot be sent.");
        else if(auth && (env.getProperty("spring.mail.username","").isBlank() || env.getProperty("spring.mail.password","").isBlank()))
            log.warn("Auth: SMTP auth is on but MAIL_USERNAME / MAIL_PASSWORD is empty - the SMTP server will reject the login.");
        else log.info("Auth: verification email via SMTP {}:{} (from {})",host,env.getProperty("spring.mail.port","587"),settings.mailFrom);
    }
}
