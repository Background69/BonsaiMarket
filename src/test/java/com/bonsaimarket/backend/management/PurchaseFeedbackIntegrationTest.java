package com.bonsaimarket.backend.management;

import com.bonsaimarket.backend.auth.*;
import com.bonsaimarket.backend.category.*;
import com.bonsaimarket.backend.order.*;
import com.bonsaimarket.backend.order.Order;
import com.bonsaimarket.backend.product.*;
import com.bonsaimarket.backend.store.*;
import com.bonsaimarket.backend.user.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Isolated H2 + real application services/security, no provider or live database calls. */
@SpringBootTest(properties={"spring.profiles.active=purchase-feedback-test",
    "spring.datasource.url=jdbc:h2:mem:purchase-feedback;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
    "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
    "spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc @TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PurchaseFeedbackIntegrationTest {
    @Autowired MockMvc mvc; @Autowired JdbcTemplate db;
    @Autowired UserRepository users; @Autowired StoreRepository stores; @Autowired CategoryRepository categories;
    @Autowired ProductRepository products; @Autowired OrderRepository orders; @Autowired OrderItemRepository items;
    @MockitoBean AuthMailSender mail;
    User seller,admin,buyer,other; Store store; Product product; Order order; OrderItem item;
    @BeforeAll void schema() throws Exception {
        String sql=Files.readString(Path.of("src/main/resources/db/migration/V15__product_questions_and_answers.sql"));
        String replies=Files.readString(Path.of("src/main/resources/db/migration/V18__purchase_reviews_and_question_replies.sql"));
        sql+="\n"+replies.substring(replies.indexOf("CREATE TABLE product_question_replies"));
        try(var c=db.getDataSource().getConnection()) {ScriptUtils.executeSqlScript(c,new ByteArrayResource(sql.replace("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4","").getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
    }
    @BeforeEach void setup() {
        seller=user(UserRole.SELLER); admin=user(UserRole.ADMIN); buyer=user(UserRole.CUSTOMER); other=user(UserRole.CUSTOMER);
        store=new Store();store.setOwner(seller);store.setName("Vườn kiểm thử");store.setStatus(StoreStatus.ACTIVE);store=stores.save(store);
        var c=new Category();c.setName("Bonsai");c.setSlug(UUID.randomUUID().toString());c.setIsActive(true);c=categories.save(c);
        product=new Product();product.setName("Mai chiếu thủy");product.setSlug(UUID.randomUUID().toString());product.setStore(store);product.setCategory(c);product.setPrice(BigDecimal.TEN);product.setStock(10);product.setStatus(ProductStatus.ACTIVE);product=products.save(product);
        order=new Order();order.setCode(UUID.randomUUID().toString());order.setStore(store);order.setBuyer(buyer);order.setStatus(OrderStatus.PENDING);order.setCreatedAt(LocalDateTime.now());order.setUpdatedAt(order.getCreatedAt());order.setSubtotal(BigDecimal.TEN);order.setShippingFee(BigDecimal.ZERO);order.setTotal(BigDecimal.TEN);order.setShippingAddressSnapshot("Test address");order.setShippingMethod("STANDARD");order=orders.save(order);
        item=line(order,product);
    }
    User user(UserRole role) {var u=new User();u.setName(role+" kiểm thử");u.setEmail(UUID.randomUUID()+"@example.invalid");u.setPasswordHash("!");u.setIsActive(true);u.setRole(role);u.setEmailVerifiedAt(LocalDateTime.now());return users.save(u);}
    OrderItem line(Order o,Product p) {var i=new OrderItem();i.setOrder(o);i.setProduct(p);i.setQuantity(2);i.setProductNameSnapshot(p.getName());i.setUnitPrice(BigDecimal.TEN);i.setLineTotal(new BigDecimal("20"));return items.save(i);}
    RequestPostProcessor as(User u) {return authentication(UsernamePasswordAuthenticationToken.authenticated(AuthPrincipal.of(u),null,List.of(new SimpleGrantedAuthority("ROLE_"+u.getRole()))));}
    String reviewPath(){return "/api/account/products/"+product.getId()+"/reviews";}
    String reviewBody(long itemId,int rating){return "{\"orderItemId\":"+itemId+",\"rating\":"+rating+",\"comment\":\"Cây đúng mô tả\",\"userId\":"+other.getId()+"}";}
    String cancelPath(){return "/api/account/orders/"+order.getId()+"/cancel";}
    String statusPath(){return "/api/seller/orders/"+order.getId()+"/status";}
    void completed(){order.setStatus(OrderStatus.COMPLETED);orders.save(order);}
    long ask() throws Exception {
        mvc.perform(post("/api/account/products/"+product.getId()+"/questions").with(as(buyer)).with(csrf()).contentType("application/json").content("{\"body\":\"Có kèm chậu không?\"}"))
            .andExpect(status().isOk());
        return db.queryForObject("select max(id) from product_questions where product_id=?",Long.class,product.getId());
    }
    String replies(long q,boolean write){return "/api/"+(write?"account/":"")+"products/"+product.getId()+"/questions/"+q+"/replies";}
    @Test void buyerProjectionIsScopedAndOnlyOffersCancellationBeforeShipping() throws Exception {
        mvc.perform(get("/api/account/orders?search="+order.getCode()).with(as(buyer))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/account/orders/"+order.getId()).with(as(other))).andExpect(status().isNotFound());
        mvc.perform(get("/api/account/orders/"+order.getId()).with(as(buyer))).andExpect(jsonPath("$.actions[0]").value("CANCELLED"));
        mvc.perform(post(cancelPath()).with(as(other)).with(csrf())).andExpect(status().isNotFound());
    }
    @Test void cancellationRequiresSessionAndCsrf() throws Exception {
        mvc.perform(post(cancelPath()).with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(post(cancelPath()).with(as(buyer))).andExpect(status().isForbidden());
        assertEquals(OrderStatus.PENDING,orders.findById(order.getId()).orElseThrow().getStatus());
    }
    @Test void stateMachineRejectsSkipsAndSellerCompletionButAllowsAdminCompletion() throws Exception {
        for(String state:List.of("SHIPPING","DELIVERED","COMPLETED"))
            mvc.perform(post(statusPath()).with(as(seller)).with(csrf()).contentType("application/json").content("{\"status\":\""+state+"\"}")).andExpect(status().isConflict());
        for(String state:List.of("CONFIRMED","SHIPPING","DELIVERED"))
            mvc.perform(post(statusPath()).with(as(seller)).with(csrf()).contentType("application/json").content("{\"status\":\""+state+"\"}")).andExpect(status().isOk());
        mvc.perform(post(statusPath()).with(as(seller)).with(csrf()).contentType("application/json").content("{\"status\":\"COMPLETED\"}")).andExpect(status().isConflict());
        mvc.perform(post("/api/admin/orders/"+order.getId()+"/status").with(as(admin)).with(csrf()).contentType("application/json").content("{\"status\":\"COMPLETED\"}")).andExpect(status().isOk());
        assertEquals(8,products.findById(product.getId()).orElseThrow().getStock());
        assertEquals(1,db.queryForObject("select count(*) from inventory_debits where order_id=?",Integer.class,order.getId()));
    }
    @Test void sellerCannotTransitionForeignOrderAndDuplicateConfirmDoesNotDebitTwice() throws Exception {
        var stranger=user(UserRole.SELLER);
        mvc.perform(post(statusPath()).with(as(stranger)).with(csrf()).contentType("application/json").content("{\"status\":\"CONFIRMED\"}")).andExpect(status().isNotFound());
        mvc.perform(post(statusPath()).with(as(seller)).with(csrf()).contentType("application/json").content("{\"status\":\"CONFIRMED\"}")).andExpect(status().isOk());
        mvc.perform(post(statusPath()).with(as(seller)).with(csrf()).contentType("application/json").content("{\"status\":\"CONFIRMED\"}")).andExpect(status().isConflict());
        assertEquals(8,products.findById(product.getId()).orElseThrow().getStock());
    }
    @Test void pendingCancellationDoesNotInventDebitRefundOrEmail() throws Exception {
        mvc.perform(post(cancelPath()).with(as(buyer)).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        assertEquals(10,products.findById(product.getId()).orElseThrow().getStock());
        assertEquals(0,db.queryForObject("select count(*) from inventory_debits where order_id=?",Integer.class,order.getId()));
        verifyNoInteractions(mail);
    }
    @Test void confirmedCancellationRestoresExactlyOnceAndRejectsShippingOrTerminal() throws Exception {
        mvc.perform(post(statusPath()).with(as(seller)).with(csrf()).contentType("application/json").content("{\"status\":\"CONFIRMED\"}")).andExpect(status().isOk());
        assertEquals(8,products.findById(product.getId()).orElseThrow().getStock());
        mvc.perform(post(cancelPath()).with(as(buyer)).with(csrf())).andExpect(status().isOk());
        mvc.perform(post(cancelPath()).with(as(buyer)).with(csrf())).andExpect(status().isConflict());
        assertEquals(10,products.findById(product.getId()).orElseThrow().getStock());
        for(var state:List.of(OrderStatus.SHIPPING,OrderStatus.DELIVERED,OrderStatus.COMPLETED)) {
            order.setStatus(state);orders.save(order);
            mvc.perform(post(cancelPath()).with(as(buyer)).with(csrf())).andExpect(status().isConflict());
        }
    }
    @Test void foreignStoreLinesBlockEvenAdminCancellation() throws Exception {
        var s=new Store();s.setOwner(user(UserRole.SELLER));s.setName("Other");s.setStatus(StoreStatus.ACTIVE);s=stores.save(s);
        product.setStore(s);products.save(product);
        mvc.perform(post("/api/admin/orders/"+order.getId()+"/status").with(as(admin)).with(csrf()).contentType("application/json").content("{\"status\":\"CANCELLED\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ORDER_STORE_CONFLICT"));
        assertEquals(OrderStatus.PENDING,orders.findById(order.getId()).orElseThrow().getStatus());
    }
    @Test void concurrentCancelAndConfirmKeepLedgerAndStockConsistent() throws Exception {
        race(()->mvc.perform(post(cancelPath()).with(as(buyer)).with(csrf())).andReturn().getResponse().getStatus(),
            ()->mvc.perform(post(statusPath()).with(as(seller)).with(csrf()).contentType("application/json").content("{\"status\":\"CONFIRMED\"}")).andReturn().getResponse().getStatus());
        assertEquals(OrderStatus.CANCELLED,orders.findById(order.getId()).orElseThrow().getStatus());
        assertEquals(10,products.findById(product.getId()).orElseThrow().getStock());
        assertEquals(0,db.queryForObject("select count(*) from inventory_debits where order_id=? and restored=false",Integer.class,order.getId()));
    }
    @Test void concurrentCancellationProducesOneAuditAndOneRestore() throws Exception {
        var results=race(()->mvc.perform(post(cancelPath()).with(as(buyer)).with(csrf())).andReturn().getResponse().getStatus(),
            ()->mvc.perform(post(cancelPath()).with(as(buyer)).with(csrf())).andReturn().getResponse().getStatus());
        assertEquals(1,Collections.frequency(results,200));assertEquals(1,Collections.frequency(results,409));
        assertEquals(1,db.queryForObject("select count(*) from marketplace_audit where target_id=? and action='ORDER_CANCELLED'",Integer.class,order.getId()));
    }
    @Test void completedPurchaseAllowsReviewAndIgnoresForgedUser() throws Exception {
        completed();
        mvc.perform(get("/api/account/products/"+product.getId()+"/review-eligibility").with(as(buyer))).andExpect(status().isOk()).andExpect(jsonPath("$.purchases[0].orderItemId").value(item.getId()));
        mvc.perform(post(reviewPath()).with(as(buyer)).with(csrf()).contentType("application/json").content(reviewBody(item.getId(),5))).andExpect(status().isCreated());
        assertEquals(buyer.getId(),db.queryForObject("select user_id from reviews where order_item_id=?",Long.class,item.getId()));
        mvc.perform(get("/api/products/"+product.getId()+"/reviews")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.reviews.content[0].verifiedPurchase").value(true)).andExpect(jsonPath("$.averageRating").value(5.0));
        mvc.perform(get("/api/account/products/"+product.getId()+"/review-eligibility").with(as(buyer))).andExpect(jsonPath("$.purchases.length()").value(0));
    }
    @Test void unfinishedCancelledWrongOwnerProductAndMissingItemCannotBeReviewed() throws Exception {
        for(var state:List.of(OrderStatus.PENDING,OrderStatus.CONFIRMED,OrderStatus.SHIPPING,OrderStatus.DELIVERED,OrderStatus.CANCELLED)) {
            order.setStatus(state);orders.save(order);
            mvc.perform(post(reviewPath()).with(as(buyer)).with(csrf()).contentType("application/json").content(reviewBody(item.getId(),5))).andExpect(status().isConflict());
        }
        completed();
        mvc.perform(post(reviewPath()).with(as(other)).with(csrf()).contentType("application/json").content(reviewBody(item.getId(),5))).andExpect(status().isNotFound());
        mvc.perform(post(reviewPath()).with(as(buyer)).with(csrf()).contentType("application/json").content(reviewBody(Long.MAX_VALUE,5))).andExpect(status().isNotFound());
        var p=new Product();p.setStore(store);p.setCategory(product.getCategory());p.setName("Other product");p.setSlug(UUID.randomUUID().toString());p.setStatus(ProductStatus.ACTIVE);p=products.save(p);
        mvc.perform(post("/api/account/products/"+p.getId()+"/reviews").with(as(buyer)).with(csrf()).contentType("application/json").content(reviewBody(item.getId(),5))).andExpect(status().isNotFound());
    }
    @Test void reviewValidationAuthCsrfAndDuplicateAreEnforced() throws Exception {
        completed();
        mvc.perform(post(reviewPath()).with(csrf()).contentType("application/json").content(reviewBody(item.getId(),5))).andExpect(status().isUnauthorized());
        mvc.perform(post(reviewPath()).with(as(buyer)).contentType("application/json").content(reviewBody(item.getId(),5))).andExpect(status().isForbidden());
        for(int rating:List.of(0,6))mvc.perform(post(reviewPath()).with(as(buyer)).with(csrf()).contentType("application/json").content(reviewBody(item.getId(),rating))).andExpect(status().isBadRequest());
        for(String comment:List.of("","x".repeat(2001)))mvc.perform(post(reviewPath()).with(as(buyer)).with(csrf()).contentType("application/json").content("{\"orderItemId\":"+item.getId()+",\"rating\":5,\"comment\":\""+comment+"\"}")).andExpect(status().isBadRequest());
        mvc.perform(post(reviewPath()).with(as(buyer)).with(csrf()).contentType("application/json").content(reviewBody(item.getId(),5))).andExpect(status().isCreated());
        mvc.perform(post(reviewPath()).with(as(buyer)).with(csrf()).contentType("application/json").content(reviewBody(item.getId(),5))).andExpect(status().isConflict());
    }
    @Test void separatePurchaseItemsAllowSeparateReviewsAndCorrectAggregate() throws Exception {
        completed();var second=line(order,product);
        mvc.perform(post(reviewPath()).with(as(buyer)).with(csrf()).contentType("application/json").content(reviewBody(item.getId(),5))).andExpect(status().isCreated());
        mvc.perform(post(reviewPath()).with(as(buyer)).with(csrf()).contentType("application/json").content(reviewBody(second.getId(),1))).andExpect(status().isCreated());
        mvc.perform(get("/api/products/"+product.getId()+"/reviews")).andExpect(jsonPath("$.ratingCount").value(2)).andExpect(jsonPath("$.averageRating").value(3.0));
    }
    @Test void concurrentDuplicateReviewIsWrittenOnce() throws Exception {
        completed();Callable<Integer> submit=()->mvc.perform(post(reviewPath()).with(as(buyer)).with(csrf()).contentType("application/json").content(reviewBody(item.getId(),5))).andReturn().getResponse().getStatus();
        var results=race(submit,submit);assertEquals(1,Collections.frequency(results,201));assertEquals(1,Collections.frequency(results,409));
        assertEquals(1,db.queryForObject("select count(*) from reviews where order_item_id=?",Integer.class,item.getId()));
    }
    @Test void legacyReviewIsNeverPresentedAsVerifiedAndPaginationIsBounded() throws Exception {
        for(int i=0;i<11;i++)db.update("insert into reviews(product_id,user_id,rating,comment) values(?,?,?,?)",product.getId(),buyer.getId(),4,"Legacy "+i);
        mvc.perform(get("/api/products/"+product.getId()+"/reviews?page=1")).andExpect(jsonPath("$.reviews.content.length()").value(1)).andExpect(jsonPath("$.reviews.content[0].verifiedPurchase").value(false));
        mvc.perform(get("/api/products/"+product.getId()+"/reviews?page=-1")).andExpect(status().isBadRequest());
    }
    @Test void buyerSellerAndAdminRepliesUseServerLabelsAndPreserveOfficialAnswer() throws Exception {
        long q=ask();
        mvc.perform(put("/api/seller/product-questions/"+q+"/answer").with(as(seller)).with(csrf()).contentType("application/json").content("{\"body\":\"Có kèm chậu\",\"revision\":0}")).andExpect(status().isOk());
        for(var u:List.of(buyer,seller,admin,other))mvc.perform(post(replies(q,true)).with(as(u)).with(csrf()).contentType("application/json").content("{\"body\":\"Trả lời của tôi\",\"authorId\":"+seller.getId()+",\"role\":\"ADMIN\"}")).andExpect(status().isCreated());
        mvc.perform(get(replies(q,false))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(4))
            .andExpect(jsonPath("$.content[0].authorLabel").value("MEMBER")).andExpect(jsonPath("$.content[1].authorLabel").value("SELLER"))
            .andExpect(jsonPath("$.content[2].authorLabel").value("ADMIN")).andExpect(jsonPath("$.content[3].authorLabel").value("MEMBER"));
        assertEquals("Có kèm chậu",db.queryForObject("select answer_body from product_questions where id=?",String.class,q));
    }
    @Test void anonymousCsrfInactiveForgedCrossProductAndHiddenParentRepliesAreDenied() throws Exception {
        long q=ask();String body="{\"body\":\"Một trả lời\"}";
        mvc.perform(post(replies(q,true)).with(csrf()).contentType("application/json").content(body)).andExpect(status().isUnauthorized());
        mvc.perform(post(replies(q,true)).with(as(buyer)).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post(replies(q,true).replace("/products/"+product.getId()+"/","/products/999999/")).with(as(buyer)).with(csrf()).contentType("application/json").content(body)).andExpect(status().isNotFound());
        mvc.perform(post(replies(Long.MAX_VALUE,true)).with(as(buyer)).with(csrf()).contentType("application/json").content(body)).andExpect(status().isNotFound());
        db.update("update product_questions set status='HIDDEN' where id=?",q);
        mvc.perform(post(replies(q,true)).with(as(buyer)).with(csrf()).contentType("application/json").content(body)).andExpect(status().isNotFound());
        mvc.perform(get(replies(q,false))).andExpect(status().isNotFound());
        buyer.setIsActive(false);users.save(buyer);
        mvc.perform(post(replies(q,true)).with(as(buyer)).with(csrf()).contentType("application/json").content(body)).andExpect(status().isUnauthorized());
    }
    @Test void hiddenProductBlocksQuestionsRepliesAndReviews() throws Exception {
        long q=ask();product.setStatus(ProductStatus.ARCHIVED);products.save(product);
        mvc.perform(get(replies(q,false))).andExpect(status().isNotFound());
        mvc.perform(get("/api/products/"+product.getId()+"/reviews")).andExpect(status().isNotFound());
        mvc.perform(post("/api/account/products/"+product.getId()+"/questions").with(as(buyer)).with(csrf()).contentType("application/json").content("{\"body\":\"Question\"}")).andExpect(status().isNotFound());
    }
    @Test void replyLengthDuplicateRateLimitPaginationAndPlainTextAreEnforced() throws Exception {
        long q=ask();
        for(String text:List.of("","x".repeat(4001)))mvc.perform(post(replies(q,true)).with(as(buyer)).with(csrf()).contentType("application/json").content("{\"body\":\""+text+"\"}")).andExpect(status().isBadRequest());
        String xss="<script>alert(1)</script>";
        mvc.perform(post(replies(q,true)).with(as(buyer)).with(csrf()).contentType("application/json").content("{\"body\":\""+xss+"\"}")).andExpect(status().isCreated());
        mvc.perform(post(replies(q,true)).with(as(buyer)).with(csrf()).contentType("application/json").content("{\"body\":\""+xss+"\"}")).andExpect(status().isConflict());
        for(int i=0;i<29;i++)db.update("insert into product_question_replies(question_id,author_id,body,created_at) values(?,?,?,CURRENT_TIMESTAMP)",q,buyer.getId(),"Reply "+i);
        mvc.perform(post(replies(q,true)).with(as(buyer)).with(csrf()).contentType("application/json").content("{\"body\":\"Too many\"}")).andExpect(status().isTooManyRequests());
        mvc.perform(get(replies(q,false)+"?page=1")).andExpect(jsonPath("$.content.length()").value(10)).andExpect(jsonPath("$.totalPages").value(3));
        mvc.perform(get(replies(q,false)+"?page=-1")).andExpect(status().isBadRequest());
    }
    List<Integer> race(Callable<Integer> first,Callable<Integer> second)throws Exception {
        var gate=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {var a=pool.submit(()->{gate.await();return first.call();});var b=pool.submit(()->{gate.await();return second.call();});gate.countDown();
            var result=List.of(a.get(25,TimeUnit.SECONDS),b.get(25,TimeUnit.SECONDS));assertTrue(result.stream().allMatch(s->s==200||s==201||s==409),result.toString());return result;
        } finally {pool.shutdownNow();}
    }
}
