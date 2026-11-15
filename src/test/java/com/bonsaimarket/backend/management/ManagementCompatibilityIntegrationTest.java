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

/** Kiểm tra contract quản lý qua JPA/security thật trên H2 cô lập, không dùng MySQL của người dùng. */
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
    @Autowired StatisticsService statistics;
    @Autowired jakarta.persistence.EntityManager em;
    @Autowired jakarta.persistence.EntityManagerFactory entityManagerFactory;
    @Autowired com.bonsaimarket.backend.payment.TransactionRepository transactions;
    @Autowired com.bonsaimarket.backend.marketplace.MarketplaceAccess access;
    @Autowired AdminHistory adminHistory;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
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
            // MockMvc tự tính Content-Length từ body kể cả khi xóa header.
            // Gọi nhánh streaming với độ dài chưa biết như request chunked.
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

    @Test void dailyRevenueAggregatesRowsAndKeepsVietnamMidnightAndSellerScope() throws Exception {
        var rows=new java.util.ArrayList<Order>();
        for(int i=0;i<120;i++) rows.add(revenueOrder(LocalDateTime.of(2026,1,1,18,0),new BigDecimal("10.25")));
        rows.add(revenueOrder(LocalDateTime.of(2026,1,1,16,59,59),new BigDecimal("7.50")));
        rows.add(revenueOrder(LocalDateTime.of(2026,1,2,17,0),new BigDecimal("99")));
        orders.saveAll(rows);
        long before=em.createQuery("select o.createdAt,o.subtotal from Order o where o.store.owner.id=:owner and o.status in :statuses and o.createdAt>=:start and o.createdAt<:end",Object[].class)
            .setParameter("owner",seller.getId()).setParameter("statuses",ManagementSupport.REVENUE_STATUSES)
            .setParameter("start",LocalDateTime.of(2026,1,1,17,0)).setParameter("end",LocalDateTime.of(2026,1,2,17,0)).getResultList().size();
        var metrics=entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        boolean enabled=metrics.isStatisticsEnabled();metrics.setStatisticsEnabled(true);metrics.clear();
        try {
            var result=statistics.seller(seller.getId(),"2026-01-02","2026-01-02");
            assertEquals(0,new BigDecimal("1230.00").compareTo(result.totalRevenue()));
            assertEquals(120,result.revenueOrders());assertEquals(120,result.days().getFirst().orders());
            assertEquals(0,statistics.seller(otherSeller.getId(),"2026-01-02","2026-01-02").revenueOrders());
            var query=java.util.Arrays.stream(metrics.getQueries()).filter(q->q.contains("timestampadd(second")).findFirst().orElseThrow();
            long after=metrics.getQueryStatistics(query).getExecutionRowCount();
            assertEquals(120,before);assertEquals(1,after);
            System.out.println("Daily revenue H2 same fixture: raw rows="+before+", grouped rows="+after+", subtotal=1230.00");
        } finally {metrics.setStatisticsEnabled(enabled);}
    }

    @Test void historicalVietnamOffsetChangePreservesDayBuckets() {
        var transition=ManagementSupport.BUSINESS_ZONE.getRules().getTransitions().getLast();
        var instant=transition.getInstant();
        var times=List.of(instant.minusSeconds(1),instant,instant.plusSeconds(3600));
        orders.saveAll(times.stream().map(time->revenueOrder(LocalDateTime.ofInstant(time,java.time.ZoneOffset.UTC),BigDecimal.TEN)).toList());
        var from=ManagementSupport.businessDay(LocalDateTime.ofInstant(instant.minusSeconds(86400),java.time.ZoneOffset.UTC));
        var to=from.plusDays(2);
        var result=statistics.seller(seller.getId(),from.toString(),to.toString());
        assertEquals(3,result.revenueOrders());assertEquals(0,new BigDecimal("30").compareTo(result.totalRevenue()));
        for(var day:result.days()) assertEquals(times.stream().filter(time->ManagementSupport.businessDay(LocalDateTime.ofInstant(time,java.time.ZoneOffset.UTC)).toString().equals(day.date())).count(),day.orders());
    }

    private Order revenueOrder(LocalDateTime time,BigDecimal subtotal) {
        var value=new Order();value.setBuyer(buyer);value.setStore(store);value.setCode(UUID.randomUUID().toString());
        value.setStatus(OrderStatus.DELIVERED);value.setSubtotal(subtotal);value.setShippingFee(BigDecimal.TEN);value.setTotal(subtotal.add(BigDecimal.TEN));
        value.setCreatedAt(time);value.setUpdatedAt(time);return value;
    }

    @Test void adminCatalogUsesBoundedQueriesForManyStoresAndProducts() {
        for(int i=0;i<21;i++) {
            var owner=user(UserRole.SELLER);
            var shop=new Store();shop.setName(key);shop.setOwner(owner);shop.setStatus(StoreStatus.ACTIVE);shop=stores.save(shop);
            var category=new Category();category.setName(key);category.setSlug(UUID.randomUUID().toString());category.setIsActive(true);category=categories.save(category);
            var item=new Product();item.setName(key);item.setSlug(UUID.randomUUID().toString());item.setStore(shop);item.setCategory(category);item.setStatus(ProductStatus.ACTIVE);item.setStock(2);item.setPrice(BigDecimal.TEN);products.save(item);
        }
        var observedJdbc=org.mockito.Mockito.spy(jdbc);
        var service=new AdminCatalogService(em,access,observedJdbc,adminHistory);
        var metrics=entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        boolean enabled=metrics.isStatisticsEnabled();metrics.setStatisticsEnabled(true);
        try {
            new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(tx->{
                em.clear();metrics.clear();
                var shops=service.stores(admin.getId(),0,key,null);
                assertEquals(22,shops.totalElements());assertEquals(20,shops.content().size());
                assertTrue(shops.content().stream().allMatch(s->s.products()==1&&s.orders()==0));
                assertEquals(5,metrics.getPrepareStatementCount(),"actor + page + total + two grouped counts");
                org.mockito.Mockito.verifyNoInteractions(observedJdbc);
                System.out.println("Admin stores H2: 20 rows, SQL="+metrics.getPrepareStatementCount()+" including actor; per-store JDBC counts=0");
                em.clear();metrics.clear();
                var result=service.products(admin.getId(),0,key,null,null);
                assertEquals(22,result.totalElements());assertEquals(20,result.content().size());
                assertTrue(result.content().stream().allMatch(p->p.ownerName().equals(key)&&p.category().equals(key)));
                assertEquals(3,metrics.getPrepareStatementCount(),"actor + page + total, no lazy per-product queries");
                var second=service.stores(admin.getId(),1,key,null);assertEquals(2,second.content().size());
                assertEquals(1,second.content().stream().filter(s->s.orders()==1).count());
                assertTrue(service.stores(admin.getId(),2,key,null).content().isEmpty());
            });
        } finally {metrics.setStatisticsEnabled(enabled);}
    }

    @Test void adminOrderFallbackShowsOnlyLatestAttemptAndAuthoritativePaymentWins() throws Exception {
        for(var status:List.of(com.bonsaimarket.backend.payment.TransactionStatus.SUCCESS,com.bonsaimarket.backend.payment.TransactionStatus.FAILED)) {
            var attempt=new com.bonsaimarket.backend.payment.Transaction();attempt.setOrder(order);attempt.setTransactionCode(UUID.randomUUID().toString());
            attempt.setPaymentMethod("VNPAY");attempt.setAmount(order.getTotal());attempt.setStatus(status);
            attempt.setCreatedAt(order.getCreatedAt());attempt.setUpdatedAt(order.getCreatedAt());transactions.save(attempt);
        }
        String path="/api/admin/orders?search="+key;
        mvc.perform(get(path).with(as(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].payment.status").value("FAILED"))
            .andExpect(jsonPath("$.content[0].payment.latestAttempt").value(true));
        jdbc.update("insert into order_payments(order_id,method,status,amount_due,currency,fee_payer,created_at) values(?, 'COD','PENDING',?,'VND',1,?)",order.getId(),order.getTotal(),order.getCreatedAt());
        mvc.perform(get(path).with(as(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].payment.method").value("COD"))
            .andExpect(jsonPath("$.content[0].payment.latestAttempt").value(false));
        mvc.perform(get(path).with(as(buyer))).andExpect(status().isForbidden());
    }
}
