package com.bonsaimarket.backend.review;

import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in owned MySQL + actual local model + Spring HTTP + Chrome. No model double. */
@EnabledIfEnvironmentVariable(named="BM_SENTIMENT_QA_URL",matches=".+")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.profiles.active=sentiment-qa","server.address=127.0.0.1",
    "spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=validate",
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
    "sentiment.enabled=true","sentiment.allow-demo=true","sentiment.poll-ms=1000",
    "server.servlet.session.cookie.secure=false","auth.frontend-origin=http://localhost:5179",
    "auth.public-origin=http://localhost:5179","ai.enabled=false","orders.mail.enabled=false"})
class RealModelSentimentQaTest extends SentimentTestSupport {
    static final Path OUT=Path.of("artifacts/sentiment-20261119");
    static final Map<String,Object> evidence=new LinkedHashMap<>();
    @LocalServerPort int port;
    @Autowired PasswordEncoder encoder;

    @DynamicPropertySource static void database(DynamicPropertyRegistry props) throws Exception {
        String url=System.getenv("BM_SENTIMENT_QA_URL"),password=System.getenv("BM_SENTIMENT_QA_PASSWORD");
        assertTrue(url.matches("jdbc:mysql://127[.]0[.]0[.]1:13364/bm_sentiment_qa_20261119[a-z0-9_]*[?].*"));
        try(var c=DriverManager.getConnection(url,"root",password);var s=c.createStatement()) {
            assertTrue(c.getMetaData().getDatabaseProductVersion().startsWith("8.4."));
            try(var r=s.executeQuery("select count(*) from information_schema.tables where table_schema=database()")) {r.next();assertEquals(0,r.getInt(1),"Only a fresh owned QA schema");}
            evidence.put("mysqlVersion",c.getMetaData().getDatabaseProductVersion());
        }
        var base=Flyway.configure().dataSource(url,"root",password).cleanDisabled(true).target("9").load();base.migrate();
        try(var c=DriverManager.getConnection(url,"root",password);var s=c.createStatement()) {
            s.executeUpdate("insert into users(id,name,email,password_hash,role,email_verified_at) values(900001,'QA Legacy','legacy-sentiment@example.invalid','!','CUSTOMER',CURRENT_TIMESTAMP)");
            s.executeUpdate("insert into categories(id,name,slug) values(900001,'QA Legacy','qa-legacy-sentiment')");
            s.executeUpdate("insert into stores(id,owner_user_id,name,status) values(900001,900001,'QA Legacy store','ACTIVE')");
            s.executeUpdate("insert into products(id,store_id,category_id,name,slug,price,stock,status) values(900001,900001,900001,'QA legacy product','qa-legacy-sentiment',10,1,'ACTIVE')");
            s.executeUpdate("insert into reviews(id,product_id,user_id,rating,comment) values(900001,900001,900001,1,'Legacy negative remains public')");
        }
        var current=Flyway.configure().dataSource(url,"root",password).cleanDisabled(true).load();current.migrate();current.validate();
        try(var c=DriverManager.getConnection(url,"root",password);var s=c.createStatement()) {
            try(var r=s.executeQuery("select comment,rating from reviews where id=900001")) {assertTrue(r.next());assertEquals("Legacy negative remains public",r.getString(1));assertEquals(1,r.getInt(2));}
            try(var r=s.executeQuery("select count(*) from review_sentiment")) {r.next();assertEquals(0,r.getInt(1),"No startup backfill");}
        }
        evidence.put("migration","V1-V9 then V10 with preserved legacy review; validate PASS; no backfill");
        props.add("spring.datasource.url",()->url);props.add("spring.datasource.username",()->"root");props.add("spring.datasource.password",()->password);
        props.add("sentiment.endpoint",()->"http://127.0.0.1:18091/predict");
        props.add("sentiment.internal-token",()->System.getenv("SENTIMENT_INTERNAL_TOKEN"));
    }

    @Override @BeforeAll void schema() {
        assertEquals(10,db.queryForObject("select max(cast(version as unsigned)) from flyway_schema_history where success=1",Integer.class));
    }

    @Test void buyerToBackgroundModelToSellerAdminBrowser() throws Exception {
        String browserPassword=UUID.randomUUID().toString();String hash=encoder.encode(browserPassword);
        for(var user:List.of(seller,admin,buyer,foreign)) db.update("update users set password_hash=? where id=?",hash,user.getId());
        var builder=new ProcessBuilder("node","tools/sentiment/browser-qa.mjs").redirectErrorStream(true)
            .redirectOutput(OUT.resolve("evidence/browser-console.log").toFile());
        var env=builder.environment();env.put("BM_LIVE_BACKEND_ORIGIN","http://127.0.0.1:"+port);
        env.put("BM_SENTIMENT_BUYER",buyer.getEmail());env.put("BM_SENTIMENT_SELLER",seller.getEmail());
        env.put("BM_SENTIMENT_ADMIN",admin.getEmail());env.put("BM_SENTIMENT_FOREIGN",foreign.getEmail());
        env.put("BM_SENTIMENT_BROWSER_PASSWORD",browserPassword);env.put("BM_SENTIMENT_PRODUCT",product.getId().toString());
        var process=builder.start();
        try {assertTrue(process.waitFor(180,TimeUnit.SECONDS),"Browser QA deadline");assertEquals(0,process.exitValue(),"Inspect browser-console.log and browser-results.json");}
        finally {if(process.isAlive())process.destroyForcibly();}
        var rows=db.queryForList("select r.rating,r.comment,s.status,s.label,s.model_version,s.content_version,s.attempts from reviews r join review_sentiment s on s.review_id=r.id where r.order_item_id=?",item.getId());
        assertEquals(1,rows.size());assertEquals("ANALYZED",rows.getFirst().get("status"));assertEquals("negative",rows.getFirst().get("label"));
        assertEquals("tfidf-logreg-b9427cf9a15a150a61a4561279fb7945a912f27dec174838f710bdb810eceb69",rows.getFirst().get("model_version"));
        assertFalse(worker.processOnce());
        evidence.put("realModelRows",rows);evidence.put("boundary","Actual local CPU model, MySQL 8.4, Spring random-port HTTP, browser; not production quality");
        Files.writeString(OUT.resolve("evidence/real-model-integration.json"),new tools.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(evidence));
    }
}
