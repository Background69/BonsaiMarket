package com.bonsaimarket.backend.report;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import com.bonsaimarket.backend.user.*;
import com.bonsaimarket.backend.store.*;
import com.bonsaimarket.backend.order.*;
import com.bonsaimarket.backend.order.Order;
import com.bonsaimarket.backend.marketplace.MarketplaceException;
import com.bonsaimarket.backend.support.BaselineSql;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static com.bonsaimarket.backend.report.ReportDtos.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.profiles.active=report-test","spring.datasource.url=jdbc:h2:mem:customerreports;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=none","orders.mail.enabled=false","ai.enabled=false","vnpay.enabled=false"})
@AutoConfigureMockMvc @TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CustomerReportingIntegrationTest {
    @Autowired JdbcTemplate db;@Autowired MockMvc mvc;@Autowired ReportService reports;
    @Autowired UserRepository users;@Autowired StoreRepository stores;@Autowired OrderRepository orders;
    User buyer,seller,admin,otherSeller,otherBuyer;Store store;Order order;java.sql.Connection bootstrap;
    @BeforeAll void schema()throws Exception{bootstrap=db.getDataSource().getConnection();BaselineSql.currentSchema(bootstrap);}
    @AfterAll void close()throws Exception{bootstrap.close();}
    @BeforeEach void fixture(){
        buyer=user(UserRole.CUSTOMER);seller=user(UserRole.SELLER);admin=user(UserRole.ADMIN);otherSeller=user(UserRole.SELLER);otherBuyer=user(UserRole.CUSTOMER);
        store=new Store();store.setOwner(seller);store.setName("Reporting fixture");store.setStatus(StoreStatus.ACTIVE);store.setCreatedAt(LocalDateTime.now());store.setUpdatedAt(LocalDateTime.now());store=stores.save(store);
        order=new Order();order.setCode(UUID.randomUUID().toString());order.setStore(store);order.setBuyer(buyer);order.setStatus(OrderStatus.COMPLETED);order.setSubtotal(BigDecimal.TEN);order.setShippingFee(BigDecimal.ZERO);order.setTotal(BigDecimal.TEN);order.setShippingMethod("STANDARD");order.setShippingAddressSnapshot("Isolated fixture");order.setCreatedAt(LocalDateTime.now());order.setUpdatedAt(LocalDateTime.now());order=orders.save(order);
    }
    User user(UserRole role){var u=new User();u.setName(role+" fixture");u.setEmail(UUID.randomUUID()+"@example.invalid");u.setRole(role);u.setIsActive(true);u.setPasswordHash("!disabled!");u.setEmailVerifiedAt(LocalDateTime.now());u.setCreatedAt(LocalDateTime.now());u.setUpdatedAt(LocalDateTime.now());return users.save(u);}
    RequestPostProcessor as(User u){return authentication(UsernamePasswordAuthenticationToken.authenticated(AuthPrincipal.of(u),null,List.of(new SimpleGrantedAuthority("ROLE_"+u.getRole()))));}
    ReportWrite write(){return new ReportWrite("USER",buyer.getId(),"HARASSMENT","Yêu cầu xem xét nội dung giao tiếp.","ORDER",order.getId(),UUID.randomUUID().toString());}
    void code(String expected,Runnable action){assertEquals(expected,assertThrows(MarketplaceException.class,action::run).code);}
    String json(ReportWrite w){return "{\"targetType\":\"USER\",\"targetId\":"+w.targetId()+",\"reason\":\""+w.reason()+"\",\"description\":\"test\",\"contextType\":\"ORDER\",\"contextId\":"+w.contextId()+",\"requestKey\":\""+w.requestKey()+"\"}";}
    @Test void validSellerContextSubmissionAndReplay(){var w=write();var context=reports.customerContext(seller.getId(),order.getId());assertEquals(buyer.getId(),context.customerId());assertTrue(context.eligible());var r=reports.create(seller.getId(),w);assertEquals("OPEN",r.status());assertEquals(r.id(),reports.create(seller.getId(),w).id());assertEquals(1,db.queryForObject("select count(*) from marketplace_reports where id=?",Integer.class,r.id()));assertEquals(1,db.queryForObject("select count(*) from marketplace_audit where target_type='REPORT' and target_id=?",Integer.class,r.id()));}
    @Test void changedReplayAndNewRequestDuplicateAreConflicts(){var w=write();reports.create(seller.getId(),w);code("REPORT_REPLAY_CONFLICT",()->reports.create(seller.getId(),new ReportWrite("USER",buyer.getId(),"FRAUD","Different", "ORDER",order.getId(),w.requestKey())));code("REPORT_EXISTS",()->reports.create(seller.getId(),write()));}
    @Test void ownershipTargetRoleAndSelfReportAreClosed(){code("NOT_FOUND",()->reports.customerContext(otherSeller.getId(),order.getId()));code("FORBIDDEN",()->reports.customerContext(buyer.getId(),order.getId()));var w=write();code("NOT_FOUND",()->reports.create(seller.getId(),new ReportWrite("USER",otherBuyer.getId(),w.reason(),w.description(),"ORDER",order.getId(),w.requestKey())));code("NOT_FOUND",()->reports.create(seller.getId(),new ReportWrite("USER",seller.getId(),w.reason(),w.description(),"ORDER",order.getId(),w.requestKey())));code("NOT_FOUND",()->reports.create(seller.getId(),new ReportWrite("USER",admin.getId(),w.reason(),w.description(),"ORDER",order.getId(),w.requestKey())));}
    @Test void targetMustBeCustomerEvenWhenOrderExists(){db.update("update orders set buyer_user_id=? where id=?",admin.getId(),order.getId());assertFalse(reports.customerContext(seller.getId(),order.getId()).eligible());code("NOT_FOUND",()->reports.create(seller.getId(),new ReportWrite("USER",admin.getId(),"OTHER","Test","ORDER",order.getId(),UUID.randomUUID().toString())));}
    @Test void invalidContextAndMissingKeyAreRejected(){var w=write();code("VALIDATION_FAILED",()->reports.create(seller.getId(),new ReportWrite("USER",buyer.getId(),"FRAUD","Test","CHAT",order.getId(),w.requestKey())));code("VALIDATION_FAILED",()->reports.create(seller.getId(),new ReportWrite("USER",buyer.getId(),"FRAUD","Test")));code("VALIDATION_FAILED",()->reports.create(seller.getId(),new ReportWrite("USER",buyer.getId(),"OTHER"," ","ORDER",order.getId(),w.requestKey())));}
    @Test void httpSecurityCsrfValidationAndSessionExpiry()throws Exception{
        var w=write();String content=json(w),path="/api/account/reports";
        mvc.perform(post(path).with(csrf()).contentType("application/json").content(content)).andExpect(status().isUnauthorized());
        mvc.perform(post(path).with(as(seller)).contentType("application/json").content(content)).andExpect(status().isForbidden());
        mvc.perform(post(path).with(as(seller)).with(csrf()).contentType("application/json").content(content.replace("HARASSMENT","FORGED"))).andExpect(status().isBadRequest());
        mvc.perform(post(path).with(as(otherSeller)).with(csrf()).contentType("application/json").content(content)).andExpect(status().isNotFound());
        mvc.perform(get("/api/seller/orders/"+order.getId()+"/customer-report-context").with(as(otherSeller))).andExpect(status().isNotFound());
        mvc.perform(post(path).with(as(seller)).with(csrf()).contentType("application/json").content(content)).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("OPEN"));
    }
    @Test void privateReportsAndModerationCannotBeAccessedByNonAdmin()throws Exception{var r=reports.create(seller.getId(),write());for(var u:List.of(buyer,otherBuyer,seller,otherSeller)){mvc.perform(get("/api/admin/reports/"+r.id()).with(as(u))).andExpect(status().isForbidden());mvc.perform(get("/api/admin/reports/"+r.id()+"/history").with(as(u))).andExpect(status().isForbidden());mvc.perform(patch("/api/admin/reports/"+r.id()).with(as(u)).with(csrf()).contentType("application/json").content("{\"status\":\"IN_REVIEW\",\"reason\":\"test\",\"revision\":0}")).andExpect(status().isForbidden());}}
    @Test void moderationTransitionsHistoryFiltersAndNotificationPrivacy(){var r=reports.create(seller.getId(),write());var before=reports.detail(admin.getId(),r.id());assertEquals(seller.getId(),before.reporterId());assertEquals(order.getId(),before.customerContext().orderId());assertTrue(reports.filtered(admin.getId(),"OPEN",0,"USER",null).content().stream().anyMatch(v->v.id().equals(r.id())));code("REPORT_STATE_INVALID",()->reports.decide(admin.getId(),r.id(),new ReportDecision("RESOLVED","Too soon",0L)));var review=reports.decide(admin.getId(),r.id(),new ReportDecision("IN_REVIEW","Đã tiếp nhận",0L));code("DATA_CONFLICT",()->reports.decide(admin.getId(),r.id(),new ReportDecision("DISMISSED","Stale",0L)));var closed=reports.decide(admin.getId(),r.id(),new ReportDecision("RESOLVED","Đã xem xét; không tự động khóa tài khoản",review.revision()));assertEquals(3,reports.history(admin.getId(),r.id()).size());assertEquals("RESOLVED",closed.status());code("REPORT_STATE_INVALID",()->reports.decide(admin.getId(),r.id(),new ReportDecision("DISMISSED","Closed",closed.revision())));assertEquals(0,db.queryForObject("select count(*) from order_notification_outbox where recipient_user_id=?",Integer.class,buyer.getId()));assertEquals(3,db.queryForObject("select count(*) from order_notification_outbox where recipient_user_id=?",Integer.class,seller.getId()));assertTrue(users.findById(buyer.getId()).orElseThrow().getIsActive());}
    @Test void dismissalAndConcurrentDecisionKeepOneFinalHistory()throws Exception{var r=reports.create(seller.getId(),write());var review=reports.decide(admin.getId(),r.id(),new ReportDecision("IN_REVIEW","Check",0L));var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);try{List<Future<Boolean>> jobs=new ArrayList<>();for(int i=0;i<2;i++)jobs.add(pool.submit(()->{start.await();try{reports.decide(admin.getId(),r.id(),new ReportDecision("DISMISSED","Không đủ căn cứ",review.revision()));return true;}catch(MarketplaceException e){assertEquals("DATA_CONFLICT",e.code);return false;}}));start.countDown();int successes=0;for(var j:jobs)if(j.get(20,TimeUnit.SECONDS))successes++;assertEquals(1,successes);assertEquals(3,reports.history(admin.getId(),r.id()).size());}finally{pool.shutdownNow();}}
    @Test void concurrentSubmissionHasOneReportAndOneReceiptNotification()throws Exception{var w=write();var pool=Executors.newFixedThreadPool(2);try{var a=pool.submit(()->reports.create(seller.getId(),w));var b=pool.submit(()->reports.create(seller.getId(),w));assertEquals(a.get(20,TimeUnit.SECONDS).id(),b.get(20,TimeUnit.SECONDS).id());assertEquals(1,db.queryForObject("select count(*) from order_notification_outbox where recipient_user_id=?",Integer.class,seller.getId()));}finally{pool.shutdownNow();}}
    @Test void storeReportingRemainsSupported(){var r=reports.create(buyer.getId(),new ReportWrite("STORE",store.getId(),"OTHER","Yêu cầu kiểm tra gian hàng"));assertEquals("STORE",reports.detail(admin.getId(),r.id()).targetType());assertNull(reports.detail(admin.getId(),r.id()).customerContext());assertTrue(reports.filtered(admin.getId(),"OPEN",0,"STORE",null).content().stream().anyMatch(v->v.id().equals(r.id())));}
    @Test void receiptAndDecisionNotificationsDiscloseStatusOnly(){var r=reports.create(seller.getId(),write());reports.create(seller.getId(),new ReportWrite("USER",buyer.getId(),"HARASSMENT","Yêu cầu xem xét nội dung giao tiếp.","ORDER",order.getId(),db.queryForObject("select request_key from marketplace_reports where id=?",String.class,r.id())));var review=reports.decide(admin.getId(),r.id(),new ReportDecision("IN_REVIEW","Private admin investigation note",0L));reports.decide(admin.getId(),r.id(),new ReportDecision("DISMISSED","Private admin decision note",review.revision()));var rows=db.queryForList("select subject,body,target_link from order_notification_outbox where recipient_user_id=?",seller.getId());assertEquals(3,rows.size());assertTrue(rows.stream().anyMatch(row->row.get("subject").toString().contains("không đủ cơ sở")));assertTrue(rows.stream().anyMatch(row->row.get("subject").toString().equals("Báo cáo đang được xem xét")));for(var row:rows){assertFalse(row.get("body").toString().contains("Private admin"));assertEquals("/notifications",row.get("target_link"));}assertEquals(0,db.queryForObject("select count(*) from order_notification_outbox where recipient_user_id=?",Integer.class,buyer.getId()));}
}
