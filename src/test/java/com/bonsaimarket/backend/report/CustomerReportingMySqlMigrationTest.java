package com.bonsaimarket.backend.report;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.sql.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Creates new uniquely named loopback QA schemas only. Deliberately retains them as evidence. */
@EnabledIfEnvironmentVariable(named="BM_F19_MYSQL",matches="true")
class CustomerReportingMySqlMigrationTest {
    String password=System.getenv("BM_F19_MYSQL_PASSWORD");
    String schema,url;
    @BeforeEach void isolatedSchema()throws Exception{
        schema="bm_f19_migration_"+UUID.randomUUID().toString().replace("-","").substring(0,12);
        assertTrue(schema.matches("bm_f19_migration_[a-f0-9]{12}"));
        try(var c=DriverManager.getConnection("jdbc:mysql://127.0.0.1:13361/mysql?useSSL=false&allowPublicKeyRetrieval=true","root",password);var p=c.prepareStatement("select count(*) from information_schema.schemata where schema_name=?")){
            p.setString(1,schema);try(var r=p.executeQuery()){r.next();assertEquals(0,r.getInt(1));}c.createStatement().execute("CREATE DATABASE `"+schema+"` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
        }
        url="jdbc:mysql://127.0.0.1:13361/"+schema+"?useSSL=false&allowPublicKeyRetrieval=true";
        System.out.println("Retained isolated report migration schema: "+schema);
    }
    Flyway flyway(String target){var config=Flyway.configure().dataSource(url,"root",password).locations("filesystem:src/main/resources/db/migration").cleanDisabled(true);if(target!=null)config.target(target);return config.load();}
    @Test void freshV1ThroughV6AndCustomerConstraints()throws Exception{
        assertEquals(6,flyway(null).migrate().migrationsExecuted);flyway(null).validate();
        try(var c=DriverManager.getConnection(url,"root",password);var s=c.createStatement()){
            s.executeUpdate("insert into users(id,name,email,password_hash,role,is_active) values(1,'QA seller','seller@example.invalid','!disabled!','SELLER',true),(2,'QA customer','customer@example.invalid','!disabled!','CUSTOMER',true)");
            s.executeUpdate("insert into marketplace_reports(reporter_id,target_type,target_id,reason,description,status,created_at) values(1,'STORE',99,'OTHER','Legacy permitted target','OPEN',CURRENT_TIMESTAMP)");
            assertEquals(3819,assertThrows(SQLException.class,()->s.executeUpdate("insert into marketplace_reports(reporter_id,target_type,target_id,reason,description,status,created_at) values(1,'USER',2,'FRAUD','Missing context','OPEN',CURRENT_TIMESTAMP)")).getErrorCode());
            assertEquals(1452,assertThrows(SQLException.class,()->s.executeUpdate("insert into marketplace_reports(reporter_id,target_type,target_id,reason,description,status,customer_user_id,context_order_id,request_key,request_fingerprint,created_at) values(1,'USER',2,'FRAUD','Invalid FK','OPEN',2,999,'00000000-0000-0000-0000-000000000000',REPEAT('a',64),CURRENT_TIMESTAMP)")).getErrorCode());
            try(var r=s.executeQuery("select count(*) from flyway_schema_history where success=1")){r.next();assertEquals(6,r.getInt(1));}
        }
    }
    @Test void existingV5ReportAndHistorySurviveUpgrade()throws Exception{
        assertEquals(5,flyway("5").migrate().migrationsExecuted);
        try(var c=DriverManager.getConnection(url,"root",password);var s=c.createStatement()){
            s.executeUpdate("insert into users(id,name,email,password_hash,role,is_active) values(1,'QA reporter','reporter@example.invalid','!disabled!','CUSTOMER',true)");
            s.executeUpdate("insert into marketplace_reports(id,reporter_id,target_type,target_id,reason,description,status,resolution,revision,created_at) values(1,1,'STORE',9,'OTHER','Existing store report','IN_REVIEW','Existing decision',7,CURRENT_TIMESTAMP)");
            s.executeUpdate("insert into marketplace_audit(actor_id,action,target_type,target_id,reason,created_at) values(1,'REPORT_IN_REVIEW','REPORT',1,'Existing audit',CURRENT_TIMESTAMP)");
        }
        assertEquals(1,flyway(null).migrate().migrationsExecuted);flyway(null).validate();assertEquals(0,flyway(null).migrate().migrationsExecuted);
        try(var c=DriverManager.getConnection(url,"root",password);var s=c.createStatement();var r=s.executeQuery("select r.target_type,r.description,r.resolution,r.revision,r.customer_user_id,r.context_order_id,a.reason from marketplace_reports r join marketplace_audit a on a.target_id=r.id and a.target_type='REPORT' where r.id=1")){
            assertTrue(r.next());assertEquals("STORE",r.getString(1));assertEquals("Existing store report",r.getString(2));assertEquals("Existing decision",r.getString(3));assertEquals(7,r.getLong(4));assertNull(r.getObject(5));assertNull(r.getObject(6));assertEquals("Existing audit",r.getString(7));
        }
    }
}
