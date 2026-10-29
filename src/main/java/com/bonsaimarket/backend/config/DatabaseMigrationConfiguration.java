package com.bonsaimarket.backend.config;

import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/** Khóa seed theo profile và từ chối hash demo không hợp lệ trước khi Flyway ghi dữ liệu. */
@Configuration(proxyBeanMethods=false)
@Profile("dev | prod | production")
public class DatabaseMigrationConfiguration {
    // Public development credential only. Never applied outside the dev policy.
    public static final String DEMO_PASSWORD_HASH="$2a$12$DbAHOPB1V7Nxd0WtEnGU4eGlAcfYN9Cf5PFv8SvrOGTLllIj92nT6";
    @Bean public FlywayConfigurationCustomizer databaseMigrationPolicy(Environment environment) {
        return configuration -> {
            configuration.cleanDisabled(true).baselineOnMigrate(false).validateOnMigrate(true);
            if(environment.acceptsProfiles(Profiles.of("prod", "production"))) {
                configuration.locations("classpath:db/migration");
                var placeholders=new java.util.HashMap<>(configuration.getPlaceholders());
                placeholders.remove("devSeedPasswordHash");configuration.placeholders(placeholders);
                return;
            }
            String hash=configuration.getPlaceholders().getOrDefault("devSeedPasswordHash","");
            if(hash.isBlank()) {
                hash=DEMO_PASSWORD_HASH;
                var placeholders=new java.util.HashMap<>(configuration.getPlaceholders());
                placeholders.put("devSeedPasswordHash",hash);configuration.placeholders(placeholders);
            }
            if(!hash.matches("[$]2[aby][$](0[4-9]|[12][0-9]|3[01])[$][./A-Za-z0-9]{53}"))
                throw new IllegalStateException("DEV_SEED_PASSWORD_HASH tùy chọn phải là BCrypt hợp lệ.");
            configuration.locations("classpath:db/migration","classpath:db/dev");
        };
    }
}
