package com.bonsaimarket.backend.payment;

import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

/** Real MySQL fresh/current and V8 upgrade; no provider transport or team data. */
@EnabledIfEnvironmentVariable(named="BM_PROVIDER_MIGRATION_CLEAN_URL",matches=".+")
class ProviderPaymentMySqlMigrationTest {
    String password=System.getenv("BM_PROVIDER_MIGRATION_PASSWORD");
    Flyway flyway(String url,String target,boolean seed){
        var c=Flyway.configure().dataSource(url,"root",password).cleanDisabled(true).baselineOnMigrate(false)
            .locations(seed?new String[]{"classpath:db/migration","classpath:db/dev"}:new String[]{"classpath:db/migration"})
            .placeholders(Map.of("devSeedPasswordHash","!isolated-test-login-disabled!"));
        if(target!=null)c.target(target);return c.load();
    }
    long scalar(Connection c,String sql)throws Exception{try(var s=c.createStatement();var r=s.executeQuery(sql)){assertTrue(r.next());return r.getLong(1);}}
    @Test void freshAndV8UpgradePreserveSeedAndValidateCurrentSchema()throws Exception{
        String fresh=System.getenv("BM_PROVIDER_MIGRATION_CLEAN_URL"),upgrade=System.getenv("BM_PROVIDER_MIGRATION_UPGRADE_URL");
        for(String url:List.of(fresh,upgrade)){
            assertTrue(url.matches("jdbc:mysql://127[.]0[.]0[.]1:[0-9]+/bm_provider_(clean|upgrade)_[a-z0-9_]+[?].*"));
            try(var c=DriverManager.getConnection(url,"root",password)){
                assertTrue(c.getMetaData().getDatabaseProductVersion().startsWith("8.4."));
                assertEquals(0,scalar(c,"select count(*) from information_schema.tables where table_schema=database()"));
            }
        }
        flyway(fresh,null,false).migrate();flyway(fresh,null,false).validate();
        flyway(upgrade,"8",true).migrate();
        try(var c=DriverManager.getConnection(upgrade,"root",password)){
            assertEquals(95,scalar(c,"select count(*) from orders"));
            assertEquals(230,scalar(c,"select count(*) from order_items"));
            long owner=scalar(c,"select id from users where email='user@bonsai.market'");
            // Explicit simulated accounting history exists BEFORE V9, not just empty tables.
            try(var s=c.createStatement()){
                s.executeUpdate("insert into transactions(order_id,transaction_code,payment_method,amount,status) select min(id),'QA-V8-PENDING','VNPAY',1000,'PENDING' from orders");
                s.executeUpdate("insert into vnd_wallet_accounts(id,account_key,owner_id,available,held) values(990001,'QA-V8-BUYER',"+owner+",1000,0),(990002,'QA-V8-SIMULATOR',null,-1000,0)");
                s.executeUpdate("insert into vnd_wallet_transactions(id,owner_id,request_key,fingerprint,type,status,amount,created_at) values('qa-v8-topup',"+owner+",'qa-v8-topup',REPEAT('a',64),'TOP_UP','COMPLETED',1000,CURRENT_TIMESTAMP)");
                s.executeUpdate("insert into vnd_wallet_journals(id,transaction_id,phase,created_at) values('qa-v8-journal','qa-v8-topup','COMPLETE',CURRENT_TIMESTAMP)");
                s.executeUpdate("insert into vnd_wallet_entries(journal_id,account_id,available_delta,held_delta,available_after,held_after) values('qa-v8-journal',990001,1000,0,1000,0),('qa-v8-journal',990002,-1000,0,-1000,0)");
            }
            long payments=scalar(c,"select count(*) from order_payments"),transactions=scalar(c,"select count(*) from transactions");
            var before=new ArrayList<String>();try(var s=c.createStatement();var r=s.executeQuery("select code,status,total from orders order by id")){while(r.next())before.add(r.getString(1)+":"+r.getString(2)+":"+r.getBigDecimal(3));}
            flyway(upgrade,null,true).migrate();flyway(upgrade,null,true).validate();assertEquals(0,flyway(upgrade,null,true).migrate().migrationsExecuted);
            var after=new ArrayList<String>();try(var s=c.createStatement();var r=s.executeQuery("select code,status,total from orders order by id")){while(r.next())after.add(r.getString(1)+":"+r.getString(2)+":"+r.getBigDecimal(3));}
            assertEquals(before,after);assertEquals(payments,scalar(c,"select count(*) from order_payments"));assertEquals(transactions,scalar(c,"select count(*) from transactions"));
            assertEquals(0,scalar(c,"select count(*) from payment_events"));
            assertEquals(0,scalar(c,"select count(*) from transactions where currency is not null or gateway_created_date is not null or expires_at is not null or query_count<>0 or query_after is not null"));
            assertEquals(10,scalar(c,"select max(cast(version as unsigned)) from flyway_schema_history where success=1"));
            assertEquals(70,scalar(c,"select count(*) from information_schema.tables where table_schema=database() and table_name<>'flyway_schema_history'"));
            assertEquals(1,scalar(c,"select count(*) from transactions where transaction_code='QA-V8-PENDING' and status='PENDING' and amount=1000"));
            assertEquals(2,scalar(c,"select count(*) from vnd_wallet_entries where journal_id='qa-v8-journal'"));
            assertEquals(0,scalar(c,"select sum(available_delta+held_delta) from vnd_wallet_entries where journal_id='qa-v8-journal'"));
            assertEquals(1000,scalar(c,"select available from vnd_wallet_accounts where id=990001"));
            assertEquals(4,scalar(c,"select count(*) from information_schema.triggers where trigger_schema=database() and trigger_name in ('vnd_entries_no_update','vnd_entries_no_delete','vnd_journals_no_update','vnd_journals_no_delete')"));
            try(var s=c.createStatement()){
                for(String sql:List.of("update vnd_wallet_entries set available_delta=2 where journal_id='qa-v8-journal'","delete from vnd_wallet_entries where journal_id='qa-v8-journal'","update vnd_wallet_journals set phase='EDIT' where id='qa-v8-journal'","delete from vnd_wallet_journals where id='qa-v8-journal'"))
                    assertEquals("45000",assertThrows(SQLException.class,()->s.executeUpdate(sql)).getSQLState());
            }
        }
    }
}
