package com.bonsaimarket.backend.config;

import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseMigrationConfigurationTest {
    @Test void productionCannotScanSeedEvenWhenDevOrLocationsOverrideArePresent() {
        for(String production:List.of("prod","production")) {
            var environment=new MockEnvironment();environment.setActiveProfiles("dev",production);
            var configuration=Flyway.configure().locations("classpath:db/dev").cleanDisabled(false).baselineOnMigrate(true).validateOnMigrate(false);
            new DatabaseMigrationConfiguration().databaseMigrationPolicy(environment).customize(configuration);
            assertEquals(List.of("classpath:db/migration"),Arrays.stream(configuration.getLocations()).map(Object::toString).toList());
            assertTrue(configuration.isCleanDisabled());assertFalse(configuration.isBaselineOnMigrate());assertTrue(configuration.isValidateOnMigrate());
        }
    }
    @Test void devRejectsMissingOrMalformedHashAndAcceptsAnExplicitBcrypt() {
        var environment=new MockEnvironment();environment.setActiveProfiles("dev");
        var policy=new DatabaseMigrationConfiguration().databaseMigrationPolicy(environment);
        for(String hash:List.of("","${DEV_SEED_PASSWORD_HASH}","not-a-hash")) {
            var configuration=Flyway.configure().placeholders(Map.of("devSeedPasswordHash",hash));
            assertThrows(IllegalStateException.class,()->policy.customize(configuration));
        }
        var configuration=Flyway.configure().placeholders(Map.of("devSeedPasswordHash",new BCryptPasswordEncoder().encode(UUID.randomUUID().toString())));
        policy.customize(configuration);
        assertEquals(Set.of("classpath:db/migration","classpath:db/dev"),new HashSet<>(Arrays.stream(configuration.getLocations()).map(Object::toString).toList()));
    }
}
