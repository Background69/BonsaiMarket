package com.bonsaimarket.backend.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Executes actual SQL with narrowly documented H2 syntax adaptations. This is not MySQL Flyway proof. */
class StorefrontMigrationSeedTest {
    void execute(Connection c,Path path)throws Exception {
        String sql=Files.readString(path).replace("\r\n","\n").replace("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4","")
            .replaceAll("CHARACTER SET (utf8mb4|ascii) COLLATE (utf8mb4_bin|ascii_bin)","").replaceAll("\\bSTORED\\b","")
            .replaceAll(",\\s+ADD ","; ALTER TABLE users ADD ")
            .replaceAll("DATE_SUB\\(CURRENT_TIMESTAMP\\(6\\), INTERVAL ([0-9]+) DAY\\)","DATEADD('DAY', -$1, CURRENT_TIMESTAMP(6))")
            .replace("UPDATE categories c JOIN (SELECT MIN(id) AS id FROM categories GROUP BY LOWER(TRIM(name))) canonical ON canonical.id=c.id\nSET c.name_key=LOWER(TRIM(c.name))",
                "UPDATE categories SET name_key=LOWER(TRIM(name)) WHERE id IN (SELECT MIN(id) FROM categories GROUP BY LOWER(TRIM(name)))")
            .replace("DROP TEMPORARY TABLE storefront_seed_owners","DROP TABLE storefront_seed_owners");
        ScriptUtils.executeSqlScript(c,new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)));
    }
    long scalar(Connection c,String sql)throws Exception {try(var s=c.createStatement();var r=s.executeQuery(sql)){assertTrue(r.next());return r.getLong(1);}}
    String text(Connection c,String sql)throws Exception {try(var s=c.createStatement();var r=s.executeQuery(sql)){assertTrue(r.next());return r.getString(1);}}
    void schema(Connection c,boolean legacySeed)throws Exception {
        var dir=Path.of("src/main/resources/db/migration");
        for(int v=1;v<=13;v++){
            if(v==9){if(legacySeed)execute(c,Path.of("src/main/resources/db/dev/V9__seed_development_data.sql"));continue;}
            final int version=v;
            try(var paths=Files.list(dir)){execute(c,paths.filter(p->p.getFileName().toString().startsWith("V"+version+"__")).findFirst().orElseThrow());}
        }
    }
    @Test void cleanSchemaAcceptsNullableFieldsAndLegacySchemaDataSurvives()throws Exception {
        try(var c=DriverManager.getConnection("jdbc:h2:mem:storefrontclean;MODE=MySQL","sa","")) {schema(c,false);assertEquals(0,scalar(c,"select count(*) from products"));assertEquals(0,scalar(c,"select count(*) from stores"));}
        try(var c=DriverManager.getConnection("jdbc:h2:mem:storefrontupgrade;MODE=MySQL","sa","")) {
            schema(c,true);assertEquals(20,scalar(c,"select count(*) from products"));assertEquals(1,scalar(c,"select count(*) from stores"));
            assertEquals(0,scalar(c,"select count(*) from products where content_revision<>0"));assertEquals(1,scalar(c,"select count(*) from stores where banner_url is null and about_shop is null"));
            assertEquals("Cây cảnh mẫu phục vụ môi trường phát triển.",text(c,"select description from products where id=1"));
            assertEquals(12,scalar(c,"select stock from products where id=1"));assertEquals(20,scalar(c,"select count(distinct slug) from products"));
        }
    }
    @Test void seedIsIdempotentInsertOnlyAndHasLinkedDiscoveryImagesSpecificationsAndStockStates()throws Exception {
        try(var c=DriverManager.getConnection("jdbc:h2:mem:storefrontseed;MODE=MySQL","sa","")) {
            schema(c,true);var seed=Path.of("src/main/resources/db/dev/V14__seed_storefront_catalog.sql");execute(c,seed);
            assertEquals(6,scalar(c,"select count(*) from stores"));assertEquals(69,scalar(c,"select count(*) from products"));assertEquals(8,scalar(c,"select count(*) from users"));
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
            assertEquals(69,scalar(c,"select count(*) from products"));assertEquals(8,scalar(c,"select count(*) from users"));
            assertEquals("User edit",text(c,"select description from products where slug='demo-storefront-vuon-hien-1'"));assertEquals(42,scalar(c,"select stock from products where slug='demo-storefront-vuon-hien-1'"));
            assertEquals("Owner edit",text(c,"select about_shop from stores where name='Vườn Hiên · Demo'"));
        }
    }
    @Test void existingEmailCollisionNeverAdoptsOrChangesAnExistingUserOrStore()throws Exception {
        try(var c=DriverManager.getConnection("jdbc:h2:mem:storefrontcollision;MODE=MySQL","sa","")) {
            schema(c,true);
            try(var s=c.createStatement()){s.execute("insert into users(name,email,password_hash,role,is_active) values('Real user','storefront-v1-vuon-hien@example.invalid','untouched','CUSTOMER',true)");}
            execute(c,Path.of("src/main/resources/db/dev/V14__seed_storefront_catalog.sql"));
            assertEquals("CUSTOMER",text(c,"select role from users where email='storefront-v1-vuon-hien@example.invalid'"));
            assertEquals(0,scalar(c,"select count(*) from stores s join users u on u.id=s.owner_user_id where u.email='storefront-v1-vuon-hien@example.invalid'"));
            assertEquals(0,scalar(c,"select count(*) from products where slug like 'demo-storefront-vuon-hien-%'"));
        }
    }
    @Test void productionLocationsExcludeDevelopmentSeedAndAllOldMigrationsRemainVersioned()throws Exception {
        String config=Files.readString(Path.of("src/main/resources/application.yaml"));String production=config.split("---")[0];
        assertTrue(production.contains("locations: classpath:db/migration"));assertFalse(production.contains("classpath:db/dev"));
        assertTrue(config.contains("on-profile: dev"));assertFalse(Files.exists(Path.of("src/main/resources/db/migration/V14__seed_storefront_catalog.sql")));
    }
}
