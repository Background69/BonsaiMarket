package com.bonsaimarket.backend.management;

import java.net.*;
import java.net.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in, real MySQL8.4 + embedded HTTP + JavaMail TCP. Never targets the team's database. */
@EnabledIfEnvironmentVariable(named="BM_LIVE_MYSQL_URL",matches=".+")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.profiles.active=mysql-live-verification","server.address=127.0.0.1",
    "spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=validate","ai.enabled=false",
    "server.servlet.session.cookie.secure=false","spring.mail.host=127.0.0.1","spring.mail.username=",
    "spring.mail.password=","spring.mail.properties.mail.smtp.auth=false",
    "spring.mail.properties.mail.smtp.starttls.enable=false","spring.mail.properties.mail.smtp.starttls.required=false",
    "auth.mail-from=integration@example.invalid","auth.frontend-origin=http://localhost:5179",
    "auth.public-origin=http://localhost:5179","auth.google-client-id=isolated-test-id",
    "auth.google-client-secret=isolated-test-secret","auth.facebook-client-id=isolated-test-id",
    "auth.facebook-client-secret=isolated-test-secret","auth.facebook-api-version=v99.0"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LiveMySqlIntegrationTest {
    static final ObjectMapper JSON=new ObjectMapper();
    static final Path OUT=Path.of(System.getenv().getOrDefault("BM_TEST_OUTPUT","artifacts/live-integration-20261025"));
    static final Map<String,Object> EVIDENCE=new LinkedHashMap<>();
    static SmtpSink smtp;
    static String password,url,dbPassword;
    @LocalServerPort int port;
    @Autowired JdbcTemplate db;
    @DynamicPropertySource static void database(DynamicPropertyRegistry props) throws Exception {
        url=System.getenv("BM_LIVE_MYSQL_URL");String clean=System.getenv("BM_LIVE_MYSQL_CLEAN_URL");
        password=System.getenv("BM_LIVE_TEST_PASSWORD");dbPassword=System.getenv("BM_LIVE_MYSQL_PASSWORD");
        assertNotNull(password);assertNotNull(dbPassword);
        for(String candidate:List.of(url,clean)) {
            assertTrue(candidate.matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/bm_live_(upgrade|clean)_[a-z0-9_]+\\?.*"),"Only owned localhost bm_live schemas");
            try(var c=DriverManager.getConnection(candidate,"root",dbPassword);var s=c.createStatement()) {
                assertTrue(c.getMetaData().getDatabaseProductVersion().startsWith("8.4."),"Requires MySQL8.4");
                try(var r=s.executeQuery("select count(*) from information_schema.tables where table_schema=database()")) {r.next();assertEquals(0,r.getInt(1),"Refuse nonempty schema");}
                EVIDENCE.put("mysqlVersion",c.getMetaData().getDatabaseProductVersion());
            }
        }
        flyway(clean,null).migrate();flyway(clean,null).validate();
        flyway(url,"16").migrate();
        try(var c=DriverManager.getConnection(url,"root",dbPassword);var s=c.createStatement()) {
            String hash=new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(12).encode(password);
            try(var insert=c.prepareStatement("insert into users(id,name,email,password_hash,role,email_verified_at) values(?,?,?,?,?,CURRENT_TIMESTAMP)")) {
                String[] roles={"ADMIN","SELLER","CUSTOMER","CUSTOMER","SELLER"};
                for(int i=0;i<roles.length;i++){insert.setInt(1,i+1);insert.setString(2,"Integration "+roles[i]+" "+(i+1));insert.setString(3,"live"+(i+1)+"@example.invalid");insert.setString(4,hash);insert.setString(5,roles[i]);insert.executeUpdate();}
            }
            s.executeUpdate("insert into categories(id,name,slug) values(1,'Live bonsai','live-bonsai')");
            s.executeUpdate("insert into stores(id,owner_user_id,name,status) values(1,2,'Integration garden','ACTIVE')");
            s.executeUpdate("insert into products(id,store_id,category_id,name,slug,price,stock,status) values(1,1,1,'Live test bonsai','live-test-bonsai',90000,30,'ACTIVE'),(2,1,1,'Legacy second product','legacy-second',90000,10,'ACTIVE')");
            s.executeUpdate("insert into orders(id,code,buyer_user_id,store_id,shipping_address_snapshot,shipping_method,shipping_fee,subtotal,total,status) values(1,'LIVE-COMPLETED',3,1,'Test address','STANDARD',0,270000,270000,'COMPLETED')");
            s.executeUpdate("insert into order_items(id,order_id,product_id,product_name_snapshot,unit_price,quantity,line_total) values(1,1,1,'Live test bonsai',90000,1,90000),(2,1,1,'Live test bonsai',90000,1,90000),(3,1,1,'Live test bonsai',90000,1,90000)");
            s.executeUpdate("insert into reviews(id,product_id,user_id,rating,comment,created_at,updated_at) values(1,1,3,4,'Legacy review preserved','2026-01-01 01:02:03','2026-01-01 01:02:03'),(2,2,3,3,'Legacy second preserved','2026-01-02 01:02:03','2026-01-02 01:02:03')");
            s.executeUpdate("insert into product_questions(id,product_id,author_id,body,answer_author_id,answer_body,answered_at,created_at,updated_at) values(1,1,3,'Legacy question',2,'Legacy official answer','2026-01-02 01:02:03','2026-01-01 01:02:03','2026-01-02 01:02:03')");
        }
        List<String> before=snapshot(url);
        flyway(url,null).migrate();flyway(url,null).validate();
        assertEquals(before,snapshot(url),"V18 preserves every legacy review/question field");
        EVIDENCE.put("migration",Map.of("cleanLatest",flyway(clean,null).info().current().getVersion().toString(),"upgradeFrom","16","upgradeTo",flyway(url,null).info().current().getVersion().toString(),"legacySnapshotPreserved",true));
        smtp=new SmtpSink();
        props.add("spring.datasource.url",()->url);props.add("spring.datasource.username",()->"root");props.add("spring.datasource.password",()->dbPassword);
        props.add("spring.mail.port",()->smtp.server.getLocalPort());
        props.add("auth.otp-key",()->Base64.getEncoder().encodeToString(new byte[32]));
    }
    static Flyway flyway(String db,String target) {
        var cfg=Flyway.configure().dataSource(db,"root",dbPassword).locations("filesystem:src/main/resources/db/migration").cleanDisabled(true);
        if(target!=null)cfg.target(target);return cfg.load();
    }
    static List<String> snapshot(String db) throws Exception {
        var result=new ArrayList<String>();try(var c=DriverManager.getConnection(db,"root",dbPassword);var s=c.createStatement()) {
            for(String q:List.of("select id,product_id,user_id,rating,comment,created_at,updated_at from reviews order by id","select id,product_id,author_id,body,status,answer_author_id,answer_body,answered_at,created_at,updated_at,revision from product_questions order by id"))
                try(var r=s.executeQuery(q)){while(r.next()){var row=new ArrayList<String>();for(int i=1;i<=r.getMetaData().getColumnCount();i++)row.add(Objects.toString(r.getObject(i)));result.add(row.toString());}}
        }return result;
    }
    record Response(int status,JsonNode json,HttpHeaders headers) {}
    class Client {
        CookieManager cookies=new CookieManager(null,CookiePolicy.ACCEPT_ALL);
        HttpClient http=HttpClient.newBuilder().cookieHandler(cookies).followRedirects(HttpClient.Redirect.NEVER).connectTimeout(Duration.ofSeconds(5)).build();
        String token,header;
        Response call(String method,String path,Object body,boolean csrf) throws Exception {
            if(csrf&&token==null){var r=call("GET","/api/auth/csrf",null,false);assertEquals(200,r.status);token=r.json.get("token").asText();header=r.json.get("headerName").asText();}
            var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(20)).header("Accept","application/json");
            if(csrf)builder.header(header,token);
            if(body!=null)builder.header("Content-Type","application/json");
            builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
            var r=http.send(builder.build(),HttpResponse.BodyHandlers.ofString());JsonNode node;
            try{node=JSON.readTree(r.body());}catch(Exception ignored){node=JSON.createObjectNode();}
            return new Response(r.statusCode(),node,r.headers());
        }
        Response get(String p)throws Exception{return call("GET",p,null,false);}
        Response post(String p,Object b)throws Exception{return call("POST",p,b,true);}
        void login(int id)throws Exception{assertEquals(200,post("/api/auth/login",Map.of("email","live"+id+"@example.invalid","password",password)).status);token=null;}
        String session(){return cookies.getCookieStore().getCookies().stream().filter(c->c.getName().equals("JSESSIONID")).map(HttpCookie::getValue).findFirst().orElse("");}
    }
    @Test @org.junit.jupiter.api.Order(1) void mysqlConstraintsAndLegacyData() throws Exception {
        assertEquals(2,db.queryForObject("select count(*) from reviews where order_item_id is null",Integer.class));
        var constraints=db.queryForList("select CONSTRAINT_NAME,CONSTRAINT_TYPE from information_schema.table_constraints where constraint_schema=database() and table_name in ('reviews','product_question_replies')");
        var indexes=db.queryForList("select table_name,index_name,non_unique,column_name from information_schema.statistics where table_schema=database() and table_name in ('reviews','product_question_replies') order by table_name,index_name,seq_in_index");
        EVIDENCE.put("constraints",constraints);EVIDENCE.put("indexes",indexes);
        db.update("insert into reviews(product_id,user_id,order_item_id,rating,comment) values(1,3,1,5,'Verified test record')");
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->db.update("insert into reviews(product_id,user_id,order_item_id,rating) values(1,3,1,5)"));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->db.update("insert into reviews(product_id,user_id,order_item_id,rating) values(1,3,999999,5)"));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->db.update("insert into product_question_replies(question_id,author_id,body) values(999999,3,'Wrong parent')"));
        var check=assertThrows(org.springframework.dao.DataAccessException.class,()->db.update("insert into product_question_replies(question_id,author_id,body) values(1,3,'')"));
        assertInstanceOf(SQLException.class,check.getMostSpecificCause());assertEquals(3819,((SQLException)check.getMostSpecificCause()).getErrorCode());
    }
    @Test @org.junit.jupiter.api.Order(2) void authOverHttpAndRealJavaMailTcp() throws Exception {
        var client=new Client();String email="registered@example.invalid";
        client.get("/api/auth/csrf");String oldSession=client.session();
        assertEquals(201,client.post("/api/auth/register",Map.of("name","Live registered buyer","email",email,"password",password,"confirmPassword",password,"role","ADMIN")).status);
        String otp=smtp.code(email,1);String context=client.get("/api/auth/verification-context").json.get("contextId").asText();
        assertEquals(429,client.post("/api/auth/resend-verification",Map.of("contextId",context)).status);
        var verified=client.post("/api/auth/verify-email",Map.of("contextId",context,"code",otp));assertEquals(200,verified.status);assertEquals("customer",verified.json.get("role").asText());
        assertTrue(!oldSession.equals(client.session()),"Session identifier rotates after verification");client.token=null;
        var replay=client.post("/api/auth/verify-email",Map.of("contextId",context,"code",otp));assertEquals(409,replay.status);assertEquals("VERIFICATION_CONTEXT_EXPIRED",replay.json.get("code").asText());
        assertEquals(200,client.get("/api/auth/me").status);
        assertEquals(403,client.call("POST","/api/auth/logout",Map.of(),false).status);
        assertEquals(200,client.post("/api/auth/logout",Map.of()).status);client.token=null;assertEquals(401,client.get("/api/auth/me").status);
        assertEquals(401,client.post("/api/auth/login",Map.of("email",email,"password","IncorrectPassword999")).status);
        assertEquals(200,client.post("/api/auth/login",Map.of("email",email,"password",password)).status);client.token=null;
        var oldAuthenticated=new Client();assertEquals(200,oldAuthenticated.post("/api/auth/login",Map.of("email",email,"password",password)).status);
        var reset=new Client();String flow=UUID.randomUUID().toString();
        var known=reset.post("/api/auth/forgot-password",Map.of("email",email,"flowId",flow));assertEquals(202,known.status);
        String resetCode=smtp.code(email,2);
        var unknown=new Client().post("/api/auth/forgot-password",Map.of("email","unknown@example.invalid","flowId",UUID.randomUUID().toString()));assertEquals(known.json.get("message").asText(),unknown.json.get("message").asText());
        var unproved=reset.post("/api/auth/reset-password",Map.of("password",password+"X","confirmPassword",password+"X","flowId",flow));assertEquals(409,unproved.status);assertEquals("RESET_AUTHORIZATION_EXPIRED",unproved.json.get("code").asText());
        // A failed unproved reset invalidates the session flow, not the unused challenge.
        // Reopening the wizard during SMTP cooldown uses that same still-unused OTP.
        flow=UUID.randomUUID().toString();
        assertEquals(202,reset.post("/api/auth/forgot-password",Map.of("email",email,"flowId",flow)).status);
        assertEquals(200,reset.post("/api/auth/verify-reset-code",Map.of("code",resetCode,"flowId",flow)).status);
        assertEquals(200,reset.post("/api/auth/reset-password",Map.of("password",password+"X","confirmPassword",password+"X","flowId",flow)).status);reset.token=null;
        assertEquals(401,oldAuthenticated.get("/api/auth/me").status);
        assertEquals(200,reset.post("/api/auth/login",Map.of("email",email,"password",password+"X")).status);
        var buyer=new Client();buyer.login(3);assertEquals(403,buyer.get("/api/admin/orders").status);assertEquals(403,buyer.get("/api/seller/orders").status);
        var seller=new Client();seller.login(2);assertEquals(403,seller.get("/api/admin/orders").status);
        var admin=new Client();admin.login(1);assertEquals(200,admin.get("/api/admin/orders").status);
        assertTrue(client.cookies.getCookieStore().getCookies().stream().filter(c->c.getName().equals("JSESSIONID")).allMatch(HttpCookie::isHttpOnly));
        EVIDENCE.put("smtp","Real JavaMailSender over TCP to task-owned SMTP sink; external teammate SMTP NOT VERIFIED");
    }
    long order(String status,int quantity) {
        String code="LIVE-"+UUID.randomUUID().toString().replace("-","");db.update("insert into orders(code,buyer_user_id,store_id,shipping_address_snapshot,shipping_method,shipping_fee,subtotal,total,status) values(?,3,1,'Test address','STANDARD',0,?,?,?)",code,90000L*quantity,90000L*quantity,status);
        long id=db.queryForObject("select id from orders where code=?",Long.class,code);
        db.update("insert into order_items(order_id,product_id,product_name_snapshot,unit_price,quantity,line_total) values(?,1,'Live test bonsai',90000,?,?)",id,quantity,90000L*quantity);return id;
    }
    Response transition(Client actor,long id,String status)throws Exception{return actor.post("/api/seller/orders/"+id+"/status",Map.of("status",status));}
    int stock(){return db.queryForObject("select stock from products where id=1",Integer.class);}
    @Test @org.junit.jupiter.api.Order(3) void ordersOwnershipStateInventoryAndConcurrencyOverHttp() throws Exception {
        var buyer=new Client();buyer.login(3);var seller=new Client();seller.login(2);var other=new Client();other.login(4);var otherSeller=new Client();otherSeller.login(5);var admin=new Client();admin.login(1);
        int before=stock(),mailCount=smtp.total();long id=order("PENDING",2);
        var stored=db.queryForObject("select created_at from orders where id=?",java.time.LocalDateTime.class,id);
        var published=java.time.OffsetDateTime.parse(buyer.get("/api/account/orders/"+id).json.get("createdAt").asText()).toLocalDateTime();
        EVIDENCE.put("orderTimestamp",Map.of("storedUtc",stored.toString(),"publishedUtc",published.toString()));
        EVIDENCE.put("mysqlTimezone",db.queryForMap("select @@session.time_zone sessionZone,@@system_time_zone systemZone,UTC_TIMESTAMP(6) utcNow,NOW(6) databaseNow"));
        assertEquals(stored,published,"Order API must preserve the persisted UTC timestamp");
        assertEquals(404,other.post("/api/account/orders/"+id+"/cancel",Map.of()).status);assertEquals(404,transition(otherSeller,id,"CONFIRMED").status);
        assertEquals(409,transition(seller,id,"SHIPPING").status);assertEquals(200,transition(seller,id,"CONFIRMED").status);assertEquals(before-2,stock());
        assertEquals(409,transition(seller,id,"CONFIRMED").status);assertEquals(before-2,stock());
        assertEquals(200,transition(seller,id,"SHIPPING").status);assertEquals(409,buyer.post("/api/account/orders/"+id+"/cancel",Map.of()).status);
        assertEquals(200,transition(seller,id,"DELIVERED").status);assertEquals(409,transition(seller,id,"COMPLETED").status);
        assertEquals(200,admin.post("/api/admin/orders/"+id+"/status",Map.of("status","COMPLETED")).status);
        long race=order("PENDING",2);int initial=stock();buyer.get("/api/auth/csrf");seller.get("/api/auth/csrf");
        var pool=Executors.newFixedThreadPool(2);try {
            var gate=new CountDownLatch(1);
            var a=pool.submit(()->{gate.await();return buyer.post("/api/account/orders/"+race+"/cancel",Map.of()).status;});
            var b=pool.submit(()->{gate.await();return transition(seller,race,"CONFIRMED").status;});gate.countDown();
            assertEquals(200,a.get(25,TimeUnit.SECONDS));assertTrue(Set.of(200,409).contains(b.get(25,TimeUnit.SECONDS)));
        }finally{pool.shutdownNow();}
        assertEquals("CANCELLED",db.queryForObject("select status from orders where id=?",String.class,race));assertEquals(initial,stock());
        assertEquals(409,buyer.post("/api/account/orders/"+race+"/cancel",Map.of()).status);assertEquals(initial,stock());
        assertEquals(0,db.queryForObject("select count(*) from inventory_debits where order_id=? and restored=false",Integer.class,race));
        long insufficient=order("PENDING",1000);assertEquals(409,transition(seller,insufficient,"CONFIRMED").status);assertEquals("PENDING",db.queryForObject("select status from orders where id=?",String.class,insufficient));assertEquals(initial,stock());
        assertEquals(mailCount,smtp.total(),"Cancellation does not claim or send unsupported notification");
    }
    @Test @org.junit.jupiter.api.Order(4) void reviewsEligibilityUniquenessAndAggregationOverHttp() throws Exception {
        var buyer=new Client();buyer.login(3);var other=new Client();other.login(4);String endpoint="/api/account/products/1/reviews";
        var payload=Map.of("orderItemId",2,"rating",5,"comment","MySQL HTTP review","userId",4);
        assertEquals(404,other.post(endpoint,payload).status);assertEquals(404,buyer.post("/api/account/products/2/reviews",payload).status);
        assertEquals(400,buyer.post(endpoint,Map.of("orderItemId",2,"rating",6,"comment","Invalid")).status);
        var pool=Executors.newFixedThreadPool(2);var second=new Client();second.login(3);
        try{var gate=new CountDownLatch(1);var a=pool.submit(()->{gate.await();return buyer.post(endpoint,payload).status;});var b=pool.submit(()->{gate.await();return second.post(endpoint,payload).status;});gate.countDown();var outcomes=List.of(a.get(25,TimeUnit.SECONDS),b.get(25,TimeUnit.SECONDS));assertEquals(1,Collections.frequency(outcomes,201));assertEquals(1,Collections.frequency(outcomes,409));}finally{pool.shutdownNow();}
        assertEquals(1,db.queryForObject("select count(*) from reviews where order_item_id=2",Integer.class));
        assertEquals(3L,db.queryForObject("select user_id from reviews where order_item_id=2",Long.class));
        var listing=buyer.get("/api/products/1/reviews");assertEquals(200,listing.status);assertEquals(3,listing.json.get("ratingCount").asInt());
        assertTrue(Math.abs(listing.json.get("averageRating").asDouble()-14.0/3)<0.00001);
        assertEquals(1,java.util.stream.StreamSupport.stream(listing.json.get("reviews").get("content").spliterator(),false).filter(n->!n.get("verifiedPurchase").asBoolean()).count());
        var eligible=buyer.get("/api/account/products/1/review-eligibility");assertEquals(200,eligible.status);
        int expected=db.queryForObject("select count(*) from order_items i join orders o on o.id=i.order_id where i.product_id=1 and o.buyer_user_id=3 and o.status='COMPLETED' and not exists(select 1 from reviews r where r.order_item_id=i.id)",Integer.class);
        assertEquals(expected,eligible.json.get("purchases").size());
        var eligibleIds=java.util.stream.StreamSupport.stream(eligible.json.get("purchases").spliterator(),false).map(n->n.get("orderItemId").asInt()).toList();
        assertTrue(eligibleIds.contains(3));assertFalse(eligibleIds.contains(1));assertFalse(eligibleIds.contains(2));
    }
    @Test @org.junit.jupiter.api.Order(5) void questionsRepliesAndParentMappingOverHttp() throws Exception {
        var buyer=new Client();buyer.login(3);var seller=new Client();seller.login(2);var admin=new Client();admin.login(1);
        var asked=buyer.post("/api/account/products/1/questions",Map.of("body","Live HTTP question"));assertEquals(200,asked.status);long id=asked.json.get("id").asLong();
        String path="/api/account/products/1/questions/"+id+"/replies";
        for(var actor:List.of(buyer,seller,admin))assertEquals(201,actor.post(path,Map.of("body","Reply over HTTP","role","ADMIN","authorId",1)).status);
        var replies=buyer.get("/api/products/1/questions/"+id+"/replies");assertEquals(200,replies.status);assertEquals(3,replies.json.get("totalElements").asInt());
        assertEquals(List.of("MEMBER","SELLER","ADMIN"),java.util.stream.StreamSupport.stream(replies.json.get("content").spliterator(),false).map(n->n.get("authorLabel").asText()).toList());
        assertEquals(401,new Client().post(path,Map.of("body","Anonymous")).status);
        assertEquals(403,buyer.call("POST",path,Map.of("body","No CSRF"),false).status);
        assertEquals(404,buyer.post(path.replace("/products/1/","/products/2/"),Map.of("body","Cross product")).status);
        assertEquals(409,buyer.post(path,Map.of("body","Reply over HTTP")).status);
        db.update("update product_questions set status='HIDDEN' where id=?",id);
        assertEquals(404,buyer.post(path,Map.of("body","Hidden parent")).status);
        assertEquals("Legacy official answer",db.queryForObject("select answer_body from product_questions where id=1",String.class));
    }
    @Test @org.junit.jupiter.api.Order(6) void oauthHttpStateAndCookieFailClosedWithoutProviderCalls() throws Exception {
        for(String provider:List.of("google","facebook")) {
            var client=new Client();var start=client.get("/oauth2/authorization/"+provider);assertEquals(302,start.status);
            String location=start.headers.firstValue("location").orElseThrow();assertTrue(location.contains("state="));
            assertTrue(URLDecoder.decode(location,StandardCharsets.UTF_8).contains("redirect_uri=http://localhost:5179/login/oauth2/code/"+provider));
            var bad=client.get("/login/oauth2/code/"+provider+"?code=not-sent-to-provider&state=incorrect");assertEquals(302,bad.status);
            assertTrue(bad.headers.firstValue("location").orElse("").contains("/login?authError=OAUTH_FAILED"));assertEquals(401,client.get("/api/auth/me").status);
        }
        EVIDENCE.put("oauth","Actual HTTP state rejection and redirect construction with synthetic local-test registration; Google/Facebook provider LIVE NOT VERIFIED");
    }
    @Test @org.junit.jupiter.api.Order(7) void browserUsesActualBackendWithoutInterception() throws Exception {
        long browserOrder=order("PENDING",1);
        var pb=new ProcessBuilder("node","frontend/qa/live-integration-browser.js");pb.environment().put("BM_LIVE_BACKEND_ORIGIN","http://127.0.0.1:"+port);pb.environment().put("BM_LIVE_TEST_PASSWORD",password);pb.environment().put("BM_LIVE_BROWSER_ORDER",String.valueOf(browserOrder));
        pb.redirectErrorStream(true);pb.redirectOutput(OUT.resolve("browser-live.log").toFile());
        var child=pb.start();try{assertTrue(child.waitFor(150,TimeUnit.SECONDS),"Browser verification timed out");assertEquals(0,child.exitValue(),"See sanitized browser-live.log");}finally{if(child.isAlive())child.destroyForcibly();}
    }
    @AfterAll static void evidence() throws Exception {
        if(smtp!=null){EVIDENCE.put("smtpMessages",smtp.total());smtp.close();}
        Files.createDirectories(OUT);Files.writeString(OUT.resolve("mysql-http-evidence.json"),JSON.writerWithDefaultPrettyPrinter().writeValueAsString(EVIDENCE));
    }
    /** SMTP receiver stays in memory. Neither raw MIME nor OTPs/tokens are written to evidence. */
    static class SmtpSink implements AutoCloseable {
        final ServerSocket server;final ExecutorService worker=Executors.newSingleThreadExecutor();
        final Map<String,List<String>> messages=new ConcurrentHashMap<>();
        SmtpSink() throws IOException {server=new ServerSocket(0,20,InetAddress.getByName("127.0.0.1"));worker.submit(()->{while(!server.isClosed()){try(var socket=server.accept()){receive(socket);}catch(Exception ignored){}}});}
        void receive(Socket socket) throws Exception {
            socket.setSoTimeout(10000);var input=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));var output=new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8),true);output.print("220 localhost SMTP test\r\n");output.flush();String recipient="",line;
            while((line=input.readLine())!=null){String upper=line.toUpperCase(Locale.ROOT);String reply="250 OK";
                if(upper.startsWith("RCPT TO:")){recipient=line.substring(line.indexOf(':')+1).strip().replace("<","").replace(">","");}
                if(upper.equals("DATA")){output.print("354 End with dot\r\n");output.flush();var raw=new StringBuilder();while((line=input.readLine())!=null&&!line.equals("."))raw.append(line.startsWith("..")?line.substring(1):line).append("\r\n");messages.computeIfAbsent(recipient,k->Collections.synchronizedList(new ArrayList<>())).add(raw.toString());}
                if(upper.equals("QUIT")){output.print("221 Bye\r\n");output.flush();break;}output.print(reply+"\r\n");output.flush();
            }
        }
        int total(){return messages.values().stream().mapToInt(List::size).sum();}
        String code(String email,int count)throws Exception {
            for(int n=0;n<100;n++){var rows=messages.get(email);if(rows!=null&&rows.size()>=count){var message=new jakarta.mail.internet.MimeMessage(jakarta.mail.Session.getInstance(new Properties()),new ByteArrayInputStream(rows.get(count-1).getBytes(StandardCharsets.UTF_8)));var matcher=Pattern.compile("(?<![0-9])[0-9]{6}(?![0-9])").matcher(plain(message));assertTrue(matcher.find(),"SMTP MIME contains OTP");return matcher.group();}Thread.sleep(50);}throw new AssertionError("SMTP delivery was not received");
        }
        String plain(jakarta.mail.Part part)throws Exception {if(part.isMimeType("text/plain"))return part.getContent().toString();if(part.getContent() instanceof jakarta.mail.Multipart multi)for(int i=0;i<multi.getCount();i++){String text=plain(multi.getBodyPart(i));if(!text.isEmpty())return text;}return "";}
        public void close()throws Exception{server.close();worker.shutdownNow();}
    }
}
