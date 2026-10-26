package com.bonsaimarket.backend.shipping;

import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in production Flyway SQL on empty, task-owned loopback schemas only. No team database. */
@EnabledIfEnvironmentVariable(named="BM_SHIPPING_MYSQL_URL",matches=".+")
class ShippingMySqlMigrationTest {
    String password=System.getenv("BM_SHIPPING_MYSQL_PASSWORD");
    Flyway flyway(String url,String version){var c=Flyway.configure().dataSource(url,"root",password).locations("filesystem:src/main/resources/db/migration").cleanDisabled(true);if(version!=null)c.target(version);return c.load();}
    void empty(String url)throws Exception{
        assertNotNull(url);assertTrue(url.matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/bm_shipping_(clean|upgrade)_[a-z0-9]+\\?.*"));
        try(var c=DriverManager.getConnection(url,"root",password);var s=c.createStatement();var r=s.executeQuery("select count(*) from information_schema.tables where table_schema=database()")){
            assertTrue(c.getMetaData().getDatabaseProductVersion().startsWith("8.4."));assertTrue(r.next());assertEquals(0,r.getInt(1),"Refuse non-empty schema before any writes");
        }
    }
    @Test void freshAndUpgradeRunRealFlywayAndConstraintsPreserveLegacy()throws Exception{
        String clean=System.getenv("BM_SHIPPING_MYSQL_URL"),upgrade=System.getenv("BM_SHIPPING_MYSQL_UPGRADE_URL");empty(clean);empty(upgrade);
        flyway(clean,null).migrate();flyway(clean,null).validate();assertEquals("19",flyway(clean,null).info().current().getVersion().toString());
        flyway(upgrade,"18").migrate();
        try(var c=DriverManager.getConnection(upgrade,"root",password);var s=c.createStatement()){
            s.executeUpdate("insert into users(id,name,email,password_hash,role,is_active,email_verified_at) values(1,'QA Admin','shipping-admin@example.invalid','!','ADMIN',true,CURRENT_TIMESTAMP),(2,'QA Seller','shipping-seller@example.invalid','!','SELLER',true,CURRENT_TIMESTAMP),(3,'QA Buyer','shipping-buyer@example.invalid','!','CUSTOMER',true,CURRENT_TIMESTAMP)");
            s.executeUpdate("insert into stores(id,owner_user_id,name,status) values(1,2,'Shipping QA','ACTIVE')");
            s.executeUpdate("insert into orders(id,code,buyer_user_id,store_id,shipping_address_snapshot,shipping_method,shipping_fee,subtotal,total,status) values(1,'SHIPPING-QA',3,1,'Legacy private address unchanged','STANDARD',25000,100000,125000,'CONFIRMED')");
        }
        flyway(upgrade,null).migrate();flyway(upgrade,null).validate();
        var evidence=new LinkedHashMap<String,Object>();evidence.put("boundary","Real MySQL8.4/Flyway only; no GHN, payment or team database");evidence.put("freshVersion",flyway(clean,null).info().current().getVersion().toString());evidence.put("upgradeFrom","18");
        try(var c=DriverManager.getConnection(upgrade,"root",password);var s=c.createStatement()){
            evidence.put("mysql",c.getMetaData().getDatabaseProductVersion());
            try(var r=s.executeQuery("select shipping_address_snapshot,shipping_fee,total from orders where id=1")){assertTrue(r.next());assertEquals("Legacy private address unchanged",r.getString(1));assertEquals(25000,r.getInt(2));assertEquals(125000,r.getInt(3));}
            sqlError(3819,()->s.executeUpdate("insert into store_public_locations(store_id,mode,published,region,ward,public_address,updated_at) values(1,'AREA',true,'Test','Test','',CURRENT_TIMESTAMP)"));
            sqlError(3819,()->s.executeUpdate("insert into store_public_locations(store_id,mode,published,region,ward,public_address,latitude,longitude,consent_at,updated_at) values(1,'EXACT',true,'Test','Test','Public',91,106,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)"));
            sqlError(3819,()->s.executeUpdate("insert into shipping_parcels(order_id,weight_grams,length_cm,width_cm,height_cm,carrier_accepted_goods,updated_at) values(1,0,10,10,10,true,CURRENT_TIMESTAMP)"));
            s.executeUpdate("insert into shipping_quotes(id,order_id,buyer_id,shop_id,parcel_revision,service_type,service_name,fee,order_fingerprint,destination_snapshot,pickup_snapshot,parcel_snapshot,expires_at,created_at) values('quote',1,3,123,1,2,'Test',25000,'fingerprint','{}','{}','{}',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
            s.executeUpdate("insert into shipments(id,order_id,quote_id,shop_id,client_reference,state,created_at,updated_at) values('shipment',1,'quote',123,'BM-stable','UNKNOWN',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
            sqlError(1062,()->s.executeUpdate("insert into shipments(id,order_id,quote_id,shop_id,client_reference,state,created_at,updated_at) values('duplicate',1,'quote',123,'BM-other','UNKNOWN',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)"));
            sqlError(1452,()->s.executeUpdate("insert into shipments(id,order_id,quote_id,shop_id,client_reference,state,created_at,updated_at) values('wrong-order',999999,'quote',123,'BM-other','UNKNOWN',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)"));
            s.executeUpdate("insert into shipment_events(shipment_id,dedup_key,carrier_status,state,event_at,received_at,source) values('shipment','event','picked','PICKED_UP',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'WEBHOOK')");
            sqlError(1062,()->s.executeUpdate("insert into shipment_events(shipment_id,dedup_key,carrier_status,state,event_at,received_at,source) values('shipment','event','picked','PICKED_UP',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'WEBHOOK')"));
            evidence.put("legacyPreserved",true);evidence.put("constraints","Consent, coordinate, parcel, FK, unique order and event dedup enforced");
        }
        Files.writeString(Path.of("artifacts/shipping-map-20261025/mysql-flyway.json"),new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(evidence));
    }
    void sqlError(int code,org.junit.jupiter.api.function.Executable work){assertEquals(code,assertThrows(SQLException.class,work).getErrorCode());}
}
