package com.bonsaimarket.backend.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Thực thi SQL thật với điều chỉnh H2 có giới hạn; không chứng minh Flyway trên MySQL. */
class StorefrontMigrationSeedTest {
    void execute(Connection c,Path path)throws Exception { com.bonsaimarket.backend.support.BaselineSql.execute(c,Files.readString(path)); }
    long scalar(Connection c,String sql)throws Exception {try(var s=c.createStatement();var r=s.executeQuery(sql)){assertTrue(r.next());return r.getLong(1);}}
    String text(Connection c,String sql)throws Exception {try(var s=c.createStatement();var r=s.executeQuery(sql)){assertTrue(r.next());return r.getString(1);}}
    void schema(Connection c,boolean seed)throws Exception { com.bonsaimarket.backend.support.BaselineSql.schema(c);if(seed)com.bonsaimarket.backend.support.BaselineSql.seed(c); }
    @Test void cleanSchemaHasNoDevelopmentRows()throws Exception {
        try(var c=DriverManager.getConnection("jdbc:h2:mem:baselineempty;MODE=MySQL","sa","")){schema(c,false);assertEquals(0,scalar(c,"select count(*) from products"));assertEquals(0,scalar(c,"select count(*) from users"));}
    }
    @Test void seedIsIdempotentInsertOnlyAndHasLinkedDiscoveryImagesSpecificationsAndStockStates()throws Exception {
        try(var c=DriverManager.getConnection("jdbc:h2:mem:storefrontseed;MODE=MySQL","sa","")) {
            schema(c,true);var seed=Path.of("src/main/resources/db/dev/R__dev_seed.sql");execute(c,seed);
            assertEquals(6,scalar(c,"select count(*) from stores"));assertEquals(69,scalar(c,"select count(*) from products"));assertEquals(11,scalar(c,"select count(*) from users"));
            assertEquals(5,scalar(c,"select count(*) from stores where banner_url like 'https://images.pexels.com/%' and about_shop is not null"));
            assertEquals(3,scalar(c,"select count(distinct p.store_id) from products p join categories c on c.id=p.category_id where p.slug like 'demo-storefront-%' and c.slug='sen-da'"));
            assertEquals(2,scalar(c,"select count(distinct p.store_id) from products p join categories c on c.id=p.category_id where p.slug like 'demo-storefront-%' and c.slug='bonsai'"));
            assertTrue(scalar(c,"select count(*) from products where slug like 'demo-storefront-%' and image_url is null")>0);
            assertTrue(scalar(c,"select count(*) from products where slug like 'demo-storefront-%' and image_url like 'https://images.pexels.com/%'")>0);
            assertTrue(scalar(c,"select count(*) from products where slug like 'demo-storefront-%' and stock=0")>0);assertTrue(scalar(c,"select count(*) from products where slug like 'demo-storefront-%' and stock=1")>0);
            assertEquals(49,scalar(c,"select count(distinct description) from products where slug like 'demo-storefront-%'"));
            assertTrue(scalar(c,"select count(*) from products where slug like 'demo-storefront-%' and char_length(description)>1200")>=2,"Seed includes real long-form descriptions for expand/collapse review");
            assertTrue(text(c,"select technical_specs_json from products where slug='demo-storefront-vuon-hien-1'").contains("heightCm"));
            try(var s=c.createStatement()){s.execute("update products set description='User edit',stock=42 where slug='demo-storefront-vuon-hien-1'");s.execute("update stores set about_shop='Owner edit' where name='Vườn Hiên · Demo'");}
            execute(c,seed);
            assertEquals(69,scalar(c,"select count(*) from products"));assertEquals(11,scalar(c,"select count(*) from users"));
            assertEquals("User edit",text(c,"select description from products where slug='demo-storefront-vuon-hien-1'"));assertEquals(42,scalar(c,"select stock from products where slug='demo-storefront-vuon-hien-1'"));
            assertEquals("Owner edit",text(c,"select about_shop from stores where name='Vườn Hiên · Demo'"));
        }
    }
    @Test void existingEmailCollisionNeverAdoptsOrChangesAnExistingUserOrStore()throws Exception {
        try(var c=DriverManager.getConnection("jdbc:h2:mem:storefrontcollision;MODE=MySQL","sa","")) {
            schema(c,false);
            try(var s=c.createStatement()){s.execute("insert into users(name,email,password_hash,role,is_active) values('Real user','storefront-v1-vuon-hien@example.invalid','untouched','CUSTOMER',true)");}
            execute(c,Path.of("src/main/resources/db/dev/R__dev_seed.sql"));
            assertEquals("CUSTOMER",text(c,"select role from users where email='storefront-v1-vuon-hien@example.invalid'"));
            assertEquals(0,scalar(c,"select count(*) from stores s join users u on u.id=s.owner_user_id where u.email='storefront-v1-vuon-hien@example.invalid'"));
            assertEquals(0,scalar(c,"select count(*) from products where slug like 'demo-storefront-vuon-hien-%'"));
        }
    }
    @Test void productionLocationsExcludeDevelopmentSeed()throws Exception {
        String config=Files.readString(Path.of("src/main/resources/application.yaml"));String production=config.split("---")[0];
        assertTrue(production.contains("locations: classpath:db/migration"));assertFalse(production.contains("classpath:db/dev"));
        assertTrue(config.contains("on-profile: dev"));
        assertTrue(config.contains("on-profile: prod | production"));
    }
}
