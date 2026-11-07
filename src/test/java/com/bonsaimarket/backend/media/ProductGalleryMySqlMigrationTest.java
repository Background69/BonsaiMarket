package com.bonsaimarket.backend.media;

import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

/** NOT RUN. Later opt-in requires an explicitly supplied EMPTY bm_gallery_* MySQL 8.4 schema. */
@EnabledIfEnvironmentVariable(named="BM_GALLERY_MIGRATION_URL",matches=".+")
class ProductGalleryMySqlMigrationTest {
    @Test void legacyBackfillKeepsSourcesAndGeneratedConstraintPreventsTwoCovers()throws Exception{
        String url=System.getenv("BM_GALLERY_MIGRATION_URL"),user=System.getenv("BM_GALLERY_MIGRATION_USER"),password=System.getenv("BM_GALLERY_MIGRATION_PASSWORD");
        try(var c=DriverManager.getConnection(url,user,password)){
            assertEquals("MySQL",c.getMetaData().getDatabaseProductName());assertTrue(c.getMetaData().getDatabaseProductVersion().startsWith("8.4."));assertTrue(c.getCatalog().matches("bm_gallery_[a-z0-9_]+"));
            assertEquals(0,scalar(c,"select count(*) from information_schema.tables where table_schema=database()"));
            var config=Flyway.configure().dataSource(url,user,password).locations("filesystem:src/main/resources/db/migration").cleanDisabled(true).baselineOnMigrate(false);
            config.target("3").load().migrate();
            sql(c,"insert into users(id,name,email,password_hash,role) values(990001,'Gallery fixture','gallery-fixture@example.invalid','unused','SELLER')");
            sql(c,"insert into stores(id,owner_user_id,name,status) values(990001,990001,'Gallery fixture','ACTIVE')");
            sql(c,"insert into categories(id,name,slug) values(990001,'Gallery fixture','gallery-fixture')");
            String a=UUID.randomUUID()+".png",b=UUID.randomUUID()+".png",d=UUID.randomUUID()+".png";
            try(var statement=c.prepareStatement("insert into products(id,store_id,category_id,name,slug,price,status,image_url) values(?,990001,990001,'Gallery fixture',?,100000,'ACTIVE',?)")){
                for(int i=1;i<=4;i++){statement.setLong(1,990010+i);statement.setString(2,"gallery-fixture-"+i);statement.setString(3,i==1?"/api/products/990011/image?v="+a.substring(0,36):i==2?"https://example.invalid/selected-cover.jpg":i==3?"https://example.invalid/external-only.jpg":null);statement.executeUpdate();}
            }
            try(var statement=c.prepareStatement("insert into product_images(product_id,file_key,content_type,bytes) values(?,?,'image/png',100)")){
                for(var row:List.of(new String[]{"990011",a},new String[]{"990012",b},new String[]{"990014",d})){statement.setLong(1,Long.parseLong(row[0]));statement.setString(2,row[1]);statement.executeUpdate();}
            }
            config.target("4").load().migrate();config.load().validate();
            assertEquals(4,scalar(c,"select count(*) from product_gallery_images"));assertEquals(3,scalar(c,"select count(*) from product_images"));
            assertEquals(4,scalar(c,"select count(*) from products where gallery_initialized=true and image_url is not null"));
            assertEquals(2,scalar(c,"select count(*) from product_gallery_images where storage_key is not null and file_size_bytes=100 and width is null and height is null"));
            assertEquals(2,scalar(c,"select count(*) from product_gallery_images where storage_key is null and content_type is null and file_size_bytes is null"));
            assertEquals(1,scalar(c,"select count(*) from product_gallery_images where product_id=990012 and image_url='https://example.invalid/selected-cover.jpg' and is_primary=true"));
            assertThrows(SQLException.class,()->sql(c,"insert into product_gallery_images(product_id,image_url,sort_order,is_primary) values(990011,'https://example.invalid/other.jpg',1,true)"));
            sql(c,"insert into product_gallery_images(product_id,image_url,sort_order,is_primary) values(990011,'https://example.invalid/other.jpg',0,false)");
            assertEquals(2,scalar(c,"select count(*) from product_gallery_images where product_id=990011 and sort_order=0"));
            assertThrows(SQLException.class,()->sql(c,"update product_gallery_images set width=0,height=30 where product_id=990011"));
            assertEquals(3,scalar(c,"select count(*) from product_images"));
        }
    }
    void sql(Connection c,String sql)throws SQLException{try(var statement=c.createStatement()){statement.execute(sql);}}
    long scalar(Connection c,String sql)throws SQLException{try(var statement=c.createStatement();var rows=statement.executeQuery(sql)){assertTrue(rows.next());return rows.getLong(1);}}
}
