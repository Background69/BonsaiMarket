package com.bonsaimarket.backend.catalog;

import com.bonsaimarket.backend.support.OwnedMySql;
import java.net.*;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

/** HTTP thật và Hibernate validate trên seed mới; profile riêng tránh đọc .env nhóm. */
@EnabledIfEnvironmentVariable(named="BM_SEED_MYSQL_URL",matches=".+")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.profiles.active=isolated-seed-test","server.address=127.0.0.1",
    "spring.flyway.locations=classpath:db/migration,classpath:db/dev",
    "spring.jpa.hibernate.ddl-auto=validate","server.servlet.session.cookie.secure=false",
    "orders.mail.enabled=false","shipping.ghn.enabled=false","vnpay.enabled=false","ai.enabled=false"})
class MySqlSeedHttpTest {
    @LocalServerPort int port;
    static final ObjectMapper JSON=new ObjectMapper();
    @DynamicPropertySource static void database(DynamicPropertyRegistry p)throws Exception {
        String url=System.getenv("BM_SEED_MYSQL_URL"),secret=System.getenv("BM_PARITY_PASSWORD");
        OwnedMySql.empty(url,secret);
        String hash=new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(System.getenv("BM_CHECKOUT_TEST_PASSWORD"));
        p.add("spring.datasource.url",()->url);p.add("spring.datasource.username",()->"root");p.add("spring.datasource.password",()->secret);
        p.add("spring.datasource.driver-class-name",()->"com.mysql.cj.jdbc.Driver");
        p.add("spring.flyway.placeholders.devSeedPasswordHash",()->hash);
    }
    @Test void seededRolesLoginAndReadRealCatalogAndOrders()throws Exception {
        for(String role:List.of("admin","seller","user")) {
            var http=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
            String origin="http://127.0.0.1:"+port;
            var csrf=http.send(HttpRequest.newBuilder(URI.create(origin+"/api/auth/csrf")).GET().build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(200,csrf.statusCode());var token=JSON.readTree(csrf.body());
            var login=http.send(HttpRequest.newBuilder(URI.create(origin+"/api/auth/login")).header("Content-Type","application/json")
                .header(token.path("headerName").asText(),token.path("token").asText())
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(Map.of("email",role+"@example.invalid","password",System.getenv("BM_CHECKOUT_TEST_PASSWORD"))))).build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(200,login.statusCode(),role+" đăng nhập bằng seed");
            String endpoint=role.equals("admin")?"/api/admin/orders":role.equals("seller")?"/api/seller/orders":"/api/account/orders";
            var orders=http.send(HttpRequest.newBuilder(URI.create(origin+endpoint)).GET().build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(200,orders.statusCode());assertTrue(orders.body().contains("BSM-DEV-"));
            var catalog=http.send(HttpRequest.newBuilder(URI.create(origin+"/api/products")).GET().build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(200,catalog.statusCode());assertTrue(catalog.body().contains("tung-la-han")||catalog.body().contains("demo-storefront"));
            if(role.equals("user"))assertEquals(403,http.send(HttpRequest.newBuilder(URI.create(origin+"/api/admin/orders")).GET().build(),HttpResponse.BodyHandlers.discarding()).statusCode());
        }
    }
}
