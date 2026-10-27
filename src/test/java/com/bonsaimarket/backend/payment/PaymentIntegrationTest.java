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
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class PaymentIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager manager;
    @Autowired ObjectMapper json;
    @Autowired PaymentService paymentService;

    User buyer;
    User otherBuyer;
    Store store;
    Order testOrder;

    void tx(Runnable action) {
        new TransactionTemplate(manager).executeWithoutResult(status -> action.run());
    }

    @BeforeEach
    void setup() {
        for (String table : List.of("transactions", "order_items", "orders", "products", "stores", "categories", "users")) {
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
            store.setOwner(buyer);
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
                .andExpect(status().isForbidden());
    }

    @Test
    void mockConfirmPaymentUpdatesTransactionAndOrderToConfirmed() throws Exception {
        var initRes = paymentService.createPayment(buyer.getId(), new PaymentDtos.CreatePaymentRequest(testOrder.getId(), "VNPAY", null, null), "127.0.0.1");
        assertNotNull(initRes.transactionCode());

        mvc.perform(post("/api/account/payments/mock-confirm/" + initRes.transactionCode())
                        .with(authentication(auth(buyer)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.responseCode").value("00"));

        tx(() -> {
            Order updated = em.find(Order.class, testOrder.getId());
            assertEquals(OrderStatus.CONFIRMED, updated.getStatus());
        });
    }

    @Test
    void getBankTransferInfoReturnsVietQr() throws Exception {
        mvc.perform(get("/api/account/payments/banking/" + testOrder.getId())
                        .with(authentication(auth(buyer))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrUrl").exists())
                .andExpect(jsonPath("$.transferContent").exists());
    }
}
