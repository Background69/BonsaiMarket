package com.bonsaimarket.backend.checkout;

import com.bonsaimarket.backend.payment.*;
import com.bonsaimarket.backend.refund.RefundService;
import com.bonsaimarket.backend.shipping.ShippingService;
import com.bonsaimarket.backend.support.BaselineSql;
import java.math.BigDecimal;
import java.sql.DriverManager;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static com.bonsaimarket.backend.shipping.ShippingProvider.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

/** Whole active schema, real Spring transactions, explicitly doubled provider transports. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.profiles.active=provider-test","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=validate","orders.mail.enabled=false","vnpay.enabled=true","vnpay.tmn-code=TESTONLY","vnpay.hash-secret=provider-double-key","server.servlet.session.cookie.secure=false","auth.frontend-origin=http://localhost:5179","auth.public-origin=http://localhost:5179"})
@AutoConfigureMockMvc
class ProviderCheckoutIntegrationTest extends CheckoutTestFixture {
    static java.sql.Connection schema;
    @DynamicPropertySource static void database(DynamicPropertyRegistry r)throws Exception{String url="jdbc:h2:mem:providercheckout;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000";schema=DriverManager.getConnection(url,"sa","");BaselineSql.currentSchema(schema);r.add("spring.datasource.url",()->url);}
    @AfterAll static void close()throws Exception{schema.close();}
    @Autowired PaymentService payments;@Autowired RefundService refunds;@MockitoBean VnpayGateway gateway;
    @org.springframework.boot.test.web.server.LocalServerPort int port;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder passwords;
    @Test @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="BM_PROVIDER_BROWSER",matches="true")
    void browserCheckoutAndReturnUsesRealHttpSessionAndH2WithProviderDoubles()throws Exception{
        String password=UUID.randomUUID().toString();db.update("update users set password_hash=? where id=?",passwords.encode(password),buyer.getId());addresses.create(buyer.getId(),delivery());
        var builder=new ProcessBuilder("node","frontend/test/provider-browser.js").redirectErrorStream(true).redirectOutput(new java.io.File("artifacts/p0-provider-completion-20261113/evidence/browser-run.log"));
        builder.environment().put("BM_LIVE_BACKEND_ORIGIN","http://127.0.0.1:"+port);builder.environment().put("BM_PROVIDER_BUYER",buyer.getEmail());builder.environment().put("BM_PROVIDER_PASSWORD",password);builder.environment().put("BM_PROVIDER_STORE",store.getId().toString());
        var child=builder.start();try{assertTrue(child.waitFor(150,java.util.concurrent.TimeUnit.SECONDS),"Browser timeout");assertEquals(0,child.exitValue(),"See retained browser-run.log");}finally{if(child.isAlive())child.destroyForcibly();}
        assertEquals(1,db.queryForObject("select count(*) from orders where buyer_user_id=?",Integer.class,buyer.getId()));assertEquals("SUCCESS",db.queryForObject("select status from transactions where order_id in (select id from orders where buyer_user_id=?)",String.class,buyer.getId()));assertEquals(9,stock());
    }
    @BeforeEach void configured(){when(gateway.available()).thenReturn(true);when(gateway.refundsAvailable()).thenReturn(true);when(provider.quote(eq(shop),any(),any(),any(),anyInt())).thenReturn(List.of(new Rate(2,"Prepaid double",new BigDecimal("21000"))));}
    CheckoutService.QuoteView onlineQuote(){return checkout.quote(buyer.getId(),new CheckoutService.QuoteInput(store.getId(),checkout.preview(buyer.getId()).fingerprint(),null,delivery(),null,null,"VNPAY")).getFirst();}
    long order(){return checkout.place(buyer.getId(),new CheckoutService.Place(onlineQuote().id(),UUID.randomUUID().toString(),"VNPAY","")).orderId();}
    void pay(long order){String code=payments.createPayment(buyer.getId(),new PaymentDtos.CreatePaymentRequest(order,"VNPAY",null,null),"127.0.0.1").transactionCode();var p=new TreeMap<String,String>(Map.of("vnp_TxnRef",code,"vnp_TmnCode","TESTONLY","vnp_Amount","12100000","vnp_ResponseCode","00","vnp_TransactionStatus","00","vnp_TransactionNo","999","vnp_PayDate","20261113120000"));String data=p.entrySet().stream().map(e->e.getKey()+"="+e.getValue()).collect(java.util.stream.Collectors.joining("&"));p.put("vnp_SecureHash",VNPayUtil.hmacSHA512("provider-double-key",data));assertTrue(payments.processVnpayCallback(p).success());}
    RefundService.Refund approve(long id,BigDecimal amount){var r=refunds.request(buyer.getId(),id,"account",new RefundService.Write("Provider double review",amount)).refund();return refunds.decide(admin.getId(),r.id(),new RefundService.Decision("APPROVED","Approved for isolated test",r.revision()));}
    void refundResponse(String status){when(gateway.refund(anyString(),anyString(),anyString(),anyString(),any(),anyString(),anyLong())).thenReturn(Map.of("vnp_ResponseCode","00","vnp_TransactionStatus",status,"vnp_TransactionNo","888","vnp_ResponseId","response-1"));}
    void inspected(long order){db.update("update orders set status='DELIVERED' where id=?",order);db.update("insert into order_returns(order_id,kind,status,reason,inspection,restock,carrier_request_status,requested_at,received_at,inspected_at,updated_at) values(?,'BUYER_RETURN','INSPECTED','Returned','Inspected',false,'NOT_REQUIRED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",order);}
    @Test void prepaidQuoteDoesNotRequireCodPermissionAndShipmentWaitsForVerifiedPayment(){settings.getCodEnabledStoreIds().remove(store.getId());long id=order();management.transition(seller.getId(),false,id,"CONFIRMED");code("PAYMENT_NOT_READY",()->shipping.create(seller.getId(),id));verify(provider,never()).create(anyInt(),any());pay(id);shipping.create(seller.getId(),id);shipping.create(seller.getId(),id);verify(provider,times(1)).create(eq(shop),argThat(c->c.feePayer()==1&&c.codAmount()==0));assertNotNull(shipping.overview(buyer.getId(),id,ShippingService.Scope.BUYER).shipment().trackingCode());assertEquals(9,stock());}
    @Test void paymentMethodCannotReuseCodQuoteOrFrontendAmount(){var q=quote(buyer);code("PAYMENT_QUOTE_MISMATCH",()->checkout.place(buyer.getId(),new CheckoutService.Place(q.id(),UUID.randomUUID().toString(),"VNPAY","")));assertEquals(10,stock());}
    @Test void fullRefundVerifiedOnceAllowsSeparateCancellation(){long id=order();pay(id);var r=approve(id,null);refundResponse("00");var result=refunds.execute(admin.getId(),r.id(),r.revision());assertEquals("SUCCEEDED",result.status());assertEquals("SUCCEEDED",refunds.execute(admin.getId(),r.id(),r.revision()).status());verify(gateway,times(1)).refund(anyString(),anyString(),anyString(),eq("999"),eq(new BigDecimal("121000.00")),eq("02"),eq(admin.getId()));assertEquals(9,stock());management.cancelByBuyer(buyer.getId(),id);assertEquals(10,stock());}
    @Test void partialRequiresInspectedReturnAndNeverPermitsFullCancellation(){long id=order();pay(id);code("REFUND_RETURN_REQUIRED",()->refunds.request(buyer.getId(),id,"account",new RefundService.Write("Partial",new BigDecimal("10000"))));inspected(id);var r=approve(id,new BigDecimal("10000"));refundResponse("00");assertEquals("SUCCEEDED",refunds.execute(admin.getId(),r.id(),r.revision()).status());verify(gateway).refund(anyString(),anyString(),anyString(),eq("999"),eq(new BigDecimal("10000.00")),eq("03"),eq(admin.getId()));assertEquals("PARTIALLY_REFUNDED",db.queryForObject("select status from order_payments where order_id=?",String.class,id));assertEquals(9,stock());}
    @Test void timeoutKeepsStableIntentAndNeverRetriesRefund(){long id=order();pay(id);var r=approve(id,null);when(gateway.refund(anyString(),anyString(),anyString(),anyString(),any(),anyString(),anyLong())).thenThrow(new RuntimeException("Doubled timeout"));assertEquals("UNKNOWN",refunds.execute(admin.getId(),r.id(),r.revision()).status());assertEquals("UNKNOWN",refunds.execute(admin.getId(),r.id(),r.revision()).status());verify(gateway,times(1)).refund(anyString(),anyString(),anyString(),anyString(),any(),anyString(),anyLong());assertNotNull(db.queryForObject("select provider_request_id from order_refunds where id=?",String.class,r.id()));code("REFUND_REVIEW_REQUIRED",()->management.cancelByBuyer(buyer.getId(),id));}
    @Test void acceptedRefundIsNotCompletionAndBlocksShipment(){long id=order();pay(id);var r=approve(id,null);refundResponse("05");assertEquals("AWAITING_EXTERNAL",refunds.execute(admin.getId(),r.id(),r.revision()).status());management.transition(seller.getId(),false,id,"CONFIRMED");code("PAYMENT_NOT_READY",()->shipping.create(seller.getId(),id));assertEquals("SUCCESS",db.queryForObject("select status from transactions where order_id=?",String.class,id));}
    @Test void refundQueryCannotMistakeOriginalPaymentForRefund(){long id=order();pay(id);var r=approve(id,null);refundResponse("05");r=refunds.execute(admin.getId(),r.id(),r.revision());when(gateway.queryRefund(anyString(),anyString(),anyString(),eq("888"))).thenReturn(Map.of("vnp_ResponseCode","00","vnp_TransactionStatus","00","vnp_TransactionNo","999","vnp_TransactionType","01","vnp_Amount","12100000","vnp_ResponseId","query-1"));assertEquals("AWAITING_EXTERNAL",refunds.reconcile(admin.getId(),r.id(),r.revision()).status());}
    @Test void refundQueryFinalizesOnlyLinkedAmountTypeAndTransaction(){long id=order();pay(id);var r=approve(id,null);refundResponse("05");r=refunds.execute(admin.getId(),r.id(),r.revision());when(gateway.queryRefund(anyString(),anyString(),anyString(),eq("888"))).thenReturn(Map.of("vnp_ResponseCode","00","vnp_TransactionStatus","00","vnp_TransactionNo","888","vnp_TransactionType","02","vnp_Amount","12100000","vnp_ResponseId","query-2"));assertEquals("SUCCEEDED",refunds.reconcile(admin.getId(),r.id(),r.revision()).status());assertEquals("REFUNDED",db.queryForObject("select status from order_payments where order_id=?",String.class,id));}
    @Test void executeRequiresAdminCsrfAndOwnership()throws Exception{long id=order();pay(id);var r=approve(id,null);mvc.perform(post("/api/admin/refunds/"+r.id()+"/execute").with(as(buyer)).with(csrf()).contentType("application/json").content("{\"revision\":1}")).andExpect(status().isForbidden());mvc.perform(post("/api/admin/refunds/"+r.id()+"/execute").with(as(admin)).contentType("application/json").content("{\"revision\":1}")).andExpect(status().isForbidden());mvc.perform(get("/api/account/orders/"+id+"/refund").with(as(other))).andExpect(status().isNotFound());verify(gateway,never()).refund(anyString(),anyString(),anyString(),anyString(),any(),anyString(),anyLong());}
}
