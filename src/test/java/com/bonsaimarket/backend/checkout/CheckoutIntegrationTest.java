package com.bonsaimarket.backend.checkout;

import com.bonsaimarket.backend.address.*;
import com.bonsaimarket.backend.auth.AuthPrincipal;
import com.bonsaimarket.backend.cart.*;
import com.bonsaimarket.backend.category.Category;
import com.bonsaimarket.backend.management.OrderManagementService;
import com.bonsaimarket.backend.marketplace.MarketplaceException;
import com.bonsaimarket.backend.order.*;
import com.bonsaimarket.backend.product.*;
import com.bonsaimarket.backend.store.*;
import com.bonsaimarket.backend.user.*;
import jakarta.persistence.EntityManager;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import static com.bonsaimarket.backend.checkout.CheckoutDtos.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Isolated H2; never uses or modifies the developer's MySQL database. */
@SpringBootTest(properties = {"spring.profiles.active=checkout-test",
    "spring.datasource.url=jdbc:h2:mem:checkout;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
class CheckoutIntegrationTest {
    @Autowired CheckoutService checkout;
    @Autowired AddressService addresses;
    @Autowired OrderManagementService management;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager manager;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    User buyer, other, seller;
    Product product;
    Store store;
    Long addressId;
    void tx(Runnable action) {new TransactionTemplate(manager).executeWithoutResult(status -> action.run());}

    @BeforeEach void setup() {
        for (String table : List.of("checkout_attempts", "marketplace_audit", "inventory_debits", "order_items", "orders", "cart_items", "carts",
                "addresses", "products", "stores", "categories", "auth_challenges", "oauth_identities", "users")) {
            if (!table.equals("marketplace_audit")) jdbc.update("delete from " + table);
        }
        tx(() -> {
            buyer = user(UserRole.CUSTOMER); other = user(UserRole.CUSTOMER); seller = user(UserRole.SELLER);
            var category = new Category(); category.setName("Bonsai"); category.setSlug("bonsai"); category.setIsActive(true); em.persist(category);
            store = new Store(); store.setName("Vườn A"); store.setOwner(seller); store.setStatus(StoreStatus.ACTIVE); em.persist(store);
            product = new Product(); product.setName("Tùng La Hán"); product.setSlug("tung"); product.setPrice(new BigDecimal("100000.00"));
            product.setStock(10); product.setStatus(ProductStatus.ACTIVE); product.setStore(store); product.setCategory(category); em.persist(product);
        });
        addressId = addresses.create(buyer.getId(), address("12 Đường Số 1")).id();
    }
    User user(UserRole role) {
        var user = new User(); user.setName("Test"); user.setEmail(UUID.randomUUID() + "@example.com"); user.setPasswordHash("unused");
        user.setRole(role); user.setIsActive(true); user.setEmailVerifiedAt(LocalDateTime.now()); em.persist(user); return user;
    }
    AddressDtos.Write address(String detail) {return new AddressDtos.Write("Nhà", "Nguyễn An", "0901234567", "TP HCM", "", "Bến Thành", detail, true);}
    Request buy() {return new Request(Source.BUY_NOW, addressId, product.getId(), 2, "Giao buổi sáng");}
    Quote quote() {return checkout.quote(buyer.getId(), buy());}
    RequestPostProcessor as(User user) {return authentication(UsernamePasswordAuthenticationToken.authenticated(AuthPrincipal.of(user), null,
            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))));}
    void error(String code, Runnable operation) {assertEquals(code, assertThrows(MarketplaceException.class, operation::run).code);}
    int count(String table) {return jdbc.queryForObject("select count(*) from " + table, Integer.class);}
    void cart(Product p, int quantity) {
        tx(() -> {
            var found = em.createQuery("select c from Cart c where c.user.id=:id", Cart.class).setParameter("id", buyer.getId()).getResultList();
            var cart = found.isEmpty() ? new Cart() : found.getFirst();
            if (found.isEmpty()) {cart.setUser(em.getReference(User.class, buyer.getId())); em.persist(cart);}
            var line = new CartItem(); line.setCart(cart); line.setProduct(em.getReference(Product.class, p.getId())); line.setQuantity(quantity); em.persist(line);
        });
    }

    @Test void buyNowCreatesSnapshotAndRetryReturnsSameOrderEvenAfterExpiry() {
        var quote = quote();
        assertEquals(new BigDecimal("230000.00"), quote.review().total());
        var receipt = checkout.submit(buyer.getId(), quote.quoteId());
        assertEquals(1, receipt.orders().size()); assertEquals("PENDING", receipt.orders().getFirst().status());
        assertEquals(1, count("orders")); assertEquals(1, count("order_items")); assertEquals(0, count("inventory_debits"));
        assertEquals(10, jdbc.queryForObject("select stock from products where id=?", Integer.class, product.getId()));
        addresses.update(buyer.getId(), addressId, address("Địa chỉ mới"));
        jdbc.update("update checkout_attempts set expires_at=?", LocalDateTime.now().minusDays(1));
        assertEquals(receipt, checkout.submit(buyer.getId(), quote.quoteId()));
        assertEquals(receipt, checkout.receipt(buyer.getId(), quote.quoteId()));
        assertTrue(jdbc.queryForObject("select shipping_address_snapshot from orders", String.class).contains("12 Đường Số 1"));
        assertEquals(1, count("orders"));
    }

    @Test void sellerConfirmationAndCancellationUseExistingInventoryLedger() {
        var result = checkout.submit(buyer.getId(), quote().quoteId());
        long id = result.orders().getFirst().id();
        management.transition(seller.getId(), false, id, "CONFIRMED");
        assertEquals(8, jdbc.queryForObject("select stock from products where id=?", Integer.class, product.getId()));
        management.transition(seller.getId(), false, id, "CANCELLED");
        assertEquals(10, jdbc.queryForObject("select stock from products where id=?", Integer.class, product.getId()));
    }

    @Test void mixedStoreCartSplitsOrdersAndClearsOnlyBuyersCart() {
        cart(product, 2);
        final Product[] second = new Product[1];
        tx(() -> {
            var s = new Store(); s.setName("Vườn B"); s.setOwner(user(UserRole.SELLER)); s.setStatus(StoreStatus.ACTIVE); em.persist(s);
            var p = new Product(); p.setName("Mai"); p.setSlug("mai"); p.setPrice(new BigDecimal("50000.00")); p.setStock(4);
            p.setStatus(ProductStatus.ACTIVE); p.setStore(s); p.setCategory(em.find(Product.class, product.getId()).getCategory()); em.persist(p); second[0] = p;
            var c = new Cart(); c.setUser(em.getReference(User.class, other.getId())); em.persist(c);
            var i = new CartItem(); i.setCart(c); i.setProduct(p); i.setQuantity(1); em.persist(i);
        });
        cart(second[0], 1);
        var q = checkout.quote(buyer.getId(), new Request(Source.CART, addressId, null, null, ""));
        var result = checkout.submit(buyer.getId(), q.quoteId());
        assertEquals(2, result.orders().size()); assertEquals(new BigDecimal("310000.00"), result.total());
        assertTrue(checkout.cart(buyer.getId()).items().isEmpty()); assertEquals(1, count("cart_items"));
        assertEquals(result, checkout.submit(buyer.getId(), q.quoteId()));
    }

    @Test void priceChangeRequiresNewReviewAndWritesNothing() {
        var q = quote(); jdbc.update("update products set price=110000 where id=?", product.getId());
        error("CHECKOUT_CHANGED", () -> checkout.submit(buyer.getId(), q.quoteId())); assertEquals(0, count("orders"));
    }
    @Test void changedAddressRequiresNewReview() {
        var q = quote(); addresses.update(buyer.getId(), addressId, address("Địa chỉ khác"));
        error("CHECKOUT_CHANGED", () -> checkout.submit(buyer.getId(), q.quoteId())); assertEquals(0, count("orders"));
    }
    @Test void changedCartRequiresNewReviewAndKeepsCart() {
        cart(product, 2);
        var q = checkout.quote(buyer.getId(), new Request(Source.CART, addressId, null, null, ""));
        jdbc.update("update cart_items set quantity=3");
        error("CHECKOUT_CHANGED", () -> checkout.submit(buyer.getId(), q.quoteId())); assertEquals(1, count("cart_items"));
        assertEquals(0, count("orders"));
    }
    @Test void stockAndProductVisibilityAreRecheckedBeforeSubmit() {
        var q = quote(); jdbc.update("update products set stock=1 where id=?", product.getId());
        error("OUT_OF_STOCK", () -> checkout.submit(buyer.getId(), q.quoteId()));
        jdbc.update("update products set stock=10,status='ARCHIVED' where id=?", product.getId());
        error("PRODUCT_UNAVAILABLE", () -> checkout.submit(buyer.getId(), q.quoteId())); assertEquals(0, count("orders"));
    }
    @Test void emptyCartExpiredQuoteAndForeignAddressAreRejected() {
        error("CART_EMPTY", () -> checkout.quote(buyer.getId(), new Request(Source.CART, addressId, null, null, "")));
        error("NOT_FOUND", () -> checkout.quote(other.getId(), buy()));
        var q = quote(); jdbc.update("update checkout_attempts set expires_at=?", LocalDateTime.now().minusDays(1));
        error("QUOTE_EXPIRED", () -> checkout.submit(buyer.getId(), q.quoteId())); assertEquals(0, count("orders"));
    }
    @Test void quoteAndReceiptNeverLeakToAnotherBuyer() {
        var q = quote();
        error("NOT_FOUND", () -> checkout.submit(other.getId(), q.quoteId()));
        error("NOT_FOUND", () -> checkout.savedQuote(other.getId(), q.quoteId()));
        checkout.submit(buyer.getId(), q.quoteId());
        error("NOT_FOUND", () -> checkout.receipt(other.getId(), q.quoteId()));
    }
    @Test void concurrentRetriesCreateOneOrder() throws Exception {
        var q = quote(); var pool = Executors.newFixedThreadPool(2); var go = new CountDownLatch(1);
        try {
            Callable<Receipt> call = () -> {assertTrue(go.await(5, TimeUnit.SECONDS)); return checkout.submit(buyer.getId(), q.quoteId());};
            var a = pool.submit(call); var b = pool.submit(call); go.countDown();
            assertEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)); assertEquals(1, count("orders"));
        } finally {pool.shutdownNow();}
    }
    @Test void differentQuotesForSameCartCannotCreateDuplicateOrders() {
        cart(product, 1); var request = new Request(Source.CART, addressId, null, null, "");
        var a = checkout.quote(buyer.getId(), request); var b = checkout.quote(buyer.getId(), request);
        checkout.submit(buyer.getId(), a.quoteId());
        error("CART_EMPTY", () -> checkout.submit(buyer.getId(), b.quoteId())); assertEquals(1, count("orders"));
    }
    @Test void httpRequiresSessionCsrfAndValidInput() throws Exception {
        mvc.perform(get("/api/account/checkout")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/account/checkout/quote").with(as(buyer)).contentType("application/json").content(json.writeValueAsString(buy())))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/account/checkout/quote").with(as(buyer)).with(csrf()).contentType("application/json")
                .content(json.writeValueAsString(new Request(Source.BUY_NOW, addressId, product.getId(), 0, ""))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/account/checkout/quote").with(as(buyer)).with(csrf()).contentType("application/json").content(json.writeValueAsString(buy())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.review.total").value(230000))
                .andExpect(header().string("Cache-Control", "no-store"));
    }
}
