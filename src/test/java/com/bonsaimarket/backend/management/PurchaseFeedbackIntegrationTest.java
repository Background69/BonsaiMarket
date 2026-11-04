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

/** H2 cùng service/security thật, không gọi nhà cung cấp hoặc DB thực tế của nhóm. */
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
    @Autowired com.bonsaimarket.backend.product.question.ProductQuestions questions;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    User seller,admin,buyer,other; Store store; Product product; Order order; OrderItem item;
    @BeforeAll void schema() throws Exception {
        try(var c=db.getDataSource().getConnection()) {
            com.bonsaimarket.backend.support.BaselineSql.execute(c,
                com.bonsaimarket.backend.support.BaselineSql.tables("product_questions","product_question_replies"));
        }
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
    @Test void replyEditRemovalPermissionsRevisionAndNotifications() throws Exception {
        long q=ask();String path="/api/account/products/"+product.getId()+"/questions/"+q+"/replies";
        mvc.perform(post(path).with(as(other)).with(csrf()).contentType("application/json").content("{\"body\":\"Phản hồi công khai của thành viên\"}")).andExpect(status().isCreated());
        long id=db.queryForObject("select max(id) from product_question_replies where question_id=?",Long.class,q);
        mvc.perform(put(path+"/"+id).with(as(buyer)).with(csrf()).contentType("application/json").content("{\"body\":\"Cannot forge author\",\"revision\":0}")).andExpect(status().isForbidden());
        mvc.perform(put(path+"/"+id).with(as(admin)).with(csrf()).contentType("application/json").content("{\"body\":\"Admin cannot impersonate writer\",\"revision\":0}")).andExpect(status().isForbidden());
        mvc.perform(put(path+"/"+id).with(as(other)).with(csrf()).contentType("application/json").content("{\"body\":\"<script>alert(1)</script> nội dung thuần\",\"revision\":0}")).andExpect(status().isOk());
        mvc.perform(put(path+"/"+id).with(as(other)).with(csrf()).contentType("application/json").content("{\"body\":\"Stale draft\",\"revision\":0}")).andExpect(status().isConflict());
        mvc.perform(delete(path+"/"+id+"?revision=1").with(as(seller)).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(delete(path+"/"+id+"?revision=1").with(as(admin)).with(csrf())).andExpect(status().isOk());
        mvc.perform(get("/api/products/"+product.getId()+"/questions/"+q+"/replies")).andExpect(jsonPath("$.totalElements").value(0));
        assertFalse(questions.list(product.getId(),0,buyer.getId()).content().getFirst().canEdit());
        assertEquals(2,db.queryForObject("select count(*) from marketplace_audit where target_type='QUESTION_REPLY' and target_id=?",Integer.class,id));
        assertEquals(0,db.queryForObject("select count(*) from order_notification_outbox where event_type='PRODUCT_QUESTION_REPLIED' and recipient=?",Integer.class,other.getEmail()));
        assertEquals(2,db.queryForObject("select count(*) from order_notification_outbox where event_type='PRODUCT_QUESTION_REPLIED' and dedup_key like ?",Integer.class,"QUESTION:"+q+":%"));
    }
    @Test void officialReplyNotifiesBuyerAndAdminReplyStaysSeparate() throws Exception {
        long q=ask();mvc.perform(put("/api/seller/product-questions/"+q+"/answer").with(as(seller)).with(csrf()).contentType("application/json").content("{\"body\":\"Có kèm chậu\",\"revision\":0}")).andExpect(status().isOk());
        assertEquals(1,db.queryForObject("select count(*) from order_notification_outbox where event_type='PRODUCT_QUESTION_ANSWERED' and recipient=?",Integer.class,buyer.getEmail()));
        questions.reply(product.getId(),q,admin.getId(),new com.bonsaimarket.backend.product.question.ProductQuestions.Write("Admin hỗ trợ trao đổi công khai",null));
        assertEquals("ADMIN",questions.replies(product.getId(),q,0).content().getFirst().authorLabel());
        assertEquals(seller.getId(),questions.list(product.getId(),0,buyer.getId()).content().getFirst().answerAuthorId());
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
    @Test void statusHistoryRecordsActualTransitionsAndScopesActorIdentity() throws Exception {
        mvc.perform(post(statusPath()).with(as(seller)).with(csrf()).contentType("application/json").content("{\"status\":\"CONFIRMED\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.history[0].fromStatus").value("PENDING"))
            .andExpect(jsonPath("$.history[0].toStatus").value("CONFIRMED"))
            .andExpect(jsonPath("$.history[0].actorLabel").value("Gian hàng"))
            .andExpect(jsonPath("$.history[0].actorId").doesNotExist());
        mvc.perform(get("/api/account/orders/" + order.getId()).with(as(buyer)))
            .andExpect(jsonPath("$.history[0].actorId").doesNotExist());
        mvc.perform(get("/api/admin/orders/" + order.getId()).with(as(admin)))
            .andExpect(jsonPath("$.history[0].actorId").value(seller.getId()));
        db.update("update marketplace_audit set reason='' where target_id=? and target_type='ORDER'", order.getId());
        mvc.perform(get("/api/account/orders/" + order.getId()).with(as(buyer)))
            .andExpect(jsonPath("$.history[0].fromStatus").doesNotExist())
            .andExpect(jsonPath("$.history[0].toStatus").value("CONFIRMED"));
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
    @Test void histogramIncludesEveryStarAndSeparatesVerifiedPurchasesWithoutRemovingLegacy() throws Exception {
        completed();
        db.update("insert into reviews(product_id,user_id,rating,comment) values(?,?,2,'Legacy two'),(?,?,4,'Legacy four')", product.getId(), buyer.getId(), product.getId(), buyer.getId());
        mvc.perform(post(reviewPath()).with(as(buyer)).with(csrf()).contentType("application/json").content(reviewBody(item.getId(),5))).andExpect(status().isCreated());
        mvc.perform(get("/api/products/"+product.getId()+"/reviews"))
            .andExpect(jsonPath("$.ratingCount").value(3)).andExpect(jsonPath("$.averageRating").value(11d/3))
            .andExpect(jsonPath("$.verifiedRatingCount").value(1)).andExpect(jsonPath("$.verifiedAverageRating").value(5d))
            .andExpect(jsonPath("$.unverifiedRatingCount").value(2)).andExpect(jsonPath("$.ratingDistribution.length()").value(5))
            .andExpect(jsonPath("$.ratingDistribution[0].stars").value(5)).andExpect(jsonPath("$.ratingDistribution[0].count").value(1))
            .andExpect(jsonPath("$.ratingDistribution[0].verifiedCount").value(1))
            .andExpect(jsonPath("$.ratingDistribution[1].count").value(1)).andExpect(jsonPath("$.ratingDistribution[2].count").value(0))
            .andExpect(jsonPath("$.ratingDistribution[3].count").value(1)).andExpect(jsonPath("$.ratingDistribution[4].count").value(0));
        assertEquals(3,db.queryForObject("select count(*) from reviews where product_id=?",Integer.class,product.getId()));
    }
    @Test void emptyHistogramHasNoInventedScoreAndLinkedReviewStillRequiresValidPurchaseOwnership() throws Exception {
        mvc.perform(get("/api/products/"+product.getId()+"/reviews"))
            .andExpect(jsonPath("$.ratingCount").value(0)).andExpect(jsonPath("$.averageRating").doesNotExist())
            .andExpect(jsonPath("$.verifiedAverageRating").doesNotExist()).andExpect(jsonPath("$.ratingDistribution.length()").value(5));
        completed();
        db.update("insert into reviews(product_id,user_id,order_item_id,rating,comment) values(?,?,?,?,?)",product.getId(),other.getId(),item.getId(),5,"Imported inconsistent ownership");
        mvc.perform(get("/api/products/"+product.getId()+"/reviews"))
            .andExpect(jsonPath("$.ratingCount").value(1)).andExpect(jsonPath("$.verifiedRatingCount").value(0))
            .andExpect(jsonPath("$.reviews.content[0].verifiedPurchase").value(false));
    }
    @Test void fractionalRatingCannotBeSilentlyCoercedToAnInteger() throws Exception {
        completed();
        mvc.perform(post(reviewPath()).with(as(buyer)).with(csrf()).contentType("application/json")
            .content("{\"orderItemId\":"+item.getId()+",\"rating\":1.5,\"comment\":\"Không làm tròn số sao\"}"))
            .andExpect(status().isBadRequest());
        assertEquals(0,db.queryForObject("select count(*) from reviews where product_id=?",Integer.class,product.getId()));
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
    @Test void authorEditRequiresFreshRevisionAndClosesAfterAnyMemberReply() throws Exception {
        long q=ask();String path="/api/account/product-questions/"+q;
        String edit="{\"body\":\"Xin hỏi kích thước chậu đi kèm?\",\"revision\":0}";
        mvc.perform(put(path).with(as(other)).with(csrf()).contentType("application/json").content(edit)).andExpect(status().isForbidden());
        mvc.perform(put(path).with(as(admin)).with(csrf()).contentType("application/json").content(edit)).andExpect(status().isForbidden());
        mvc.perform(put(path).with(as(buyer)).contentType("application/json").content(edit)).andExpect(status().isForbidden());
        mvc.perform(put(path).with(as(buyer)).with(csrf()).contentType("application/json").content(edit))
            .andExpect(status().isOk()).andExpect(jsonPath("$.revision").value(1)).andExpect(jsonPath("$.canEdit").value(true));
        mvc.perform(put(path).with(as(buyer)).with(csrf()).contentType("application/json").content(edit)).andExpect(status().isConflict());
        mvc.perform(post(replies(q,true)).with(as(other)).with(csrf()).contentType("application/json").content("{\"body\":\"Một phản hồi thành viên\"}")).andExpect(status().isCreated());
        mvc.perform(put(path).with(as(buyer)).with(csrf()).contentType("application/json").content(edit.replace("\"revision\":0","\"revision\":1")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("QUESTION_EDIT_CLOSED"));
        assertEquals(1,db.queryForObject("select count(*) from product_question_replies where question_id=?",Integer.class,q));
    }
    @Test void officialAnswerAlsoClosesAuthorEditingAndValidatesEditContent() throws Exception {
        long q=ask();String path="/api/account/product-questions/"+q;
        for(String body:List.of(" ","x".repeat(2001)))mvc.perform(put(path).with(as(buyer)).with(csrf()).contentType("application/json").content("{\"body\":\""+body+"\",\"revision\":0}")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/seller/product-questions/"+q+"/answer").with(as(seller)).with(csrf()).contentType("application/json").content("{\"body\":\"Kích thước chậu 20 cm\",\"revision\":0}")).andExpect(status().isOk());
        mvc.perform(put(path).with(as(buyer)).with(csrf()).contentType("application/json").content("{\"body\":\"Đổi câu hỏi\",\"revision\":1}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("QUESTION_EDIT_CLOSED"));
        mvc.perform(get("/api/products/"+product.getId()+"/questions").with(as(buyer))).andExpect(jsonPath("$.content[0].canEdit").value(false));
    }
    @Test void sellerInboxIsScopedAndUsesOfficialAnswersRatherThanMemberReplies() throws Exception {
        long q=ask();
        mvc.perform(get("/api/seller/product-questions").with(as(buyer))).andExpect(status().isForbidden());
        mvc.perform(get("/api/seller/product-questions")).andExpect(status().isUnauthorized());
        var foreignSeller=user(UserRole.SELLER);
        mvc.perform(get("/api/seller/product-questions").with(as(foreignSeller))).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/seller/product-questions").with(as(admin))).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(post(replies(q,true)).with(as(buyer)).with(csrf()).contentType("application/json").content("{\"body\":\"Bổ sung câu hỏi\"}")).andExpect(status().isCreated());
        mvc.perform(get("/api/seller/product-questions").with(as(seller))).andExpect(jsonPath("$.content[0].id").value(q)).andExpect(jsonPath("$.content[0].answered").value(false));
        mvc.perform(put("/api/seller/product-questions/"+q+"/answer").with(as(seller)).with(csrf()).contentType("application/json").content("{\"body\":\"Trả lời chính thức\",\"revision\":0}")).andExpect(status().isOk());
        mvc.perform(get("/api/seller/product-questions").with(as(seller))).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/seller/product-questions?answer=ANSWERED").with(as(seller))).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/seller/product-questions?answer=invalid").with(as(seller))).andExpect(status().isBadRequest());
    }
    @Test void questionNotificationUsesActualSellerAndRollsBackWithQuestion() throws Exception {
        long q=ask();
        assertEquals(seller.getEmail(),db.queryForObject("select recipient from order_notification_outbox where dedup_key=?",String.class,"QUESTION:"+q+":ASKED:SELLER:"+seller.getId()));
        assertEquals(1,db.queryForObject("select count(*) from order_notification_outbox where recipient=? and order_id is null and event_type='PRODUCT_QUESTION_ASKED' and status='PENDING'",Integer.class,seller.getEmail()));
        mvc.perform(post("/api/account/products/"+product.getId()+"/questions").with(as(buyer)).with(csrf()).contentType("application/json").content("{\"body\":\"Có kèm chậu không?\"}"))
            .andExpect(status().isConflict());
        var tx=new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        tx.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(s->{questions.ask(product.getId(),buyer.getId(),new com.bonsaimarket.backend.product.question.ProductQuestions.Write("Rollback notification question",null));throw new IllegalStateException("Owned H2 rollback");}));
        assertEquals(0,db.queryForObject("select count(*) from product_questions where body='Rollback notification question'",Integer.class));
        assertEquals(1,db.queryForObject("select count(*) from order_notification_outbox where recipient=?",Integer.class,seller.getEmail()));
        verifyNoInteractions(mail);
    }
    @Test void deepQuestionLinkResolvesItsPageAndSellerInboxPaginationIsScoped() throws Exception {
        for(int i=0;i<12;i++)db.update("insert into product_questions(product_id,author_id,body) values(?,?,?)",product.getId(),buyer.getId(),"Question "+i);
        long first=db.queryForObject("select min(id) from product_questions where product_id=?",Long.class,product.getId());
        mvc.perform(get("/api/products/"+product.getId()+"/questions?questionId="+first))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(1)).andExpect(jsonPath("$.content[1].id").value(first));
        mvc.perform(get("/api/seller/product-questions?page=1").with(as(seller)))
            .andExpect(jsonPath("$.content.length()").value(2)).andExpect(jsonPath("$.totalElements").value(12));
        mvc.perform(get("/api/products/999999/questions?questionId="+first)).andExpect(status().isNotFound());
        mvc.perform(get("/api/seller/product-questions?page=-1").with(as(seller))).andExpect(status().isBadRequest());
    }
    @Test void firstReplyAndAuthorEditSerializeWithoutLosingEitherExistingContent() throws Exception {
        long q=ask();
        var results=race(()->mvc.perform(put("/api/account/product-questions/"+q).with(as(buyer)).with(csrf()).contentType("application/json").content("{\"body\":\"Câu hỏi sửa trước phản hồi\",\"revision\":0}")).andReturn().getResponse().getStatus(),
            ()->mvc.perform(post(replies(q,true)).with(as(other)).with(csrf()).contentType("application/json").content("{\"body\":\"Phản hồi đồng thời\"}")).andReturn().getResponse().getStatus());
        assertTrue(results.contains(201));assertTrue(results.contains(200)||results.contains(409));
        assertEquals(1,db.queryForObject("select count(*) from product_question_replies where question_id=?",Integer.class,q));
        assertEquals(results.contains(200)?1L:0L,db.queryForObject("select revision from product_questions where id=?",Long.class,q));
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
