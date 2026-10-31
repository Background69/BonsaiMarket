package com.bonsaimarket.backend.payment;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import com.bonsaimarket.backend.category.Category;
import com.bonsaimarket.backend.order.*;
import com.bonsaimarket.backend.product.Product;
import com.bonsaimarket.backend.product.ProductStatus;
import com.bonsaimarket.backend.store.Store;
import com.bonsaimarket.backend.store.StoreStatus;
import com.bonsaimarket.backend.user.User;
import com.bonsaimarket.backend.user.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.profiles.active=payment-test",
        "spring.datasource.url=jdbc:h2:mem:payment;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop", "vnpay.enabled=true", "vnpay.tmn-code=TESTONLY", "vnpay.hash-secret=isolated-test-signature-key"
})
@AutoConfigureMockMvc
class PaymentIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager manager;
    @Autowired ObjectMapper json;
    @Autowired PaymentService paymentService;
    @Autowired com.bonsaimarket.backend.management.OrderManagementService management;

    User buyer;
    User otherBuyer;
    Store store;
    Order testOrder;

    void tx(Runnable action) {
        new TransactionTemplate(manager).executeWithoutResult(status -> action.run());
    }

    @BeforeEach
    void setup() {
        for (String table : List.of("transactions", "inventory_debits", "marketplace_audit", "order_notification_outbox", "order_items", "orders", "products", "stores", "categories", "users")) {
            jdbc.update("delete from " + table);
        }

        tx(() -> {
            buyer = new User();
            buyer.setName("Buyer Test");
            buyer.setEmail("buyer@example.com");
            buyer.setPasswordHash("hash");
            buyer.setRole(UserRole.CUSTOMER);
            buyer.setIsActive(true);
            buyer.setEmailVerifiedAt(LocalDateTime.now());
            buyer.setCreatedAt(LocalDateTime.now());
            buyer.setUpdatedAt(LocalDateTime.now());
            em.persist(buyer);

            otherBuyer = new User();
            otherBuyer.setName("Other Buyer");
            otherBuyer.setEmail("other@example.com");
            otherBuyer.setPasswordHash("hash");
            otherBuyer.setRole(UserRole.CUSTOMER);
            otherBuyer.setIsActive(true);
            otherBuyer.setEmailVerifiedAt(LocalDateTime.now());
            otherBuyer.setCreatedAt(LocalDateTime.now());
            otherBuyer.setUpdatedAt(LocalDateTime.now());
            em.persist(otherBuyer);

            store = new Store();
            store.setName("Test Garden");
            store.setOwner(otherBuyer);
            store.setStatus(StoreStatus.ACTIVE);
            store.setCreatedAt(LocalDateTime.now());
            store.setUpdatedAt(LocalDateTime.now());
            em.persist(store);

            testOrder = new Order();
            testOrder.setCode("BSM-ORDER-TEST-01");
            testOrder.setBuyer(buyer);
            testOrder.setStore(store);
            testOrder.setStatus(OrderStatus.PENDING);
            testOrder.setShippingAddressSnapshot("{}");
            testOrder.setShippingMethod("STANDARD");
            testOrder.setShippingFee(new BigDecimal("30000.00"));
            testOrder.setSubtotal(new BigDecimal("100000.00"));
            testOrder.setTotal(new BigDecimal("130000.00"));
            testOrder.setCreatedAt(LocalDateTime.now());
            testOrder.setUpdatedAt(LocalDateTime.now());
            em.persist(testOrder);
            var category=new Category();category.setName("Test");category.setSlug("test");category.setIsActive(true);em.persist(category);
            var product=new Product();product.setName("Test");product.setSlug("test");product.setStore(store);product.setCategory(category);product.setStatus(ProductStatus.ACTIVE);product.setPrice(new BigDecimal("100000"));product.setStock(10);em.persist(product);
            var item=new OrderItem();item.setOrder(testOrder);item.setProduct(product);item.setProductNameSnapshot("Test");item.setUnitPrice(product.getPrice());item.setLineTotal(product.getPrice());item.setQuantity(1);em.persist(item);
        });
    }

    private UsernamePasswordAuthenticationToken auth(User user) {
        return UsernamePasswordAuthenticationToken.authenticated(AuthPrincipal.of(user), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
    }

    @Test
    void initPaymentVnpayCreatesTransactionAndReturnsUrl() throws Exception {
        var req = new PaymentDtos.CreatePaymentRequest(testOrder.getId(), "VNPAY", null, null);

        mvc.perform(post("/api/account/payments/init")
                        .with(authentication(auth(buyer)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionCode").exists())
                .andExpect(jsonPath("$.paymentMethod").value("VNPAY"))
                .andExpect(jsonPath("$.paymentUrl").exists());
    }

    @Test
    void otherBuyerCannotPayForForeignOrder() throws Exception {
        var req = new PaymentDtos.CreatePaymentRequest(testOrder.getId(), "VNPAY", null, null);

        mvc.perform(post("/api/account/payments/init")
                        .with(authentication(auth(otherBuyer)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    void buyerCannotConfirmPaymentWithoutProviderProof() throws Exception {
        var initRes = paymentService.createPayment(buyer.getId(), new PaymentDtos.CreatePaymentRequest(testOrder.getId(), "VNPAY", null, null), "127.0.0.1");
        assertNotNull(initRes.transactionCode());

        mvc.perform(post("/api/account/payments/mock-confirm/" + initRes.transactionCode())
                        .with(authentication(auth(buyer)))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PAYMENT_MOCK_DISABLED"));

        tx(() -> {
            Order updated = em.find(Order.class, testOrder.getId());
            assertEquals(OrderStatus.PENDING, updated.getStatus());
            assertEquals(TransactionStatus.PENDING,em.createQuery("select t from Transaction t",Transaction.class).getSingleResult().getStatus());
        });
    }

    @Test
    void bankTransferWithoutReviewedAccountFailsClosed() throws Exception {
        mvc.perform(get("/api/account/payments/banking/" + testOrder.getId())
                        .with(authentication(auth(buyer))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("BANKING_NOT_CONFIGURED"));
    }

    private String init() {
        return paymentService.createPayment(buyer.getId(),new PaymentDtos.CreatePaymentRequest(testOrder.getId(),"VNPAY",null,"https://untrusted.invalid"),"127.0.0.1").transactionCode();
    }
    private Map<String,String> signed(String code,String amount,String merchant,String response) {
        Map<String,String> params=new TreeMap<>();
        params.put("vnp_TxnRef",code);params.put("vnp_Amount",amount);params.put("vnp_TmnCode",merchant);
        params.put("vnp_ResponseCode",response);params.put("vnp_TransactionStatus",response);params.put("vnp_TransactionNo","123456");
        params.put("vnp_PayDate","20261028123000");
        String data=params.entrySet().stream().map(e->e.getKey()+"="+java.net.URLEncoder.encode(e.getValue(),java.nio.charset.StandardCharsets.US_ASCII)).collect(java.util.stream.Collectors.joining("&"));
        params.put("vnp_SecureHash",VNPayUtil.hmacSHA512("isolated-test-signature-key",data));return params;
    }
    @Test void invalidSignatureCannotMutateTransaction() {
        String code=init();var params=signed(code,"13000000","TESTONLY","00");params.put("vnp_SecureHash","bad");
        assertEquals("INVALID_SIGNATURE",assertThrows(com.bonsaimarket.backend.marketplace.MarketplaceException.class,()->paymentService.processVnpayCallback(buyer.getId(),params)).code);
        assertEquals("PENDING",jdbc.queryForObject("select status from transactions",String.class));
    }
    @Test void signedWrongAmountOrMerchantCannotMarkPaid() {
        String code=init();
        for(var params:List.of(signed(code,"100","TESTONLY","00"),signed(code,"13000000","FOREIGN","00")))
            assertEquals("PAYMENT_AMOUNT_MISMATCH",assertThrows(com.bonsaimarket.backend.marketplace.MarketplaceException.class,()->paymentService.processVnpayCallback(buyer.getId(),params)).code);
        assertEquals("PENDING",jdbc.queryForObject("select status from transactions",String.class));
    }
    @Test void signedSuccessIsIdempotentAndDoesNotConfirmSellerOrder() {
        String code=init();var params=signed(code,"13000000","TESTONLY","00");
        assertTrue(paymentService.processVnpayCallback(buyer.getId(),params).success());
        assertTrue(paymentService.processVnpayCallback(buyer.getId(),params).success());
        assertTrue(paymentService.processVnpayCallback(buyer.getId(),signed(code,"13000000","TESTONLY","24")).success());
        assertEquals("SUCCESS",jdbc.queryForObject("select status from transactions",String.class));
        assertEquals("PENDING",jdbc.queryForObject("select status from orders",String.class));
        assertEquals(9,jdbc.queryForObject("select stock from products",Integer.class));
        assertEquals(1,jdbc.queryForObject("select count(*) from inventory_debits",Integer.class));
    }
    @Test void signedCallbackDoesNotExposeForeignOrder() throws Exception {
        String code=init();
        mvc.perform(post("/api/account/payments/vnpay-callback").with(authentication(auth(otherBuyer))).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(signed(code,"13000000","TESTONLY","00"))))
            .andExpect(status().isNotFound());
        assertEquals("PENDING",jdbc.queryForObject("select status from transactions",String.class));
    }
    @Test void publicIpnRequiresSignatureAndPersistsOnlyVerifiedResult() throws Exception {
        String code=init();
        mvc.perform(get("/api/payments/vnpay/ipn").param("vnp_TxnRef",code)).andExpect(status().isOk()).andExpect(jsonPath("$.RspCode").value("97"));
        var call=get("/api/payments/vnpay/ipn");signed(code,"13000000","TESTONLY","00").forEach(call::param);
        mvc.perform(call).andExpect(status().isOk()).andExpect(jsonPath("$.RspCode").value("00"));
        assertEquals("SUCCESS",jdbc.queryForObject("select status from transactions",String.class));
    }
    @Test void unimplementedMethodsAndDuplicateChargeAreRejected() {
        for(String method:List.of("MOMO","BANKING"))
            assertEquals("PAYMENT_METHOD_UNSUPPORTED",assertThrows(com.bonsaimarket.backend.marketplace.MarketplaceException.class,()->paymentService.createPayment(buyer.getId(),new PaymentDtos.CreatePaymentRequest(testOrder.getId(),method,null,null),"127.0.0.1")).code);
        var response=paymentService.createPayment(buyer.getId(),new PaymentDtos.CreatePaymentRequest(testOrder.getId(),"VNPAY",null,"https://untrusted.invalid"),"127.0.0.1");
        assertFalse(response.paymentUrl().contains("untrusted"));
        assertEquals("PAYMENT_PENDING",assertThrows(com.bonsaimarket.backend.marketplace.MarketplaceException.class,this::init).code);
        assertEquals(1,jdbc.queryForObject("select count(*) from transactions",Integer.class));
    }
    @Test void paymentInitRequiresSessionAndCsrf() throws Exception {
        String body=json.writeValueAsString(new PaymentDtos.CreatePaymentRequest(testOrder.getId(),"VNPAY",null,null));
        mvc.perform(post("/api/account/payments/init").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/account/payments/init").with(authentication(auth(buyer))).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        assertEquals(0,jdbc.queryForObject("select count(*) from transactions",Integer.class));
    }
    @Test void paidOrderRequiresRefundReviewBeforeCancellation() {
        String code=init();paymentService.processVnpayCallback(buyer.getId(),signed(code,"13000000","TESTONLY","00"));
        assertEquals("REFUND_REVIEW_REQUIRED",assertThrows(com.bonsaimarket.backend.marketplace.MarketplaceException.class,()->management.cancelByBuyer(buyer.getId(),testOrder.getId())).code);
        assertEquals("PENDING",jdbc.queryForObject("select status from orders",String.class));
        assertEquals(9,jdbc.queryForObject("select stock from products",Integer.class));
    }
    @Test void latePaymentAfterCancellationRetainsFinancialEvidenceWithoutReopeningOrder() {
        String code=init();management.cancelByBuyer(buyer.getId(),testOrder.getId());
        assertTrue(paymentService.processVnpayCallback(buyer.getId(),signed(code,"13000000","TESTONLY","00")).success());
        assertEquals("CANCELLED",jdbc.queryForObject("select status from orders",String.class));
        assertEquals(10,jdbc.queryForObject("select stock from products",Integer.class));
        assertTrue(jdbc.queryForObject("select notes from transactions",String.class).contains("hoàn tiền"));
    }
}
