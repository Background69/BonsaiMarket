package com.bonsaimarket.backend.config;

import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/** Khóa seed theo profile và từ chối hash demo không hợp lệ trước khi Flyway ghi dữ liệu. */
@Configuration(proxyBeanMethods=false)
@Profile("dev | prod | production")
public class DatabaseMigrationConfiguration {
    @Bean FlywayConfigurationCustomizer databaseMigrationPolicy(Environment environment) {
        return configuration -> {
            configuration.cleanDisabled(true).baselineOnMigrate(false).validateOnMigrate(true);
            if(environment.acceptsProfiles(Profiles.of("prod", "production"))) {
                configuration.locations("classpath:db/migration");
                return;
            }
            String hash=configuration.getPlaceholders().getOrDefault("devSeedPasswordHash","");
            if(!hash.matches("[$]2[aby][$](0[4-9]|[12][0-9]|3[01])[$][./A-Za-z0-9]{53}"))
                throw new IllegalStateException("Dev cần DEV_SEED_PASSWORD_HASH là BCrypt hợp lệ; không có mật khẩu mặc định.");
            configuration.locations("classpath:db/migration","classpath:db/dev");
        };
    }
}
