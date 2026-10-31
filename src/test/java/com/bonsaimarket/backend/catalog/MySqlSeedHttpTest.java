package com.bonsaimarket.backend.catalog;

import com.bonsaimarket.backend.support.OwnedMySql;
import com.bonsaimarket.backend.config.DatabaseMigrationConfiguration;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

/** Real HTTP + MySQL; isolated profile avoids importing the team's .env. */
@EnabledIfEnvironmentVariable(named="BM_SEED_MYSQL_URL",matches=".+")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.profiles.active=isolated-seed-test","server.address=127.0.0.1",
    "auth.frontend-origin=http://localhost:5179","auth.public-origin=http://localhost:5179",
    "spring.jpa.hibernate.ddl-auto=validate","server.servlet.session.cookie.secure=false",
    "orders.mail.enabled=false","shipping.ghn.enabled=false","vnpay.enabled=false","ai.enabled=false"})
@Import(MySqlSeedHttpTest.SeedPolicy.class)
class MySqlSeedHttpTest {
    @TestConfiguration(proxyBeanMethods=false) static class SeedPolicy {
        @Bean FlywayConfigurationCustomizer isolatedDevPolicy() {
            var environment=new MockEnvironment();environment.setActiveProfiles("dev");
            return new DatabaseMigrationConfiguration().databaseMigrationPolicy(environment);
        }
    }
    @LocalServerPort int port;
    @Autowired JdbcTemplate db;
    static final ObjectMapper JSON=new ObjectMapper();
    static Stream<String> accounts(){return Stream.of("admin","seller","user","storefront-v1-vuon-hien",
        "storefront-v1-nang-som","storefront-v1-goc-la","storefront-v1-moc-vien","storefront-v1-nha-chau",
        "discussion-v1-lan","discussion-v1-minh","discussion-v1-an");}
    @DynamicPropertySource static void database(DynamicPropertyRegistry p)throws Exception {
        String url=System.getenv("BM_SEED_MYSQL_URL"),secret=System.getenv("BM_PARITY_PASSWORD");
        OwnedMySql.empty(url,secret);
        p.add("spring.datasource.url",()->url);p.add("spring.datasource.username",()->"root");p.add("spring.datasource.password",()->secret);
        p.add("spring.datasource.driver-class-name",()->"com.mysql.cj.jdbc.Driver");
    }
    class SessionClient {
        final CookieManager cookies=new CookieManager(null,CookiePolicy.ACCEPT_ALL);
        final HttpClient http=HttpClient.newBuilder().cookieHandler(cookies).build();
        HttpResponse<String> request(String method,String path,Object body,boolean csrf)throws Exception {
            var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path));
            if(csrf){var token=JSON.readTree(request("GET","/api/auth/csrf",null,false).body());request.header(token.path("headerName").asText(),token.path("token").asText());}
            if(body==null)request.method(method,HttpRequest.BodyPublishers.noBody());
            else request.header("Content-Type","application/json").method(method,HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
            return http.send(request.build(),HttpResponse.BodyHandlers.ofString());
        }
        HttpResponse<String> get(String path)throws Exception{return request("GET",path,null,false);}
        HttpResponse<String> post(String path,Object body)throws Exception{return request("POST",path,body,true);}
        String session(){return cookies.getCookieStore().getCookies().stream().filter(c->c.getName().equals("JSESSIONID")).map(HttpCookie::getValue).findFirst().orElse("");}
    }
    @ParameterizedTest(name="{0}@bonsai.market: login, role, ownership, CSRF, logout") @MethodSource("accounts")
    void everyDemoAccountUsesRealSecurity(String local)throws Exception {
        String email=local+"@bonsai.market",role=local.equals("admin")?"admin":local.equals("seller")||local.startsWith("storefront-")?"seller":"customer";
        var row=db.queryForMap("select id,password_hash,role,is_active,email_verified_at from users where email=?",email);
        assertEquals(role.toUpperCase(Locale.ROOT),row.get("role"));assertEquals(true,row.get("is_active"));assertNotNull(row.get("email_verified_at"));
        assertTrue(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().matches("123456",row.get("password_hash").toString()));
        var client=new SessionClient();
        assertEquals(403,client.request("POST","/api/auth/login",Map.of("email",email,"password","123456"),false).statusCode());
        assertEquals(401,client.post("/api/auth/login",Map.of("email",email,"password","wrong-password")).statusCode());
        assertEquals(401,client.get("/api/auth/me").statusCode());String before=client.session();
        var login=client.post("/api/auth/login",Map.of("email",email,"password","123456"));assertEquals(200,login.statusCode(),email+login.body());
        assertEquals(email,JSON.readTree(login.body()).path("email").asText());assertEquals(role,JSON.readTree(login.body()).path("role").asText());
        assertNotEquals(before,client.session(),"Session fixation protection");String loggedIn=client.session();
        assertEquals(email,JSON.readTree(client.get("/api/auth/me").body()).path("email").asText());
        assertEquals(200,client.get("/api/account/profile").statusCode());
        var accountOrders=client.get("/api/account/orders");assertEquals(200,accountOrders.statusCode());
        if(local.equals("user"))assertTrue(accountOrders.body().contains("BSM-DEV-"));
        else assertFalse(accountOrders.body().contains("BSM-DEV-"),"Buyer history is scoped to the session actor");
        var adminOrders=client.get("/api/admin/orders");assertEquals(role.equals("admin")?200:403,adminOrders.statusCode());
        if(role.equals("admin"))assertTrue(adminOrders.body().contains("BSM-DEV-"));
        var sellerOrders=client.get("/api/seller/orders");assertEquals(role.equals("customer")?403:200,sellerOrders.statusCode());
        if(local.equals("seller"))assertTrue(sellerOrders.body().contains("BSM-DEV-"));
        if(local.startsWith("storefront-"))assertFalse(sellerOrders.body().contains("BSM-DEV-"));
        var catalog=client.get("/api/products");assertEquals(200,catalog.statusCode());
        assertTrue(catalog.body().contains("tung-la-han")||catalog.body().contains("demo-storefront"));
        Long order=db.queryForObject("select min(id) from orders",Long.class);
        if(!local.equals("user"))assertEquals(404,client.get("/api/account/orders/"+order).statusCode(),"Other buyer's order");
        if(role.equals("seller")) {
            Long store=db.queryForObject("select id from stores where owner_user_id=?",Long.class,row.get("id"));
            assertEquals(store.longValue(),JSON.readTree(client.get("/api/seller/store").body()).path("id").asLong());
            Long ownProduct=db.queryForObject("select min(id) from products where store_id=?",Long.class,store);
            assertEquals(200,client.get("/api/seller/products/"+ownProduct+"/content").statusCode());
            for(Long foreignProduct:db.queryForList("select min(id) from products where store_id<>? group by store_id",Long.class,store))
                assertEquals(404,client.get("/api/seller/products/"+foreignProduct+"/content").statusCode(),"Other seller's product");
            if(!local.equals("seller"))assertEquals(404,client.get("/api/seller/orders/"+order).statusCode());
        }
        if(!role.equals("admin")) {
            Long foreignPost=db.queryForObject("select min(id) from community_posts where author_id<>?",Long.class,row.get("id"));
            assertEquals(403,client.request("DELETE","/api/account/community/posts/"+foreignPost+"?revision=0",null,true).statusCode());
        }
        assertEquals(400,client.post("/api/auth/change-password",Map.of("currentPassword","123456","newPassword","123456","confirmPassword","123456")).statusCode());
        assertEquals(403,client.request("POST","/api/auth/logout",Map.of(),false).statusCode());
        assertEquals(200,client.get("/api/auth/me").statusCode());assertEquals(200,client.post("/api/auth/logout",Map.of()).statusCode());
        assertEquals(401,client.get("/api/auth/me").statusCode());
        var stale=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/auth/me")).header("Cookie","JSESSIONID="+loggedIn).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(401,stale.statusCode());
        var names=accounts().toList();String next=names.get((names.indexOf(local)+1)%names.size())+"@bonsai.market";
        assertEquals(200,client.post("/api/auth/login",Map.of("email",next,"password","123456")).statusCode());
        assertEquals(next,JSON.readTree(client.get("/api/auth/me").body()).path("email").asText());
        assertEquals(next.startsWith("admin@")?200:403,client.get("/api/admin/orders").statusCode());
        assertEquals(200,client.post("/api/auth/logout",Map.of()).statusCode());
    }
    @Test void browserUsesSeededLoginAndRoleRoutes()throws Exception {
        var process=new ProcessBuilder("node","frontend/qa/live-integration-browser.js").redirectErrorStream(true);
        process.environment().put("BM_LIVE_BACKEND_ORIGIN","http://127.0.0.1:"+port);process.environment().put("BM_DEMO_SEED","true");
        var browserOutput=java.nio.file.Path.of(System.getenv("BM_TEST_OUTPUT"),"seed-browser");java.nio.file.Files.createDirectories(browserOutput);
        process.environment().put("BM_TEST_OUTPUT",browserOutput.toString());
        var output=java.nio.file.Path.of(System.getenv("BM_TEST_OUTPUT"),"demo-browser.log");process.redirectOutput(output.toFile());
        var child=process.start();
        try {assertTrue(child.waitFor(180,java.util.concurrent.TimeUnit.SECONDS));assertEquals(0,child.exitValue(),"See "+output);}
        finally{if(child.isAlive())child.destroyForcibly();}
    }
}
