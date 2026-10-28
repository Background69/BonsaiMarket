package com.bonsaimarket.backend.checkout;

import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.net.*;
import java.net.http.*;
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

/** MySQL dùng riêng, HTTP/session/browser thật khi bật test; chỉ transport của hãng là giả lập. */
@EnabledIfEnvironmentVariable(named="BM_CHECKOUT_MYSQL_URL",matches=".+")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"server.address=127.0.0.1",
    "auth.frontend-origin=http://localhost:5179","auth.public-origin=http://localhost:5179","ai.enabled=false","orders.mail.enabled=false",
    "server.servlet.session.cookie.secure=false"})
class MySqlCheckoutHttpTest extends CheckoutShippingIntegrationTest {
    @LocalServerPort int port;@Autowired PasswordEncoder encoder;
    @DynamicPropertySource static void mysql(DynamicPropertyRegistry props) throws Exception {
        String url=System.getenv("BM_CHECKOUT_MYSQL_URL"),secret=System.getenv("BM_CHECKOUT_MYSQL_PASSWORD");
        assertTrue(url.matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/bm_checkout_[a-z0-9_]+\\?.*"));
        // Xác minh hai schema MySQL riêng, rỗng trước mọi thao tác ghi.
        com.bonsaimarket.backend.support.OwnedMySql.empty(url,secret);
        com.bonsaimarket.backend.support.OwnedMySql.empty(System.getenv("BM_CHECKOUT_MYSQL_CLEAN_URL"),secret);
        Flyway.configure().dataSource(url,"root",secret).locations("classpath:db/migration").load().migrate();
        try(var c=DriverManager.getConnection(url,"root",secret);var s=c.createStatement()){
            s.executeUpdate("insert into users(name,email,password_hash,role,is_active) values('Legacy preservation','legacy-checkout@example.invalid','!','CUSTOMER',true)");
            s.executeUpdate("insert into stores(owner_user_id,name,status) select id,'Legacy store','ACTIVE' from users where email='legacy-checkout@example.invalid'");
            s.executeUpdate("insert into orders(code,buyer_user_id,store_id,shipping_address_snapshot,shipping_method,shipping_fee,subtotal,total,status) select 'LEGACY-CHECKOUT',u.id,s.id,'LEGACY SNAPSHOT','LEGACY',7,100,107,'PENDING' from users u join stores s on s.owner_user_id=u.id where u.email='legacy-checkout@example.invalid'");
        }
        var flyway=Flyway.configure().dataSource(url,"root",secret).locations("classpath:db/migration").load();flyway.migrate();flyway.validate();
        try(var c=DriverManager.getConnection(url,"root",secret);var s=c.createStatement();var rs=s.executeQuery("select count(*) from users where email='legacy-checkout@example.invalid'")){assertTrue(rs.next());assertEquals(1,rs.getInt(1));}
        try(var c=DriverManager.getConnection(url,"root",secret);var s=c.createStatement();var rs=s.executeQuery("select o.total,o.shipping_fee,o.shipping_address_snapshot,(select count(*) from order_payments p where p.order_id=o.id) as payments from orders o where o.code='LEGACY-CHECKOUT'")){assertTrue(rs.next());assertEquals(107,rs.getInt(1));assertEquals(7,rs.getInt(2));assertEquals("LEGACY SNAPSHOT",rs.getString(3));assertEquals(0,rs.getInt(4));}
        String clean=System.getenv("BM_CHECKOUT_MYSQL_CLEAN_URL");assertTrue(clean.matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/bm_checkout_[a-z0-9_]+_clean\\?.*"));
        var fresh=Flyway.configure().dataSource(clean,"root",secret).locations("classpath:db/migration").load();fresh.migrate();fresh.validate();
        Files.writeString(evidence("mysql-flyway.json"),"{\"replayFrom\":1,\"latest\":1,\"cleanToLatest\":true,\"validate\":true,\"legacyOrderPreserved\":true,\"noLegacyPaymentBackfill\":true,\"boundary\":\"disposable MySQL\"}");
        props.add("spring.datasource.url",()->url);props.add("spring.datasource.username",()->"root");props.add("spring.datasource.password",()->secret);
        props.add("spring.datasource.driver-class-name",()->"com.mysql.cj.jdbc.Driver");props.add("spring.jpa.hibernate.ddl-auto",()->"validate");props.add("spring.flyway.enabled",()->false);
    }
    @Test void fullBrowserCheckoutAndSellerFulfillmentWithoutInterception() throws Exception {
        String password=System.getenv("BM_CHECKOUT_TEST_PASSWORD"),hash=encoder.encode(password);
        db.update("update users set password_hash=? where id in (?,?,?)",hash,buyer.getId(),seller.getId(),admin.getId());
        addresses.create(buyer.getId(),delivery());
        // Mua ngay từ UI phải giữ nguyên dòng hàng đang có trong giỏ.
        var process=new ProcessBuilder("node","frontend/qa/checkout-browser.js").redirectErrorStream(true).redirectOutput(evidence("browser.log").toFile());
        var env=process.environment();env.put("BM_LIVE_BACKEND_ORIGIN","http://127.0.0.1:"+port);env.put("BM_CHECKOUT_BUYER",buyer.getEmail());env.put("BM_CHECKOUT_SELLER",seller.getEmail());env.put("BM_CHECKOUT_PRODUCT",product.getId().toString());
        var child=process.start();try{assertTrue(child.waitFor(180,TimeUnit.SECONDS),"Browser deadline");assertEquals(0,child.exitValue(),"See browser.log");}finally{if(child.isAlive())child.destroyForcibly();}
        assertEquals(9,stock());assertEquals(1,cart.view(buyer.getId()).quantity());
        var row=db.queryForMap("select o.id,o.status,p.status as payment_status,p.cod_amount,p.settlement_status from orders o join order_payments p on p.order_id=o.id where o.buyer_user_id=?",buyer.getId());
        assertEquals("CONFIRMED",row.get("status"));assertEquals("AWAITING_COLLECTION",row.get("payment_status"));assertEquals("UNKNOWN",row.get("settlement_status"));
    }
    static Path evidence(String file){return Path.of(System.getenv().getOrDefault("BM_CHECKOUT_OUTPUT","artifacts/checkout-20261026"),file);}
    final class SessionClient {
        final HttpClient http=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        HttpResponse<String> get(String path)throws Exception{return http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).GET().build(),HttpResponse.BodyHandlers.ofString());}
        HttpResponse<String> post(String path,Object body)throws Exception{var c=json.readTree(get("/api/auth/csrf").body());return http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).header("Content-Type","application/json").header(c.path("headerName").asText(),c.path("token").asText()).POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());}
        void login(String email)throws Exception{assertEquals(200,post("/api/auth/login",Map.of("email",email,"password",System.getenv("BM_CHECKOUT_TEST_PASSWORD"))).statusCode());}
    }
    @Test void realHttpReturnAndCodReportBoundaries() throws Exception {
        var password=encoder.encode(System.getenv("BM_CHECKOUT_TEST_PASSWORD"));db.update("update users set password_hash=? where id in (?,?)",password,buyer.getId(),seller.getId());
        var r=checkout.place(buyer.getId(),body(quote(buyer)));management.transition(seller.getId(),false,r.orderId(),"CONFIRMED");management.transition(seller.getId(),false,r.orderId(),"SHIPPING");management.transition(seller.getId(),false,r.orderId(),"DELIVERED");
        var buyerHttp=new SessionClient();buyerHttp.login(buyer.getEmail());var sellerHttp=new SessionClient();sellerHttp.login(seller.getEmail());
        assertEquals(200,buyerHttp.post("/api/account/orders/"+r.orderId()+"/return",Map.of("reason","Yêu cầu trả từ HTTP MySQL cô lập")).statusCode());assertEquals(9,stock());
        assertEquals(200,sellerHttp.post("/api/seller/orders/"+r.orderId()+"/return/approve",Map.of()).statusCode());assertEquals(9,stock());
        assertEquals(200,sellerHttp.post("/api/seller/orders/"+r.orderId()+"/return/inspection",Map.of("received",true,"condition","Đã thực nhận toàn bộ hàng và kiểm tra đạt", "restock",true)).statusCode());assertEquals(10,stock());
        assertEquals("INSPECTED",json.readTree(buyerHttp.get("/api/account/orders/"+r.orderId()+"/return").body()).path("request").path("status").asText());
        cart.add(buyer.getId(),product.getId(),1);var shipped=checkout.place(buyer.getId(),body(quote(buyer)));management.transition(seller.getId(),false,shipped.orderId(),"CONFIRMED");shipping.create(seller.getId(),shipped.orderId());
        settings.setWebhookSecret("isolated-cod-callback-secret-32-characters");var callback=new LinkedHashMap<String,Object>();callback.put("ShopID",shop);callback.put("OrderCode",db.queryForObject("select tracking_code from shipments where order_id=?",String.class,shipped.orderId()));callback.put("ClientOrderCode",db.queryForObject("select client_reference from shipments where order_id=?",String.class,shipped.orderId()));callback.put("Type","cod");callback.put("Status","delivered");callback.put("Time",java.time.Instant.now().toString());callback.put("CODTransferDate",java.time.Instant.now().toString());callback.put("CODAmount",100000);callback.put("PaymentType",2);callback.put("TotalFee",24000);
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/shipping/webhooks/ghn")).header("Content-Type","application/json").header("X-GHN-Webhook-Secret",settings.getWebhookSecret()).POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(callback))).build();var client=HttpClient.newHttpClient();assertEquals(204,client.send(request,HttpResponse.BodyHandlers.discarding()).statusCode());assertEquals(204,client.send(request,HttpResponse.BodyHandlers.discarding()).statusCode());
        assertEquals(1,db.queryForObject("select count(*) from cod_provider_reports where order_id=?",Integer.class,shipped.orderId()));assertEquals("PENDING_RECONCILIATION",db.queryForObject("select settlement_status from order_payments where order_id=?",String.class,shipped.orderId()));assertNull(db.queryForObject("select settled_amount from order_payments where order_id=?",java.math.BigDecimal.class,shipped.orderId()));
    }
}
