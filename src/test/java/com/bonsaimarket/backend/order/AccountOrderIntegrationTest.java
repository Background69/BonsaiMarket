package com.bonsaimarket.backend.order;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import com.bonsaimarket.backend.category.Category;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.profiles.active=order-test",
        "spring.datasource.url=jdbc:h2:mem:account_orders;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class AccountOrderIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager manager;
    @Autowired ObjectMapper json;

    User buyer;
    User otherBuyer;
    Store store;
    Order testOrder;

    void tx(Runnable action) {
        new TransactionTemplate(manager).executeWithoutResult(status -> action.run());
    }

    @BeforeEach
    void setup() {
        for (String table : List.of("reviews", "transactions", "order_items", "orders", "products", "stores", "categories", "users")) {
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
            testOrder.setShippingAddressSnapshot("{\"recipient\":\"Buyer Test\",\"address\":\"123 Main St\"}");
            testOrder.setShippingMethod("STANDARD");
            testOrder.setShippingFee(new BigDecimal("30000.00"));
            testOrder.setSubtotal(new BigDecimal("100000.00"));
            testOrder.setTotal(new BigDecimal("130000.00"));
            testOrder.setCreatedAt(LocalDateTime.now());
            testOrder.setUpdatedAt(LocalDateTime.now());
            em.persist(testOrder);

            OrderItem item = new OrderItem();
            item.setOrder(testOrder);
            item.setProductNameSnapshot("Tùng La Hán");
            item.setUnitPrice(new BigDecimal("100000.00"));
            item.setQuantity(1);
            item.setLineTotal(new BigDecimal("100000.00"));
            em.persist(item);
        });
    }

    private UsernamePasswordAuthenticationToken auth(User user) {
        return UsernamePasswordAuthenticationToken.authenticated(AuthPrincipal.of(user), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
    }

    @Test
    void listOrdersReturnsBuyerOrders() throws Exception {
        mvc.perform(get("/api/account/orders")
                        .with(authentication(auth(buyer))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("BSM-ORDER-TEST-01"))
                .andExpect(jsonPath("$.content[0].total").value(130000.00))
                .andExpect(jsonPath("$.content[0].items[0].productName").value("Tùng La Hán"));
    }

    @Test
    void getOrderDetailReturnsSingleOrder() throws Exception {
        mvc.perform(get("/api/account/orders/" + testOrder.getId())
                        .with(authentication(auth(buyer))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("BSM-ORDER-TEST-01"))
                .andExpect(jsonPath("$.items[0].name").value("Tùng La Hán"))
                .andExpect(jsonPath("$.createdAt").value(org.hamcrest.Matchers.endsWith("Z")))
                .andExpect(jsonPath("$.canCancel").value(true));
    }

    @Test
    void otherBuyerCannotViewForeignOrder() throws Exception {
        mvc.perform(get("/api/account/orders/" + testOrder.getId())
                        .with(authentication(auth(otherBuyer))))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelOrderChangesStatusToCancelled() throws Exception {
        mvc.perform(post("/api/account/orders/" + testOrder.getId() + "/cancel")
                        .with(authentication(auth(buyer)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Đổi ý không mua nữa\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        tx(() -> {
            Order o = em.find(Order.class, testOrder.getId());
            assertEquals(OrderStatus.CANCELLED, o.getStatus());
        });
    }

    private Order order(User owner, OrderStatus state, String code) {
        Order value=new Order();value.setCode(code);value.setBuyer(em.find(User.class,owner.getId()));
        value.setStore(em.find(Store.class,store.getId()));value.setStatus(state);
        value.setSubtotal(BigDecimal.TEN);value.setShippingFee(BigDecimal.ZERO);value.setTotal(BigDecimal.TEN);
        value.setCreatedAt(LocalDateTime.now());value.setUpdatedAt(LocalDateTime.now());em.persist(value);return value;
    }

    @Test
    void summaryCountsBeyondPaginationAndCannotChooseAnotherBuyer() throws Exception {
        tx(()->{
            for(int i=0;i<55;i++)order(buyer,OrderStatus.PENDING,"SUMMARY-"+i);
            order(buyer,OrderStatus.SHIPPING,"SUMMARY-SHIPPING");order(buyer,OrderStatus.COMPLETED,"SUMMARY-COMPLETED");
            order(buyer,OrderStatus.CONFIRMED,"SUMMARY-CONFIRMED");order(otherBuyer,OrderStatus.PENDING,"SUMMARY-FOREIGN");
        });
        mvc.perform(get("/api/account/orders/summary").param("buyerId",otherBuyer.getId().toString()).with(authentication(auth(buyer))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.pendingCount").value(56))
            .andExpect(jsonPath("$.shippingCount").value(1)).andExpect(jsonPath("$.completedCount").value(1))
            .andExpect(jsonPath("$.reviewEligibleCount").value(0)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Cache-Control","no-store"));
        mvc.perform(get("/api/account/orders/summary").with(authentication(auth(otherBuyer))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.pendingCount").value(1));
        mvc.perform(get("/api/account/orders/summary")).andExpect(status().isUnauthorized());
    }

    @Test
    void summaryCountsOnlyCurrentlyEligibleUnreviewedPurchases() throws Exception {
        final Long[] ids=new Long[3];
        tx(()->{
            Category category=new Category();category.setName("Review category");category.setSlug("summary-category");category.setIsActive(true);category.setCreatedAt(LocalDateTime.now());category.setUpdatedAt(LocalDateTime.now());em.persist(category);
            Product product=new Product();product.setName("Reviewable product");product.setSlug("summary-product");product.setPrice(BigDecimal.TEN);product.setStock(10);product.setStatus(ProductStatus.ACTIVE);product.setCategory(category);product.setStore(em.find(Store.class,store.getId()));product.setCreatedAt(LocalDateTime.now());product.setUpdatedAt(LocalDateTime.now());em.persist(product);
            Order owned=em.find(Order.class,testOrder.getId());owned.setStatus(OrderStatus.COMPLETED);
            OrderItem item=em.createQuery("select i from OrderItem i where i.order.id=:id",OrderItem.class).setParameter("id",owned.getId()).getSingleResult();item.setProduct(product);item.setQuantity(3);
            ids[0]=product.getId();ids[1]=category.getId();ids[2]=item.getId();
        });
        mvc.perform(get("/api/account/products/"+ids[0]+"/review-eligibility").with(authentication(auth(buyer))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.purchases.length()").value(1));
        mvc.perform(get("/api/account/orders/summary").with(authentication(auth(buyer))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.reviewEligibleCount").value(1));
        tx(()->em.find(Category.class,ids[1]).setIsActive(false));
        mvc.perform(get("/api/account/orders/summary").with(authentication(auth(buyer))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.reviewEligibleCount").value(0));
        tx(()->{em.find(Category.class,ids[1]).setIsActive(true);var review=new com.bonsaimarket.backend.review.Review();review.setProduct(em.find(Product.class,ids[0]));review.setUser(em.find(User.class,buyer.getId()));review.setOrderItem(em.find(OrderItem.class,ids[2]));review.setRating(5);review.setComment("Review fixture");review.setCreatedAt(LocalDateTime.now());review.setUpdatedAt(LocalDateTime.now());em.persist(review);});
        mvc.perform(get("/api/account/orders/summary").with(authentication(auth(buyer))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.completedCount").value(1)).andExpect(jsonPath("$.reviewEligibleCount").value(0));
    }
}
