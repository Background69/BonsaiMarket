package com.bonsaimarket.backend.management;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import com.bonsaimarket.backend.category.*;
import com.bonsaimarket.backend.marketplace.*;
import com.bonsaimarket.backend.order.*;
import com.bonsaimarket.backend.order.Order;
import com.bonsaimarket.backend.product.*;
import com.bonsaimarket.backend.store.*;
import com.bonsaimarket.backend.user.*;

import java.math.BigDecimal;
import java.nio.file.*;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.profiles.active=admin-operations-test", "spring.datasource.url=jdbc:h2:mem:admin-operations;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000", "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=", "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
class AdminOperationsIntegrationTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate db;
    @Autowired
    UserRepository users;
    @Autowired
    StoreRepository stores;
    @Autowired
    ProductRepository products;
    @Autowired
    CategoryRepository categories;
    @Autowired
    OrderRepository orders;
    @Autowired
    OrderItemRepository items;
    @Autowired
    AdminCatalogService catalog;
    @Autowired
    SupportCaseService support;
    @Autowired
    ProductContentService content;
    @Autowired
    AdminOverviewService overview;
    @Autowired
    com.bonsaimarket.backend.notification.NotificationInbox inbox;
    @Autowired
    com.bonsaimarket.backend.notification.OrderMailDispatcher dispatcher;
    @Autowired
    org.springframework.transaction.PlatformTransactionManager transactions;
    User admin, seller, buyer, stranger;
    Store store;
    Product product;
    Order order;

    @BeforeEach
    void setup() throws Exception {
        try (var c = Objects.requireNonNull(db.getDataSource()).getConnection()) {
            if (!c.getMetaData().getTables(null, null, "ORDER_REFUNDS", null).next())
                com.bonsaimarket.backend.support.BaselineSql.execute(c, com.bonsaimarket.backend.support.BaselineSql.tables("order_refunds", "refund_events"));
        }
        try (var c = Objects.requireNonNull(db.getDataSource()).getConnection()) {
            if (!c.getMetaData().getTables(null, null, "SUPPORT_CASES", null).next())
                com.bonsaimarket.backend.support.BaselineSql.execute(c, Files.readString(Path.of("src/main/resources/db/migration/V3__admin_support_cases.sql")));
        }
        admin = user(UserRole.ADMIN);
        seller = user(UserRole.SELLER);
        buyer = user(UserRole.CUSTOMER);
        stranger = user(UserRole.CUSTOMER);
        store = new Store();
        store.setOwner(seller);
        store.setName("Vườn " + UUID.randomUUID());
        store.setStatus(StoreStatus.ACTIVE);
        store.setCreatedAt(LocalDateTime.now());
        store = stores.save(store);
        var category = new Category();
        category.setName("Cây");
        category.setSlug(UUID.randomUUID().toString());
        category.setIsActive(true);
        category = categories.save(category);
        product = new Product();
        product.setName("Tùng " + UUID.randomUUID());
        product.setSlug(UUID.randomUUID().toString());
        product.setCategory(category);
        product.setStore(store);
        product.setStatus(ProductStatus.ACTIVE);
        product.setStock(10);
        product.setPrice(BigDecimal.TEN);
        product.setCreatedAt(LocalDateTime.now());
        product = products.save(product);
        order = new Order();
        order.setBuyer(buyer);
        order.setStore(store);
        order.setCode("TEST-" + UUID.randomUUID());
        order.setStatus(OrderStatus.PENDING);
        order.setSubtotal(BigDecimal.TEN);
        order.setTotal(BigDecimal.TEN);
        order.setShippingFee(BigDecimal.ZERO);
        order.setCreatedAt(LocalDateTime.now());
        order = orders.save(order);
        var line = new OrderItem();
        line.setOrder(order);
        line.setProduct(product);
        line.setQuantity(1);
        line.setUnitPrice(BigDecimal.TEN);
        line.setLineTotal(BigDecimal.TEN);
        line.setProductNameSnapshot(product.getName());
        items.save(line);
    }

    User user(UserRole role) {
        var u = new User();
        u.setName("Người dùng thử");
        u.setEmail(UUID.randomUUID() + "@example.invalid");
        u.setRole(role);
        u.setIsActive(true);
        u.setPasswordHash("test-unusable-hash");
        u.setCreatedAt(LocalDateTime.now());
        u.setEmailVerifiedAt(LocalDateTime.now());
        return users.save(u);
    }

    RequestPostProcessor as(User u) {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(AuthPrincipal.of(u), null, List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole()))));
    }

    String decision(String action, long revision) {
        return "{\"action\":\"" + action + "\",\"revision\":" + revision + ",\"reason\":\"Kiểm tra vi phạm\"}";
    }

    @Test
    void buyerAndSellerCannotCallAdminApisAndCsrfIsRequired() throws Exception {
        for (var path : List.of("/api/admin/overview", "/api/admin/products", "/api/admin/stores", "/api/admin/payment-records", "/api/admin/audit", "/api/admin/disputes")) {
            mvc.perform(get(path).with(as(buyer))).andExpect(status().isForbidden());
            mvc.perform(get(path).with(as(seller))).andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/admin/products/" + product.getId() + "/moderation").with(as(admin)).contentType("application/json").content(decision("HIDE", 0))).andExpect(status().isForbidden());
    }

    @Test
    void moderationBlocksCatalogAndPreservesOrderPriceAndStock() throws Exception {
        var hidden = catalog.moderate(admin.getId(), product.getId(), "HIDE", 0, "Sản phẩm cần xem xét");
        assertEquals("REJECTED", hidden.product().status());
        mvc.perform(get("/api/products/" + product.getId())).andExpect(status().isNotFound());
        assertEquals(1, db.queryForObject("select count(*) from order_items where order_id=?", Long.class, order.getId()));
        assertEquals(10, products.findById(product.getId()).orElseThrow().getStock());
        assertEquals(0, orders.findById(order.getId()).orElseThrow().getTotal().compareTo(BigDecimal.TEN));
        assertTrue(hidden.actions().contains("RESTORE"));
        catalog.moderate(admin.getId(), product.getId(), "RESTORE", 1, "Đã kiểm tra thông tin");
        assertEquals(ProductStatus.ACTIVE, products.findById(product.getId()).orElseThrow().getStatus());
    }

    @Test
    void sellerContentCannotReactivateAdminRestriction() throws Exception {
        catalog.moderate(admin.getId(), product.getId(), "HIDE", 0, "Chờ kiểm duyệt");
        mvc.perform(put("/api/seller/products/" + product.getId() + "/content").with(as(seller)).with(csrf()).contentType("application/json").content("{\"description\":\"Mô tả mới\",\"specifications\":{},\"revision\":1}")).andExpect(status().isOk());
        assertEquals(ProductStatus.REJECTED, products.findById(product.getId()).orElseThrow().getStatus());
        mvc.perform(post("/api/account/cart/items").with(as(buyer)).with(csrf()).contentType("application/json").content("{\"productId\":" + product.getId() + ",\"quantity\":1}")).andExpect(status().isConflict());
    }

    @Test
    void moderationConflictAndInvalidActionRollback() throws Exception {
        catalog.moderate(admin.getId(), product.getId(), "HIDE", 0, "Lý do");
        assertThrows(MarketplaceException.class, () -> catalog.moderate(admin.getId(), product.getId(), "RESTORE", 0, "Lý do"));
        mvc.perform(post("/api/admin/products/" + product.getId() + "/moderation").with(as(admin)).with(csrf()).contentType("application/json").content(decision("SET_PRICE", 1))).andExpect(status().isConflict());
        assertEquals(ProductStatus.REJECTED, products.findById(product.getId()).orElseThrow().getStatus());
    }

    @Test
    void concurrentModerationHasSingleDecision() throws Exception {
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var tasks = new ArrayList<Future<Boolean>>();
            for (int i = 0; i < 2; i++)
                tasks.add(pool.submit(() -> {
                    start.await();
                    try {
                        catalog.moderate(admin.getId(), product.getId(), "HIDE", 0, "Kiểm tra đồng thời");
                        return true;
                    } catch (MarketplaceException e) {
                        assertEquals("DATA_CONFLICT", e.code);
                        return false;
                    }
                }));
            start.countDown();
            int count = 0;
            for (var f : tasks) if (f.get(15, TimeUnit.SECONDS)) count++;
            assertEquals(1, count);
        }
        assertEquals(1, db.queryForObject("select count(*) from marketplace_audit where target_type='PRODUCT' and target_id=?", Long.class, product.getId()));
    }

    @Test
    void suspensionKeepsOrdersAndCannotBeBypassedBySeller() throws Exception {
        var d = catalog.lifecycle(admin.getId(), store.getId(), "INACTIVE", 0, "Đình chỉ kiểm tra");
        assertEquals("INACTIVE", d.store().status());
        assertEquals(OrderStatus.PENDING, orders.findById(order.getId()).orElseThrow().getStatus());
        mvc.perform(post("/api/account/cart/items").with(as(buyer)).with(csrf()).contentType("application/json").content("{\"productId\":" + product.getId() + ",\"quantity\":1}")).andExpect(status().isConflict());
        assertThrows(MarketplaceException.class, () -> catalog.lifecycle(seller.getId(), store.getId(), "ACTIVE", 1, "Không được phép"));
        catalog.lifecycle(admin.getId(), store.getId(), "ACTIVE", 1, "Đã kiểm tra");
        assertEquals(StoreStatus.ACTIVE, stores.findById(store.getId()).orElseThrow().getStatus());
    }

    @Test
    void supportHasParticipantPrivacyAndIdempotency() throws Exception {
        var c = support.open(buyer.getId(), order.getId(), "Cần đối chiếu chất lượng");
        long id = c.support().id();
        assertEquals(id, support.open(buyer.getId(), order.getId(), "Cần đối chiếu chất lượng").support().id());
        mvc.perform(get("/api/account/disputes/" + id).with(as(stranger))).andExpect(status().isNotFound());
        assertThrows(MarketplaceException.class, () -> support.open(stranger.getId(), order.getId(), "Không thuộc đơn"));
        String key = UUID.randomUUID().toString();
        var replied = support.respond(seller.getId(), false, id, 0, "Chứng cứ từ người bán", key);
        assertEquals(2, replied.events().size());
        assertEquals(2, support.respond(seller.getId(), false, id, 0, "Chứng cứ từ người bán", key).events().size());
        assertThrows(MarketplaceException.class, () -> support.respond(seller.getId(), false, id, 1, "Nội dung khác", key));
    }

    @Test
    void supportDecisionHasNoFinancialEffectsAndClosedRefusesResponses() {
        var c = support.open(buyer.getId(), order.getId(), "Cần hỗ trợ");
        long id = c.support().id();
        assertThrows(MarketplaceException.class, () -> support.decide(admin.getId(), id, 0, "RESOLVED", "Sai bước"));
        support.decide(admin.getId(), id, 0, "IN_REVIEW", "Đang xem xét");
        var resolved = support.decide(admin.getId(), id, 1, "RESOLVED", "Đã hướng dẫn các bên");
        assertFalse(resolved.canRespond());
        assertThrows(MarketplaceException.class, () -> support.respond(buyer.getId(), false, id, 2, "Không nhận thêm", UUID.randomUUID().toString()));
        assertEquals(OrderStatus.PENDING, orders.findById(order.getId()).orElseThrow().getStatus());
        assertEquals(0, db.queryForObject("select count(*) from transactions where order_id=?", Long.class, order.getId()));
        assertEquals(10, products.findById(product.getId()).orElseThrow().getStock());
        assertEquals(3, resolved.events().size());
    }

    @Test
    void overviewCountsCurrentQueuesAndSafeReadDtos() throws Exception {
        mvc.perform(get("/api/admin/products/" + product.getId()).with(as(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.product.price").value(10)).andExpect(jsonPath("$.product.passwordHash").doesNotExist());
        var before = overview.get(admin.getId(), "", "");
        support.open(buyer.getId(), order.getId(), "Đối chiếu");
        var after = overview.get(admin.getId(), "", "");
        assertEquals(before.queue().get(3).count() + 1, after.queue().get(3).count());
        assertTrue(after.developmentData());
        mvc.perform(get("/api/admin/payment-records").with(as(admin))).andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void supportStaleRevisionAndForeignCreateLeaveNoEvents() {
        var c = support.open(buyer.getId(), order.getId(), "Cần kiểm tra");
        long id = c.support().id();
        support.respond(seller.getId(), false, id, 0, "Phản hồi", UUID.randomUUID().toString());
        assertThrows(MarketplaceException.class, () -> support.respond(buyer.getId(), false, id, 0, "Bản cũ", UUID.randomUUID().toString()));
        assertEquals(2, support.detail(buyer.getId(), false, id).events().size());
        assertThrows(MarketplaceException.class, () -> support.decide(seller.getId(), id, 1, "IN_REVIEW", "Không có quyền"));
    }

    @Test
    void notificationRecipientReadStatePrivacyAndMailStateAreSeparate() throws Exception {
        support.open(buyer.getId(), order.getId(), "Cần đối chiếu đơn");
        var notifications = inbox.list(admin.getId(), true, 0, "");
        var item = notifications.content().stream().filter(i -> i.link().startsWith("/admin/disputes/")).findFirst().orElseThrow();
        assertNull(item.readAt());
        mvc.perform(post("/api/account/notifications/" + item.id() + "/read").with(as(stranger)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/notifications").with(as(buyer))).andExpect(status().isForbidden());
        long before = inbox.count(admin.getId(), true).get("unread");
        var read = inbox.read(admin.getId(), true, item.id());
        assertNotNull(read.readAt());
        assertEquals(read.readAt(), inbox.read(admin.getId(), true, item.id()).readAt());
        assertEquals(before - 1, inbox.count(admin.getId(), true).get("unread"));
        assertEquals("PENDING", db.queryForObject("select status from order_notification_outbox where id=?", String.class, item.id()));
        assertFalse(dispatcher.deliverOnce());
        mvc.perform(get("/api/admin/notifications").with(as(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].recipient").doesNotExist()).andExpect(jsonPath("$.content[0].errorCode").doesNotExist());
    }

    @Test
    void businessAuditAndNotificationsRollbackTogether() {
        long before = db.queryForObject("select count(*) from order_notification_outbox", Long.class);
        var tx = new org.springframework.transaction.support.TransactionTemplate(transactions);
        tx.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
        tx.executeWithoutResult(s -> {
            support.open(buyer.getId(), order.getId(), "Rollback hồ sơ");
            s.setRollbackOnly();
        });
        assertEquals(0, db.queryForObject("select count(*) from support_cases where order_id=?", Long.class, order.getId()));
        assertEquals(before, db.queryForObject("select count(*) from order_notification_outbox", Long.class));
    }

    @Test
    void notificationConcurrentReadIsIdempotent() throws Exception {
        support.open(buyer.getId(), order.getId(), "Kiểm tra đã đọc");
        var item = inbox.list(admin.getId(), true, 0, "true").content().getFirst();
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> inbox.read(admin.getId(), true, item.id()));
            var b = pool.submit(() -> inbox.read(admin.getId(), true, item.id()));
            assertEquals(a.get(15, TimeUnit.SECONDS).readAt(), b.get(15, TimeUnit.SECONDS).readAt());
        }
        assertEquals(1, db.queryForObject("select count(*) from order_notification_outbox where id=? and read_at is not null", Long.class, item.id()));
    }

    @Test
    void inactiveAdminsAreNotNotificationRecipients() {
        var inactive = user(UserRole.ADMIN);
        inactive.setIsActive(false);
        users.save(inactive);
        support.open(buyer.getId(), order.getId(), "Phân phối người nhận");
        assertEquals(0, db.queryForObject("select count(*) from order_notification_outbox where recipient_user_id=?", Long.class, inactive.getId()));
    }

    @Test
    void paymentReadSummaryAndMandatoryRevisionAreAuthorized() throws Exception {
        mvc.perform(get("/api/admin/payment-records/summary").with(as(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.total").isNumber());
        mvc.perform(get("/api/admin/payment-records/summary").with(as(buyer))).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/products/" + product.getId() + "/moderation").with(as(admin)).with(csrf()).contentType("application/json").content("{\"action\":\"HIDE\",\"reason\":\"Thiếu phiên bản\"}")).andExpect(status().isBadRequest());
        assertEquals(ProductStatus.ACTIVE, products.findById(product.getId()).orElseThrow().getStatus());
    }
}
