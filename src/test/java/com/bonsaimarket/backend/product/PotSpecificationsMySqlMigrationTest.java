package com.bonsaimarket.backend.product;

import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import static org.junit.jupiter.api.Assertions.*;

/** NOT RUN during implementation. Only an explicitly supplied empty bm_pot_* schema may be used later. */
@EnabledIfEnvironmentVariable(named="BM_POT_MIGRATION_URL",matches=".+")
class PotSpecificationsMySqlMigrationTest {
    @Test void upgradeBackfillsOnlyKnownPotsAndSeedReplayPreservesManualEdits() throws Exception {
        String url=System.getenv("BM_POT_MIGRATION_URL"),user=System.getenv("BM_POT_MIGRATION_USER"),password=System.getenv("BM_POT_MIGRATION_PASSWORD");
        try(var c=DriverManager.getConnection(url,user,password)){
            assertEquals("MySQL",c.getMetaData().getDatabaseProductName());assertTrue(c.getMetaData().getDatabaseProductVersion().startsWith("8.4."));
            assertTrue(c.getCatalog().matches("bm_pot_[a-z0-9_]+"));assertEquals(0,scalar(c,"select count(*) from information_schema.tables where table_schema=database()"));
            var config=Flyway.configure().dataSource(url,user,password).locations("filesystem:src/main/resources/db/migration").cleanDisabled(true).baselineOnMigrate(false);
            config.target("1").load().migrate();
            sql(c,"insert into users(id,name,email,password_hash,role) values(990001,'Migration fixture','pot-fixture@example.invalid','unused','SELLER')");
            sql(c,"insert into stores(id,owner_user_id,name,status) values(990001,990001,'Migration fixture','ACTIVE')");
            sql(c,"insert into categories(id,name,slug) values(990001,'Chậu gốm men','chau-gom-men'),(990002,'Cây fixture','plant-fixture'),(990003,'Chậu và phụ kiện','chau-va-phu-kien')");
            try(var s=c.prepareStatement("insert into products(store_id,category_id,name,slug,price,status,technical_specs_json) values(990001,?,? ,?,100000,'ACTIVE',?)")){
                for(var row:List.of(new String[]{"990001","Valid pot","pot-valid","{\"potMaterial\":\"Gốm\",\"potDiameterCm\":12}"},
                    new String[]{"990001","Malformed pot","pot-malformed","{broken"},new String[]{"990001","Invalid number","pot-invalid","{\"potDiameterCm\":\"NaN\"}"},
                    new String[]{"990002","Plant","plant-with-pot","{\"potMaterial\":\"Gốm\",\"potDiameterCm\":12}"},
                    new String[]{"990003","Accessory","mixed-accessory","{\"potMaterial\":\"Gốm\",\"potDiameterCm\":12}"})){
                    s.setLong(1,Long.parseLong(row[0]));s.setString(2,row[1]);s.setString(3,row[2]);s.setString(4,row[3]);s.executeUpdate();
                }
            }
            config.target("latest").load().migrate();config.load().validate();
            assertEquals(1,scalar(c,"select count(*) from product_pot_specs"));assertEquals(12,scalar(c,"select diameter_cm from product_pot_specs"));
            assertEquals(5,scalar(c,"select count(*) from products where technical_specs_json is not null"));
            assertThrows(SQLException.class,()->sql(c,"update product_pot_specs set height_cm=0"));
            assertThrows(SQLException.class,()->sql(c,"update product_pot_specs set drainage_status='NO',drainage_hole_count=1"));
            // Existing taxonomy was not created by the seed. Its image must stay untouched.
            seed(c);assertEquals(37,scalar(c,"select count(*) from product_pot_specs x join products p on p.id=x.product_id where p.slug like 'demo-%'"));
            assertEquals(0,scalar(c,"select count(*) from categories where id=990001 and image_url is not null"));
            long accounts=scalar(c,"select count(*) from users"),orders=scalar(c,"select count(*) from orders"),products=scalar(c,"select count(*) from products");
            sql(c,"update product_pot_specs x join products p on p.id=x.product_id set x.color='Manual color' where p.slug='demo-storefront-nha-chau-1'");
            sql(c,"update products set description='Manual description',content_revision=content_revision+1 where slug='demo-storefront-nha-chau-1'");
            seed(c);
            assertEquals(accounts,scalar(c,"select count(*) from users"));assertEquals(orders,scalar(c,"select count(*) from orders"));assertEquals(products,scalar(c,"select count(*) from products"));
            assertEquals(1,scalar(c,"select count(*) from product_pot_specs x join products p on p.id=x.product_id where p.slug='demo-storefront-nha-chau-1' and x.color='Manual color' and p.description='Manual description'"));
        }
    }
    private void seed(Connection c)throws Exception{String source=Files.readString(Path.of("src/main/resources/db/dev/R__dev_seed.sql")).replace("${devSeedPasswordHash}","!isolated-test-login-disabled!");ScriptUtils.executeSqlScript(c,new ByteArrayResource(source.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
    private void sql(Connection c,String sql)throws SQLException{try(var s=c.createStatement()){s.execute(sql);}}
    private long scalar(Connection c,String sql)throws SQLException{try(var s=c.createStatement();var r=s.executeQuery(sql)){assertTrue(r.next());return r.getLong(1);}}
}
