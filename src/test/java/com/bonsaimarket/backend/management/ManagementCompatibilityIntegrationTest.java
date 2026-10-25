package com.bonsaimarket.backend.management;

import com.bonsaimarket.backend.auth.AuthMailSender;
import com.bonsaimarket.backend.auth.AuthPrincipal;
import com.bonsaimarket.backend.category.*;
import com.bonsaimarket.backend.order.*;
import com.bonsaimarket.backend.product.*;
import com.bonsaimarket.backend.store.*;
import com.bonsaimarket.backend.user.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** New management contracts against real JPA/security on isolated H2, never the user's MySQL. */
@SpringBootTest(properties={"spring.profiles.active=merge-compatibility-test",
    "spring.datasource.url=jdbc:h2:mem:merge-compatibility;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
class ManagementCompatibilityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired StoreRepository stores;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired OrderRepository orders;
    @Autowired OrderItemRepository items;
    @MockitoBean AuthMailSender mail;
    User admin, seller, otherSeller, buyer;
    Store store;
    Product product;
    Order order;
    String key;

    @BeforeEach void fixture() {
        key=UUID.randomUUID().toString();
        admin=user(UserRole.ADMIN);seller=user(UserRole.SELLER);otherSeller=user(UserRole.SELLER);buyer=user(UserRole.CUSTOMER);
        store=new Store();store.setOwner(seller);store.setName(key);store.setStatus(StoreStatus.ACTIVE);store=stores.save(store);
        var category=new Category();category.setName(key);category.setSlug(key);category.setIsActive(true);category=categories.save(category);
        product=new Product();product.setName(key);product.setSlug(key);product.setCategory(category);product.setStore(store);
        product.setStatus(ProductStatus.ACTIVE);product.setStock(5);product.setPrice(BigDecimal.TEN);product=products.save(product);
        order=new Order();order.setCode(key);order.setBuyer(buyer);order.setStore(store);order.setStatus(OrderStatus.PENDING);
        order.setSubtotal(new BigDecimal("20"));order.setShippingFee(new BigDecimal("3"));order.setTotal(new BigDecimal("23"));
        order.setCreatedAt(LocalDateTime.of(2026,1,1,17,30));order.setUpdatedAt(order.getCreatedAt());
        order.setShippingMethod("STANDARD");
        order.setShippingAddressSnapshot("{\"recipient\":\"Test recipient\",\"detail\":\"Test address\"}");order=orders.save(order);
        var line=new OrderItem();line.setOrder(order);line.setProduct(product);line.setProductNameSnapshot(key);
        line.setUnitPrice(BigDecimal.TEN);line.setQuantity(2);line.setLineTotal(new BigDecimal("20"));items.save(line);
    }

    User user(UserRole role) {
        var u=new User();u.setName(key);u.setEmail(UUID.randomUUID()+"@example.invalid");u.setRole(role);
        u.setPasswordHash("test-only-unusable-hash");u.setIsActive(true);u.setEmailVerifiedAt(LocalDateTime.of(2026,1,1,0,0));
        return users.save(u);
    }
    RequestPostProcessor as(User u) {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(AuthPrincipal.of(u),null,
            List.of(new SimpleGrantedAuthority("ROLE_"+u.getRole()))));
    }
    String statusPath(boolean adminPath) {return "/api/"+(adminPath?"admin":"seller")+"/orders/"+order.getId()+"/status";}
    void transition(User actor,boolean adminPath,String status) throws Exception {
        mvc.perform(post(statusPath(adminPath)).with(as(actor)).with(csrf()).contentType("application/json")
            .content("{\"status\":\""+status+"\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value(status));
    }

    @Test void privateManagementResponsesAreNotCacheable() throws Exception {
        for(String path:List.of("/api/admin/users","/api/admin/users/"+buyer.getId(),"/api/admin/orders",
                "/api/admin/orders/"+order.getId(),"/api/admin/statistics")) {
            mvc.perform(get(path).with(as(admin))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
        }
        for(String path:List.of("/api/seller/orders","/api/seller/orders/"+order.getId(),"/api/seller/statistics")) {
            mvc.perform(get(path).with(as(seller))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
        }
    }

    @Test void validationAndMissingResourcesKeepSharedJsonContract() throws Exception {
        for(String path:List.of("/api/admin/users?page=-1","/api/admin/users?role=UNKNOWN","/api/admin/orders?status=UNKNOWN",
                "/api/admin/statistics?from=invalid","/api/seller/orders?from=2026-02-02&to=2026-01-01")) {
            mvc.perform(get(path).with(as(admin))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED")).andExpect(header().string("Cache-Control","no-store"));
        }
        mvc.perform(get("/api/admin/users/9223372036854775807").with(as(admin)))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(post(statusPath(true)).with(as(admin)).with(csrf()).contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.status").exists());
        mvc.perform(post(statusPath(true)).with(as(admin)).with(csrf()).contentType("application/json").content("bad json"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test void managementBodyBoundsRejectDeclaredAndUnknownLengthPayloads() throws Exception {
        String large="{\"status\":\"CONFIRMED\",\"ignored\":\""+"x".repeat(32768)+"\"}";
        for(String path:List.of(statusPath(true),statusPath(false),"/api/admin/users/"+buyer.getId()+"/active")) {
            mvc.perform(post(path).with(as(admin)).with(csrf()).contentType("application/json").content(large))
                .andExpect(status().isPayloadTooLarge()).andExpect(jsonPath("$.code").value("REQUEST_TOO_LARGE"));
            // MockMvc derives Content-Length from its body even if the header is removed.
            // Exercise the streaming branch explicitly with unknown length, as for a chunked request.
            var streaming=new org.springframework.mock.web.MockHttpServletRequest("POST",path) {
                @Override public long getContentLengthLong() {return -1;}
                @Override public int getContentLength() {return -1;}
            };
            streaming.setContent(large.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var response=new org.springframework.mock.web.MockHttpServletResponse();
            var reached=new java.util.concurrent.atomic.AtomicBoolean();
            new com.bonsaimarket.backend.marketplace.MarketplaceBodyFilter().doFilter(streaming,response,(request,result)->reached.set(true));
            assertEquals(413,response.getStatus());assertTrue(response.getContentAsString().contains("REQUEST_TOO_LARGE"));assertFalse(reached.get());
        }
        assertEquals(OrderStatus.PENDING,orders.findById(order.getId()).orElseThrow().getStatus());
        assertEquals(5,products.findById(product.getId()).orElseThrow().getStock());
    }

    @Test void roleAndCsrfGuardsProtectEveryManagementGroup() throws Exception {
        for(String path:List.of("/api/admin/users","/api/admin/orders","/api/admin/statistics")) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
            mvc.perform(get(path).with(as(seller))).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/seller/orders").with(as(buyer))).andExpect(status().isForbidden());
        mvc.perform(post(statusPath(false)).with(as(seller)).contentType("application/json").content("{\"status\":\"CONFIRMED\"}"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALID"));
        mvc.perform(post("/api/admin/users/"+buyer.getId()+"/active").with(as(admin)).contentType("application/json").content("{\"active\":false}"))
            .andExpect(status().isForbidden());
    }

    @Test void sellerCannotReadOrTransitionAnotherStoresOrder() throws Exception {
        mvc.perform(get("/api/seller/orders/"+order.getId()).with(as(otherSeller)))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(post(statusPath(false)).with(as(otherSeller)).with(csrf()).contentType("application/json").content("{\"status\":\"CONFIRMED\"}"))
            .andExpect(status().isNotFound());
        assertEquals(OrderStatus.PENDING,orders.findById(order.getId()).orElseThrow().getStatus());
    }

    @Test void sellerDtoOmitsBuyerContactAndRetainsShippingSnapshot() throws Exception {
        mvc.perform(get("/api/seller/orders?search="+key).with(as(seller))).andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].buyerEmail").isEmpty()).andExpect(jsonPath("$.content[0].itemCount").value(1))
            .andExpect(jsonPath("$.content[0].quantity").value(2));
        mvc.perform(get("/api/seller/orders/"+order.getId()).with(as(seller))).andExpect(status().isOk())
            .andExpect(jsonPath("$.buyer.email").isEmpty()).andExpect(jsonPath("$.buyer.phone").isEmpty())
            .andExpect(jsonPath("$.shippingAddress").value(order.getShippingAddressSnapshot()))
            .andExpect(jsonPath("$.createdAt").value("2026-01-01T17:30:00Z"));
    }

    @Test void confirmAndCancelIntegrateInventoryAndAuditAtomically() throws Exception {
        transition(seller,false,"CONFIRMED");
        assertEquals(3,products.findById(product.getId()).orElseThrow().getStock());
        assertEquals(1,jdbc.queryForObject("select count(*) from inventory_debits where order_id=?",Integer.class,order.getId()));
        transition(admin,true,"CANCELLED");
        assertEquals(5,products.findById(product.getId()).orElseThrow().getStock());
        assertEquals(2,jdbc.queryForObject("select count(*) from marketplace_audit where target_type='ORDER' and target_id=?",Integer.class,order.getId()));
        mvc.perform(post(statusPath(true)).with(as(admin)).with(csrf()).contentType("application/json").content("{\"status\":\"CANCELLED\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ORDER_STATE_INVALID"));
        assertEquals(5,products.findById(product.getId()).orElseThrow().getStock());
    }

    @Test void stockFailureRollsBackOrderStatusAndLedger() throws Exception {
        product.setStock(1);products.save(product);
        mvc.perform(post(statusPath(false)).with(as(seller)).with(csrf()).contentType("application/json").content("{\"status\":\"CONFIRMED\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("OUT_OF_STOCK"));
        assertEquals(OrderStatus.PENDING,orders.findById(order.getId()).orElseThrow().getStatus());
        assertEquals(1,products.findById(product.getId()).orElseThrow().getStock());
        assertEquals(0,jdbc.queryForObject("select count(*) from inventory_debits where order_id=?",Integer.class,order.getId()));
    }

    @Test void deactivateInvalidatesExistingAuthPrincipalAcrossModules() throws Exception {
        var previous=as(buyer);
        mvc.perform(post("/api/admin/users/"+buyer.getId()+"/active").with(as(admin)).with(csrf()).contentType("application/json").content("{\"active\":false}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        mvc.perform(get("/api/auth/me").with(previous)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/account/profile").with(previous)).andExpect(status().isUnauthorized());
        var saved=users.findById(buyer.getId()).orElseThrow();assertFalse(saved.getIsActive());assertEquals(buyer.getAuthVersion()+1,saved.getAuthVersion());
    }

    @Test void userRowsUseBoundedPaginationAndNeverSerializeCredentials() throws Exception {
        mvc.perform(get("/api/admin/users?search="+key).with(as(admin))).andExpect(status().isOk())
            .andExpect(jsonPath("$.size").value(20)).andExpect(jsonPath("$.totalElements").value(4))
            .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist()).andExpect(jsonPath("$.content[0].authVersion").doesNotExist());
        mvc.perform(post("/api/admin/users/"+admin.getId()+"/active").with(as(admin)).with(csrf()).contentType("application/json").content("{\"active\":false}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SELF_CHANGE"));
    }

    @Test void sellerRevenueUsesVietnameseDayAndExcludesShippingAndPendingOrders() throws Exception {
        mvc.perform(get("/api/seller/statistics?from=2026-01-02&to=2026-01-02").with(as(seller)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalRevenue").value(0)).andExpect(jsonPath("$.pendingOrders").value(1));
        transition(seller,false,"CONFIRMED");transition(seller,false,"SHIPPING");transition(seller,false,"DELIVERED");
        mvc.perform(get("/api/seller/statistics?from=2026-01-02&to=2026-01-02").with(as(seller)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalRevenue").value(20)).andExpect(jsonPath("$.revenueOrders").value(1))
            .andExpect(jsonPath("$.days[0].date").value("2026-01-02")).andExpect(jsonPath("$.categories[0].revenue").value(20));
        mvc.perform(get("/api/seller/statistics?from=2026-01-02&to=2026-01-02").with(as(otherSeller)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalRevenue").value(0)).andExpect(jsonPath("$.revenueOrders").value(0));
    }
}
