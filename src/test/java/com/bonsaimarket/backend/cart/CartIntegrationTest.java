package com.bonsaimarket.backend.cart;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import com.bonsaimarket.backend.category.Category;
import com.bonsaimarket.backend.checkout.CheckoutService;
import com.bonsaimarket.backend.marketplace.MarketplaceException;
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
import static com.bonsaimarket.backend.cart.CartDtos.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Isolated H2; never uses or modifies the developer's MySQL database. */
@SpringBootTest(properties = {"spring.profiles.active=cart-test",
        "spring.datasource.url=jdbc:h2:mem:cart;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
class CartIntegrationTest {
    @Autowired CartService carts;
    @Autowired CheckoutService checkout;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager manager;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    User buyer, other, seller;
    Category category;
    Store store;
    Product product;

    void tx(Runnable action) {new TransactionTemplate(manager).executeWithoutResult(status -> action.run());}

    @BeforeEach void setup() {
        for (String table : List.of("checkout_attempts", "inventory_debits", "order_items", "orders", "cart_items", "carts",
                "addresses", "products", "stores", "categories", "auth_challenges", "oauth_identities", "users")) {
            jdbc.update("delete from " + table);
        }
        tx(() -> {
            buyer = user(UserRole.CUSTOMER); other = user(UserRole.CUSTOMER); seller = user(UserRole.SELLER);
            category = new Category(); category.setName("Bonsai"); category.setSlug("bonsai"); category.setIsActive(true); em.persist(category);
            store = new Store(); store.setName("Vườn A"); store.setOwner(seller); store.setStatus(StoreStatus.ACTIVE); em.persist(store);
        });
        product = newProduct("tung", "Tùng La Hán", "100000.00", 10);
    }

    User user(UserRole role) {
        var user = new User(); user.setName("Test"); user.setEmail(UUID.randomUUID() + "@example.com"); user.setPasswordHash("unused");
        user.setRole(role); user.setIsActive(true); user.setEmailVerifiedAt(LocalDateTime.now()); em.persist(user); return user;
    }

    Product newProduct(String slug, String name, String price, int stock) {
        final Product[] created = new Product[1];
        tx(() -> {
            var p = new Product(); p.setName(name); p.setSlug(slug); p.setPrice(new BigDecimal(price)); p.setStock(stock);
            p.setStatus(ProductStatus.ACTIVE); p.setStore(em.getReference(Store.class, store.getId()));
            p.setCategory(em.getReference(Category.class, category.getId())); em.persist(p); created[0] = p;
        });
        return created[0];
    }

    RequestPostProcessor as(User user) {return authentication(UsernamePasswordAuthenticationToken.authenticated(AuthPrincipal.of(user), null,
            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))));}
    void error(String code, Runnable operation) {assertEquals(code, assertThrows(MarketplaceException.class, operation::run).code);}
    int count(String table) {return jdbc.queryForObject("select count(*) from " + table, Integer.class);}
    AddRequest add(Product p, int quantity) {return new AddRequest(p.getId(), quantity);}
    View addTo(User who, Product p, int quantity) {return carts.add(who.getId(), add(p, quantity));}
    int quantityInDb() {return jdbc.queryForObject("select quantity from cart_items", Integer.class);}

    @Test void emptyCartHasNoLinesAndNothingIsCreatedByReading() {
        var view = carts.view(buyer.getId());
        assertTrue(view.items().isEmpty()); assertEquals(new BigDecimal("0.00"), view.subtotal()); assertEquals(0, count("carts"));
    }

    @Test void addingSameProductMergesIntoOneLine() {
        addTo(buyer, product, 2);
        var view = addTo(buyer, product, 3);
        assertEquals(1, view.items().size()); assertEquals(5, view.items().getFirst().quantity());
        assertEquals(new BigDecimal("500000.00"), view.subtotal());
        assertEquals(1, count("cart_items")); assertEquals(1, count("carts"));
        var item = view.items().getFirst();
        assertEquals("Tùng La Hán", item.name()); assertEquals(store.getId(), item.storeId()); assertEquals("Vườn A", item.storeName());
        assertEquals(10, item.stock()); assertTrue(item.available());
    }

    @Test void addRejectsMoreThanStockIncludingWhatIsAlreadyInCart() {
        error("OUT_OF_STOCK", () -> addTo(buyer, product, 11));
        assertEquals(0, count("carts"));
        addTo(buyer, product, 6);
        error("OUT_OF_STOCK", () -> addTo(buyer, product, 5));
        assertEquals(6, quantityInDb());
    }

    @Test void addRejectsMissingUnsellableAndSoldOutProducts() {
        error("NOT_FOUND", () -> carts.add(buyer.getId(), new AddRequest(999999L, 1)));
        jdbc.update("update products set status='DRAFT' where id=?", product.getId());
        error("PRODUCT_UNAVAILABLE", () -> addTo(buyer, product, 1));
        jdbc.update("update products set status='ACTIVE', stock=0 where id=?", product.getId());
        error("OUT_OF_STOCK", () -> addTo(buyer, product, 1));
        jdbc.update("update products set stock=10 where id=?", product.getId());
        jdbc.update("update stores set status='INACTIVE' where id=?", store.getId());
        error("PRODUCT_UNAVAILABLE", () -> addTo(buyer, product, 1));
        assertEquals(0, count("cart_items"));
    }

    @Test void unverifiedBuyerCannotUseTheCart() {
        jdbc.update("update users set email_verified_at=null where id=?", buyer.getId());
        error("ACCOUNT_UNAVAILABLE", () -> addTo(buyer, product, 1));
        error("ACCOUNT_UNAVAILABLE", () -> carts.view(buyer.getId()));
    }

    @Test void updateChangesQuantityWithinStockOnly() {
        long id = addTo(buyer, product, 2).items().getFirst().cartItemId();
        var view = carts.updateQuantity(buyer.getId(), id, 7);
        assertEquals(7, view.items().getFirst().quantity()); assertEquals(new BigDecimal("700000.00"), view.subtotal());
        error("OUT_OF_STOCK", () -> carts.updateQuantity(buyer.getId(), id, 11));
        error("VALIDATION_FAILED", () -> carts.updateQuantity(buyer.getId(), id, 0));
        assertEquals(7, quantityInDb());
    }

    @Test void removeDeletesOnlyThatLine() {
        var second = newProduct("mai", "Mai Chiếu Thủy", "50000.00", 4);
        long first = addTo(buyer, product, 1).items().getFirst().cartItemId();
        addTo(buyer, second, 2);
        var view = carts.remove(buyer.getId(), first);
        assertEquals(1, view.items().size()); assertEquals(second.getId(), view.items().getFirst().productId());
        assertEquals(new BigDecimal("100000.00"), view.subtotal());
        error("NOT_FOUND", () -> carts.remove(buyer.getId(), first));
    }

    @Test void anotherBuyersLinesAreInvisibleAndUntouchable() {
        long id = addTo(buyer, product, 2).items().getFirst().cartItemId();
        error("NOT_FOUND", () -> carts.updateQuantity(other.getId(), id, 3));
        error("NOT_FOUND", () -> carts.remove(other.getId(), id));
        assertTrue(carts.view(other.getId()).items().isEmpty());
        assertEquals(2, quantityInDb());
        addTo(other, product, 1);
        assertEquals(2, count("cart_items")); assertEquals(2, count("carts"));
        assertEquals(1, carts.view(other.getId()).items().size());
    }

    @Test void viewFlagsLinesThatCanNoLongerBeBoughtAndTheyCanStillBeRemoved() {
        long id = addTo(buyer, product, 2).items().getFirst().cartItemId();
        jdbc.update("update products set stock=1 where id=?", product.getId());
        var low = carts.view(buyer.getId()).items().getFirst();
        assertEquals(1, low.stock()); assertEquals(2, low.quantity()); assertTrue(low.available());
        error("OUT_OF_STOCK", () -> carts.updateQuantity(buyer.getId(), id, 2));
        assertEquals(1, carts.updateQuantity(buyer.getId(), id, 1).items().getFirst().quantity());
        jdbc.update("update products set stock=0 where id=?", product.getId());
        assertFalse(carts.view(buyer.getId()).items().getFirst().available());
        jdbc.update("update products set stock=10, status='ARCHIVED' where id=?", product.getId());
        assertFalse(carts.view(buyer.getId()).items().getFirst().available());
        error("PRODUCT_UNAVAILABLE", () -> carts.updateQuantity(buyer.getId(), id, 1));
        assertTrue(carts.remove(buyer.getId(), id).items().isEmpty());
    }

    @Test void checkoutReadsTheSameLinesTheCartWrote() {
        long id = addTo(buyer, product, 3).items().getFirst().cartItemId();
        var seen = checkout.cart(buyer.getId()).items();
        assertEquals(1, seen.size()); assertEquals(id, seen.getFirst().cartItemId()); assertEquals(3, seen.getFirst().quantity());
        assertEquals(new BigDecimal("300000.00"), seen.getFirst().lineTotal());
    }

    @Test void concurrentAddsOfOneProductNeverDuplicateTheLine() throws Exception {
        var pool = Executors.newFixedThreadPool(2); var go = new CountDownLatch(1);
        try {
            Callable<View> call = () -> {assertTrue(go.await(5, TimeUnit.SECONDS)); return addTo(buyer, product, 2);};
            var a = pool.submit(call); var b = pool.submit(call); go.countDown();
            a.get(15, TimeUnit.SECONDS); b.get(15, TimeUnit.SECONDS);
            assertEquals(1, count("cart_items")); assertEquals(1, count("carts")); assertEquals(4, quantityInDb());
        } finally {pool.shutdownNow();}
    }

    @Test void httpRequiresSessionCsrfAndValidInput() throws Exception {
        mvc.perform(get("/api/account/cart")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/account/cart/items").with(as(buyer)).contentType("application/json")
                .content(json.writeValueAsString(add(product, 1)))).andExpect(status().isForbidden());
        mvc.perform(post("/api/account/cart/items").with(as(buyer)).with(csrf()).contentType("application/json")
                        .content(json.writeValueAsString(add(product, 0))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(post("/api/account/cart/items").with(as(buyer)).with(csrf()).contentType("application/json")
                .content("{\"productId\":\"abc\",\"quantity\":1}")).andExpect(status().isBadRequest());
        assertEquals(0, count("cart_items"));
    }

    @Test void httpAddUpdateRemoveRoundTripAndOwnership() throws Exception {
        mvc.perform(post("/api/account/cart/items").with(as(buyer)).with(csrf()).contentType("application/json")
                        .content(json.writeValueAsString(add(product, 2))))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].available").value(true)).andExpect(jsonPath("$.subtotal").value(200000));
        long id = jdbc.queryForObject("select id from cart_items", Long.class);
        String body = json.writeValueAsString(new UpdateRequest(5));
        mvc.perform(put("/api/account/cart/items/" + id).with(as(other)).with(csrf()).contentType("application/json").content(body))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(put("/api/account/cart/items/" + id).with(as(buyer)).with(csrf()).contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].quantity").value(5));
        mvc.perform(put("/api/account/cart/items/" + id).with(as(buyer)).with(csrf()).contentType("application/json")
                        .content(json.writeValueAsString(new UpdateRequest(11))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("OUT_OF_STOCK"));
        mvc.perform(delete("/api/account/cart/items/" + id).with(as(other)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(delete("/api/account/cart/items/abc").with(as(buyer)).with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/account/cart/items/" + id).with(as(buyer)).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));
        mvc.perform(get("/api/account/cart").with(as(buyer)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));
    }
}