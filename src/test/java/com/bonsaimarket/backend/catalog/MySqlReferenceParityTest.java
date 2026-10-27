package com.bonsaimarket.backend.catalog;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in only: caller provisions two disposable, empty MySQL schemas; no DROP/repair/clean. */
@EnabledIfEnvironmentVariable(named="BM_PARITY_FLYWAY_URL",matches=".+")
class MySqlReferenceParityTest {
    @Test void flywayAndBootstrapHaveIdenticalMySqlBusinessSchema()throws Exception {
        String aUrl=System.getenv("BM_PARITY_FLYWAY_URL"),bUrl=System.getenv("BM_PARITY_BOOTSTRAP_URL");
        assertNotNull(bUrl,"Both isolated schema URLs are required");assertNotEquals(aUrl,bUrl);
        String user=System.getenv("BM_PARITY_USER"),password=System.getenv("BM_PARITY_PASSWORD");
        try(var a=DriverManager.getConnection(aUrl,user,password);var b=DriverManager.getConnection(bUrl,user,password)){
            for(var c:List.of(a,b)){
                assertEquals("MySQL",c.getMetaData().getDatabaseProductName());
                assertTrue(c.getCatalog().matches("bm_parity_[a-z0-9_]+"),"Only explicitly named bm_parity_* schemas are allowed");
                assertEquals(0,scalar(c,"select count(*) from information_schema.tables where table_schema=database()"),"Refuse a nonempty schema");
            }
            Flyway.configure().dataSource(aUrl,user,password).locations("filesystem:src/main/resources/db/migration","filesystem:src/main/resources/db/dev").load().migrate();
            ScriptUtils.executeSqlScript(b,new FileSystemResource("src/main/resources/db/reference/bonsaimarket_dev_bootstrap.sql"));
            for(String query:metadataQueries())assertEquals(snapshot(a,query),snapshot(b,query),query);
            var evidence=Path.of(System.getenv().getOrDefault("BM_TEST_OUTPUT","artifacts/discovery"),"mysql-parity.txt");Files.createDirectories(evidence.getParent());Files.writeString(evidence,"PASS: MySQL "+a.getMetaData().getDatabaseProductVersion()+"; tables, columns, defaults, generated expressions, collations, constraints, checks, FK actions and indexes match. flyway_schema_history excluded.\n");
        }
    }
    static long scalar(Connection c,String sql)throws Exception{try(var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
    static List<String> metadataQueries(){return List.of(
        "select TABLE_NAME,ENGINE,TABLE_COLLATION from information_schema.TABLES where TABLE_SCHEMA=database() and TABLE_NAME<>'flyway_schema_history'",
        "select TABLE_NAME,COLUMN_NAME,ORDINAL_POSITION,COLUMN_TYPE,IS_NULLABLE,COLUMN_DEFAULT,EXTRA,GENERATION_EXPRESSION,CHARACTER_SET_NAME,COLLATION_NAME from information_schema.COLUMNS where TABLE_SCHEMA=database() and TABLE_NAME<>'flyway_schema_history'",
        "select TABLE_NAME,INDEX_NAME,NON_UNIQUE,SEQ_IN_INDEX,COLUMN_NAME,COLLATION,SUB_PART,INDEX_TYPE,EXPRESSION from information_schema.STATISTICS where TABLE_SCHEMA=database() and TABLE_NAME<>'flyway_schema_history'",
        "select TABLE_NAME,CONSTRAINT_NAME,CONSTRAINT_TYPE,ENFORCED from information_schema.TABLE_CONSTRAINTS where CONSTRAINT_SCHEMA=database() and TABLE_NAME<>'flyway_schema_history'",
        "select TABLE_NAME,CONSTRAINT_NAME,COLUMN_NAME,ORDINAL_POSITION,POSITION_IN_UNIQUE_CONSTRAINT,REFERENCED_TABLE_NAME,REFERENCED_COLUMN_NAME from information_schema.KEY_COLUMN_USAGE where CONSTRAINT_SCHEMA=database() and TABLE_NAME<>'flyway_schema_history'",
        "select TABLE_NAME,CONSTRAINT_NAME,UNIQUE_CONSTRAINT_NAME,MATCH_OPTION,UPDATE_RULE,DELETE_RULE,REFERENCED_TABLE_NAME from information_schema.REFERENTIAL_CONSTRAINTS where CONSTRAINT_SCHEMA=database()",
        "select CONSTRAINT_NAME,CHECK_CLAUSE from information_schema.CHECK_CONSTRAINTS where CONSTRAINT_SCHEMA=database()");}
    static Set<String> snapshot(Connection c,String sql)throws Exception {var rows=new TreeSet<String>();try(var s=c.createStatement();var r=s.executeQuery(sql)){while(r.next()){var row=new ArrayList<String>();for(int i=1;i<=r.getMetaData().getColumnCount();i++)row.add(Objects.toString(r.getObject(i),"<NULL>"));rows.add(String.join("|",row));}}return rows;}
}
