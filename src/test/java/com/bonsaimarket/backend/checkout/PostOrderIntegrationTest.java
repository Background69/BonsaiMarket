package com.bonsaimarket.backend.checkout;
import com.bonsaimarket.backend.auth.AuthSettings;
import com.bonsaimarket.backend.marketplace.*;
import com.bonsaimarket.backend.notification.*;
import com.bonsaimarket.backend.returns.OrderReturnService;
import com.bonsaimarket.backend.shipping.*;
import jakarta.persistence.EntityManager;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.profiles.active=post-order-test","spring.datasource.url=jdbc:h2:mem:post-order;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
    "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
    "spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=create-drop","orders.mail.enabled=false"})
@AutoConfigureMockMvc
class PostOrderIntegrationTest extends CheckoutTestFixture {
    static final OrderSmtpSink smtp=new OrderSmtpSink();
    @DynamicPropertySource static void smtp(DynamicPropertyRegistry p){p.add("spring.mail.host",()->"127.0.0.1");p.add("spring.mail.port",smtp::port);p.add("spring.mail.username",()->"");p.add("spring.mail.password",()->"");p.add("spring.mail.properties.mail.smtp.auth",()->false);p.add("spring.mail.properties.mail.smtp.starttls.enable",()->false);p.add("spring.mail.properties.mail.smtp.starttls.required",()->false);p.add("auth.mail-from",()->"post-order@example.invalid");}
    @AfterAll static void stop() throws Exception {smtp.close();}
    @Autowired OrderReturnService returns;@Autowired OrderMailDispatcher mail;@Autowired PlatformTransactionManager manager;@Autowired EntityManager em;@Autowired MarketplaceAccess access;@Autowired ObjectProvider<JavaMailSender> sender;
    CheckoutService.Receipt placed(){return checkout.place(buyer.getId(),body(quote(buyer)));}
    @Test void demoOrderRecipientNeverReachesSmtpSink(){
        var r=placed();
        db.update("update order_notification_outbox set next_attempt_at=? where order_id<>? and status='PENDING'",LocalDateTime.now(ZoneOffset.UTC).plusDays(2),r.orderId());
        db.update("update order_notification_outbox set recipient='seller@bonsai.market' where order_id=?",r.orderId());
        int before=smtp.messages.size();assertTrue(mail.deliverOnce());
        assertEquals("FAILED",db.queryForObject("select status from order_notification_outbox where order_id=?",String.class,r.orderId()));
        assertEquals("DEMO_RECIPIENT_BLOCKED",db.queryForObject("select error_code from order_notification_outbox where order_id=?",String.class,r.orderId()));
        assertEquals(before,smtp.messages.size());
    }
    CheckoutService.Receipt delivered(){var r=placed();management.transition(seller.getId(),false,r.orderId(),"CONFIRMED");management.transition(seller.getId(),false,r.orderId(),"SHIPPING");management.transition(seller.getId(),false,r.orderId(),"DELIVERED");return r;}
    CheckoutService.Receipt shipped(){var r=placed();management.transition(seller.getId(),false,r.orderId(),"CONFIRMED");shipping.create(seller.getId(),r.orderId());return r;}
    String ref(Long id){return db.queryForObject("select client_reference from shipments where order_id=?",String.class,id);}
    String tracking(Long id){return db.queryForObject("select tracking_code from shipments where order_id=?",String.class,id);}
    @Test void outboxRollsBackWithOrderAndNeverEmailsBeforeCommit(){var b=body(quote(buyer));var tx=new TransactionTemplate(manager);tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);assertThrows(IllegalStateException.class,()->tx.execute(s->{var r=checkout.place(buyer.getId(),b);mail.deliverOnce();assertEquals(0,smtp.messages.stream().filter(bytes->new String(bytes,java.nio.charset.StandardCharsets.UTF_8).contains(r.code())).count());throw new IllegalStateException("Rollback owned fixture");}));assertEquals(0,db.queryForObject("select count(*) from orders where buyer_user_id=?",Integer.class,buyer.getId()));assertEquals(1,cart.view(buyer.getId()).quantity());assertEquals(10,stock());}
    @Test void smtpSinkAcceptsUtf8AndIdempotencyDoesNotEnqueueDuplicate() throws Exception {
        var b=body(quote(buyer));var r=checkout.place(buyer.getId(),b);checkout.place(buyer.getId(),b);assertEquals(1,db.queryForObject("select count(*) from order_notification_outbox where order_id=?",Integer.class,r.orderId()));
        for(int n=0;n<100&&!"SMTP_ACCEPTED".equals(db.queryForObject("select status from order_notification_outbox where order_id=?",String.class,r.orderId()));n++)assertTrue(mail.deliverOnce());
        var matching=new ArrayList<MimeMessage>();for(var bytes:smtp.messages){var message=new MimeMessage(jakarta.mail.Session.getInstance(new Properties()),new ByteArrayInputStream(bytes));if(message.getContent().toString().contains(r.code()))matching.add(message);}
        assertEquals(1,matching.size());assertTrue(matching.getFirst().getSubject().contains("Đã nhận đơn đặt hàng"));assertTrue(matching.getFirst().getContent().toString().contains("không xác nhận"));assertFalse(matching.getFirst().getContent().toString().contains("PRIVATE DELIVERY"));assertEquals(buyer.getEmail(),((jakarta.mail.internet.InternetAddress)matching.getFirst().getAllRecipients()[0]).getAddress());
    }
    @Test void ambiguousSmtpAcceptanceIsUnknownAndNotAutomaticallyRetried(){var r=placed();db.update("update order_notification_outbox set next_attempt_at=? where order_id<>? and status='PENDING'",LocalDateTime.now(ZoneOffset.UTC).plusDays(2),r.orderId());smtp.dropAfterData=true;assertTrue(mail.deliverOnce());assertEquals("UNKNOWN",db.queryForObject("select status from order_notification_outbox where order_id=?",String.class,r.orderId()));int before=smtp.messages.size();assertFalse(mail.deliverOnce());assertEquals(before,smtp.messages.size());}
    @Test void knownMissingSmtpConfigurationBacksOffAndStopsAtEight(){var r=placed();db.update("update order_notification_outbox set next_attempt_at=? where order_id<>? and status='PENDING'",LocalDateTime.now(ZoneOffset.UTC).plusDays(2),r.orderId());var missing=new OrderMailDispatcher(em,access,sender,new AuthSettings("","","","http://localhost:5173","http://localhost:5173"),manager,false);assertTrue(missing.deliverOnce());assertEquals("PENDING",db.queryForObject("select status from order_notification_outbox where order_id=?",String.class,r.orderId()));assertFalse(missing.deliverOnce());db.update("update order_notification_outbox set attempts=7,next_attempt_at=? where order_id=?",LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1),r.orderId());assertTrue(missing.deliverOnce());assertEquals("FAILED",db.queryForObject("select status from order_notification_outbox where order_id=?",String.class,r.orderId()));}
    @Test void buyerReturnApprovalDoesNotRestockUntilReceiptAndInspection(){var r=delivered();returns.request(buyer.getId(),r.orderId(),new OrderReturnService.Request("Cây nhận chưa đúng thỏa thuận"));assertEquals(9,stock());returns.approve(seller.getId(),r.orderId());assertEquals(9,stock());code("RETURN_RECEIPT_REQUIRED",()->returns.inspect(seller.getId(),r.orderId(),new OrderReturnService.Inspection(false,"Chưa nhận đủ hàng trả",true)));returns.inspect(seller.getId(),r.orderId(),new OrderReturnService.Inspection(true,"Toàn bộ hàng còn đủ điều kiện bán",true));assertEquals(10,stock());returns.inspect(seller.getId(),r.orderId(),new OrderReturnService.Inspection(true,"Toàn bộ hàng còn đủ điều kiện bán",true));assertEquals(10,stock());assertEquals("EXTERNAL_INTEGRATION_REQUIRED",returns.overview(buyer.getId(),r.orderId(),"account").refundStatus());assertNull(db.queryForObject("select settled_amount from order_payments where order_id=?",BigDecimal.class,r.orderId()));}
    @Test void damagedReturnDoesNotBecomeSellableStock(){var r=delivered();returns.request(buyer.getId(),r.orderId(),new OrderReturnService.Request("Chậu đã bị nứt khi nhận hàng"));returns.approve(seller.getId(),r.orderId());returns.inspect(seller.getId(),r.orderId(),new OrderReturnService.Inspection(true,"Chậu vỡ không đủ điều kiện bán",false));assertEquals(9,stock());code("RETURN_ALREADY_INSPECTED",()->returns.inspect(seller.getId(),r.orderId(),new OrderReturnService.Inspection(true,"Thay đổi kết quả kiểm tra",true)));}
    @Test void carrierReturnedEventAloneNeverRestocks(){var r=shipped();shipping.webhook(shop,tracking(r.orderId()),ref(r.orderId()),"switch_status","returned",Instant.now().plusSeconds(1));assertEquals(9,stock());returns.inspect(seller.getId(),r.orderId(),new OrderReturnService.Inspection(true,"Đã nhận và kiểm tra toàn bộ hàng nguyên vẹn",true));assertEquals(10,stock());}
    @Test void carrierReturnTimeoutIsNeverBlindlyRetried(){var r=shipped();shipping.webhook(shop,tracking(r.orderId()),ref(r.orderId()),"switch_status","delivery_fail",Instant.now().plusSeconds(1));when(provider.returnToSender(shop,tracking(r.orderId()))).thenThrow(new MarketplaceException(503,"CARRIER_UNAVAILABLE","test timeout"));assertThrows(MarketplaceException.class,()->returns.carrierReturn(seller.getId(),r.orderId()));assertEquals("UNKNOWN",returns.overview(seller.getId(),r.orderId(),"seller").request().getCarrierRequestStatus());code("CARRIER_RETURN_RECONCILE",()->returns.carrierReturn(seller.getId(),r.orderId()));verify(provider,times(1)).returnToSender(shop,tracking(r.orderId()));assertEquals(9,stock());}
    @Test void codTransferReportIsReconciledOnlyAsProviderEvidenceAndReplayDedups(){var r=shipped();var at=Instant.now().plusSeconds(1);shipping.codReport(shop,tracking(r.orderId()),ref(r.orderId()),at,100000,at,new BigDecimal("24000"),2);shipping.codReport(shop,tracking(r.orderId()),ref(r.orderId()),at,100000,at,new BigDecimal("24000"),2);assertEquals(1,db.queryForObject("select count(*) from cod_provider_reports where order_id=?",Integer.class,r.orderId()));assertEquals("PENDING_RECONCILIATION",db.queryForObject("select settlement_status from order_payments where order_id=?",String.class,r.orderId()));assertEquals("REQUESTED",db.queryForObject("select collection_status from order_payments where order_id=?",String.class,r.orderId()));assertNull(db.queryForObject("select settled_amount from order_payments where order_id=?",BigDecimal.class,r.orderId()));assertNull(db.queryForObject("select collected_amount from order_payments where order_id=?",BigDecimal.class,r.orderId()));}
    @Test void lateMatchingCodCannotEraseDiscrepancy(){var r=shipped();var at=Instant.now().plusSeconds(1);shipping.codReport(shop,tracking(r.orderId()),ref(r.orderId()),at,99999,at,new BigDecimal("24000"),2);shipping.codReport(shop,tracking(r.orderId()),ref(r.orderId()),at.plusSeconds(1),100000,at,new BigDecimal("24000"),2);assertEquals("DISCREPANCY",db.queryForObject("select settlement_status from order_payments where order_id=?",String.class,r.orderId()));assertEquals("REVIEW_REQUIRED",db.queryForObject("select status from order_payments where order_id=?",String.class,r.orderId()));}
    @Test void returnedAndDeliveredDoNotMeanCodCollectedOrSettled(){var r=shipped();shipping.webhook(shop,tracking(r.orderId()),ref(r.orderId()),"switch_status","delivered",Instant.now().plusSeconds(1));assertEquals("UNKNOWN",db.queryForObject("select settlement_status from order_payments where order_id=?",String.class,r.orderId()));assertEquals("REQUESTED",db.queryForObject("select collection_status from order_payments where order_id=?",String.class,r.orderId()));}
    @Test void returnAndFinancialReportSecurity() throws Exception {var r=delivered();mvc.perform(post("/api/account/orders/"+r.orderId()+"/return").with(as(other)).with(csrf()).contentType("application/json").content("{\"reason\":\"Unauthorized return request\"}")).andExpect(status().isNotFound());mvc.perform(post("/api/account/orders/"+r.orderId()+"/return").with(as(buyer)).contentType("application/json").content("{\"reason\":\"No CSRF request\"}")).andExpect(status().isForbidden());mvc.perform(get("/api/seller/orders/"+r.orderId()+"/cod-reports").with(as(buyer))).andExpect(status().isForbidden());mvc.perform(post("/api/shipping/webhooks/ghn").contentType("application/json").content("{\"ShopID\":1,\"OrderCode\":\"A\",\"ClientOrderCode\":\"B\",\"Type\":\"cod\",\"Status\":\"delivered\",\"Time\":\""+Instant.now()+"\"}")).andExpect(status().isForbidden());}
}
