package com.bonsaimarket.backend.marketplace;

import com.bonsaimarket.backend.address.*;
import com.bonsaimarket.backend.auth.AuthPrincipal;
import com.bonsaimarket.backend.inventory.*;
import com.bonsaimarket.backend.product.*;
import com.bonsaimarket.backend.store.*;
import com.bonsaimarket.backend.category.*;
import com.bonsaimarket.backend.user.*;
import com.bonsaimarket.backend.order.*;
import com.bonsaimarket.backend.order.Order;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Repository, khóa dòng, giao dịch và kết nối song song thật chỉ trên H2 cô lập. */
@SpringBootTest(properties={"spring.profiles.active=marketplace-test", "spring.datasource.url=jdbc:h2:mem:accountinventory;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop", "spring.datasource.hikari.transaction-isolation=TRANSACTION_REPEATABLE_READ"})
@AutoConfigureMockMvc
class MarketplaceIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AddressService addresses;
    @Autowired InventoryService inventory;
    @Autowired InventoryDebitRepository debits;
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired StoreRepository stores;
    @Autowired CategoryRepository categories;
    @Autowired OrderRepository orders;
    @Autowired OrderItemRepository items;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager manager;
    User buyer,other,seller,otherSeller,admin; Store store; Category category;
    @BeforeEach void setup() {
        for(String table:List.of("inventory_debits","order_items","orders","cart_items","carts","addresses","reviews","wishlist_items","products","stores","categories","auth_challenges","oauth_identities","users")) jdbc.update("delete from "+table);
        buyer=user(UserRole.CUSTOMER);other=user(UserRole.CUSTOMER);seller=user(UserRole.SELLER);otherSeller=user(UserRole.SELLER);admin=user(UserRole.ADMIN);
        store=new Store();store.setName("Store");store.setStatus(StoreStatus.ACTIVE);store.setOwner(seller);stores.save(store);
        category=new Category();category.setName("Bonsai");category.setSlug("bonsai");category.setIsActive(true);categories.save(category);
    }
    User user(UserRole role) {var u=new User();u.setName("User");u.setEmail(UUID.randomUUID()+"@example.com");u.setPasswordHash("hash");u.setRole(role);u.setIsActive(true);u.setEmailVerifiedAt(LocalDateTime.now());return users.save(u);}
    RequestPostProcessor as(User user) {return authentication(UsernamePasswordAuthenticationToken.authenticated(AuthPrincipal.of(user),null,List.of(new SimpleGrantedAuthority("ROLE_"+user.getRole()))));}
    AddressDtos.Write address(boolean isDefault) {return new AddressDtos.Write("Nhà","Nguyễn An","0901234567","TP Hồ Chí Minh","","Phường Bến Thành","12 Đường Số 1",isDefault);}
    static final String BODY="""
        {"label":"Nhà","recipientName":"Nguyễn An","phone":"0901234567","provinceName":"TP Hồ Chí Minh","wardName":"Phường Bến Thành","addressDetail":"12 Đường Số 1","isDefault":false}
        """;
    TransactionTemplate transaction() {var template=new TransactionTemplate(manager);template.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);return template;}
    void tx(Runnable action) {transaction().executeWithoutResult(s->action.run());}
    MarketplaceException failure(Runnable action,String code) {var e=assertThrows(MarketplaceException.class,action::run);assertEquals(code,e.code);return e;}
    Product product(int stock) {var p=new Product();p.setName("Bonsai");p.setSlug(UUID.randomUUID().toString());p.setStock(stock);p.setPrice(BigDecimal.TEN);p.setStore(store);p.setCategory(category);p.setStatus(ProductStatus.ACTIVE);p.setCreatedAt(LocalDateTime.now());return products.save(p);}
    Order order(Product p,int quantity) {var o=new Order();o.setCode(UUID.randomUUID().toString());o.setBuyer(buyer);o.setStore(store);o.setShippingAddressSnapshot("immutable snapshot");o.setShippingMethod("STANDARD");o.setShippingFee(BigDecimal.ZERO);o.setSubtotal(BigDecimal.TEN.multiply(BigDecimal.valueOf(quantity)));o.setTotal(o.getSubtotal());o.setStatus(OrderStatus.CONFIRMED);orders.save(o);line(o,p,quantity);return o;}
    void line(Order o,Product p,int quantity) {var i=new OrderItem();i.setOrder(o);i.setProduct(p);i.setProductNameSnapshot(p.getName());i.setUnitPrice(BigDecimal.TEN);i.setQuantity(quantity);i.setLineTotal(BigDecimal.TEN.multiply(BigDecimal.valueOf(quantity)));items.save(i);}
    int stock(Product p) {return products.findById(p.getId()).orElseThrow().getStock();}
    void cancel(Order o) {tx(()->{var saved=orders.findById(o.getId()).orElseThrow();saved.setStatus(OrderStatus.CANCELLED);inventory.restoreCancelledOrder(o.getId());});}
    List<String> parallel(Runnable a,Runnable b) throws Exception {
        var ready=new CountDownLatch(2);var go=new CountDownLatch(1);var executor=Executors.newFixedThreadPool(2);
        try {
            var futures=new ArrayList<Future<String>>();for(var action:List.of(a,b)) futures.add(executor.submit(()->{ready.countDown();assertTrue(go.await(5,TimeUnit.SECONDS));try {action.run();return "OK";}catch(MarketplaceException e){return e.code;}}));
            assertTrue(ready.await(5,TimeUnit.SECONDS));go.countDown();return List.of(futures.get(0).get(15,TimeUnit.SECONDS),futures.get(1).get(15,TimeUnit.SECONDS));
        } finally {go.countDown();executor.shutdownNow();}
    }
    @Test void addressCrudUsesSessionOwnerAndDoesNotExposeEntity() throws Exception {
        mvc.perform(post("/api/account/addresses").with(as(buyer)).with(csrf()).contentType("application/json").content(BODY.replace("false}","false,\"userId\":"+other.getId()+"}")))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.isDefault").value(true)).andExpect(jsonPath("$.user").doesNotExist());
        var id=addresses.list(buyer.getId()).getFirst().id();assertTrue(addresses.list(other.getId()).isEmpty());
        mvc.perform(get("/api/account/addresses").with(as(buyer))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(put("/api/account/addresses/"+id).with(as(buyer)).with(csrf()).contentType("application/json").content(BODY.replace("Nguyễn An","Trần Bình"))).andExpect(status().isOk()).andExpect(jsonPath("$.recipientName").value("Trần Bình"));
        mvc.perform(delete("/api/account/addresses/"+id).with(as(buyer)).with(csrf())).andExpect(status().isNoContent());assertTrue(addresses.list(buyer.getId()).isEmpty());
    }
    @Test void firstDefaultSwitchAndDeletePromoteLowestId() {
        var a=addresses.create(buyer.getId(),address(false));var b=addresses.create(buyer.getId(),address(false));assertTrue(a.isDefault());assertFalse(b.isDefault());
        addresses.makeDefault(buyer.getId(),b.id());assertEquals(b.id(),addresses.list(buyer.getId()).stream().filter(AddressDtos.View::isDefault).findFirst().orElseThrow().id());
        addresses.delete(buyer.getId(),b.id());assertTrue(addresses.list(buyer.getId()).getFirst().isDefault());
    }
    @Test void updateAndNewDefaultKeepSingleDefault() {
        var a=addresses.create(buyer.getId(),address(false));var b=addresses.create(buyer.getId(),address(true));
        addresses.update(buyer.getId(),a.id(),address(true));assertEquals(1,addresses.list(buyer.getId()).stream().filter(AddressDtos.View::isDefault).count());
        assertFalse(addresses.list(buyer.getId()).stream().filter(x->x.id().equals(b.id())).findFirst().orElseThrow().isDefault());
    }
    @Test void addressOwnershipIsolationForAllMutations() throws Exception {
        var a=addresses.create(buyer.getId(),address(false));
        mvc.perform(get("/api/account/addresses").with(as(other))).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(put("/api/account/addresses/"+a.id()).with(as(other)).with(csrf()).contentType("application/json").content(BODY)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/account/addresses/"+a.id()).with(as(other)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(patch("/api/account/addresses/"+a.id()+"/default").with(as(other)).with(csrf())).andExpect(status().isNotFound());
        assertEquals(1,addresses.list(buyer.getId()).size());
    }
    @Test void addressAuthCsrfAndPhoneLengthValidation() throws Exception {
        mvc.perform(get("/api/account/addresses")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/account/addresses").with(csrf()).contentType("application/json").content(BODY)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/account/addresses").with(as(buyer)).contentType("application/json").content(BODY)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALID"));
        for(String body:List.of(BODY.replace("0901234567","invalid"),BODY.replace("Nguyễn An"," "),BODY.replace("12 Đường Số 1","x".repeat(501)),BODY.replace("Phường Bến Thành","")))
            mvc.perform(post("/api/account/addresses").with(as(buyer)).with(csrf()).contentType("application/json").content(body)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        assertTrue(addresses.list(buyer.getId()).isEmpty());
    }
    @Test void concurrentAddressCreatesHaveExactlyOneDefault() throws Exception {
        assertEquals(List.of("OK","OK"),parallel(()->addresses.create(buyer.getId(),address(true)),()->addresses.create(buyer.getId(),address(true))));
        assertEquals(2,addresses.list(buyer.getId()).size());assertEquals(1,addresses.list(buyer.getId()).stream().filter(AddressDtos.View::isDefault).count());
    }
    @Test void concurrentDefaultChangesRemainUnique() throws Exception {
        var a=addresses.create(buyer.getId(),address(false));var b=addresses.create(buyer.getId(),address(false));
        parallel(()->addresses.makeDefault(buyer.getId(),a.id()),()->addresses.makeDefault(buyer.getId(),b.id()));
        assertEquals(1,addresses.list(buyer.getId()).stream().filter(AddressDtos.View::isDefault).count());
    }
    @Test void shippingSnapshotSurvivesEditAndDeleteAndRejectsOtherOwner() {
        var a=addresses.create(buyer.getId(),address(false));
        var snapshot=transaction().execute(s->addresses.shippingSnapshot(buyer.getId(),a.id()));
        addresses.update(buyer.getId(),a.id(),new AddressDtos.Write("","Another","0901234567","New","","New","New",false));addresses.delete(buyer.getId(),a.id());
        assertEquals("Nguyễn An",snapshot.recipientName());assertEquals("12 Đường Số 1",snapshot.addressDetail());
        var b=addresses.create(buyer.getId(),address(false));failure(()->tx(()->addresses.shippingSnapshot(other.getId(),b.id())),"NOT_FOUND");
    }
    @Test void stockSellerOwnerAdminAndStaleWriteProtection() throws Exception {
        var p=product(5);
        mvc.perform(get("/api/seller/inventory").with(as(seller))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/seller/inventory").with(as(otherSeller))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(put("/api/seller/inventory/"+p.getId()).with(as(otherSeller)).with(csrf()).contentType("application/json").content("{\"stock\":0,\"expectedStock\":5}")).andExpect(status().isNotFound());
        mvc.perform(put("/api/seller/inventory/"+p.getId()).with(as(seller)).with(csrf()).contentType("application/json").content("{\"stock\":0,\"expectedStock\":5}")).andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(0));
        failure(()->inventory.adjust(seller.getId(),p.getId(),10,5),"STOCK_CHANGED");
        assertEquals(7,inventory.adjust(admin.getId(),p.getId(),7,0).stock());
    }
    @Test void stockAuthCsrfNegativeAndOverflowAreRejected() throws Exception {
        var p=product(1);String path="/api/seller/inventory/"+p.getId();
        mvc.perform(get("/api/seller/inventory")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/seller/inventory").with(as(buyer))).andExpect(status().isForbidden());
        mvc.perform(put(path).with(as(seller)).contentType("application/json").content("{\"stock\":0,\"expectedStock\":1}")).andExpect(status().isForbidden());
        for(String stock:List.of("-1","1000001","2147483648","1.5","null")) mvc.perform(put(path).with(as(seller)).with(csrf()).contentType("application/json").content("{\"stock\":"+stock+",\"expectedStock\":1}")).andExpect(status().isBadRequest());
        assertEquals(1,stock(p));
    }
    @RepeatedTest(5) void concurrentOrdersForOneBonsaiHaveOneWinner() throws Exception {
        var p=product(1);var a=order(p,1);var b=order(p,1);
        var outcomes=parallel(()->tx(()->inventory.debitConfirmedOrder(a.getId())),()->tx(()->inventory.debitConfirmedOrder(b.getId())));
        assertEquals(1,outcomes.stream().filter("OK"::equals).count());assertEquals(1,outcomes.stream().filter("OUT_OF_STOCK"::equals).count());assertEquals(0,stock(p));assertEquals(1,debits.count());
    }
    @Test void simultaneousOrderRetriesDebitOnce() throws Exception {
        var p=product(3);var o=order(p,2);
        assertEquals(List.of("OK","OK"),parallel(()->tx(()->inventory.debitConfirmedOrder(o.getId())),()->tx(()->inventory.debitConfirmedOrder(o.getId()))));assertEquals(1,stock(p));assertEquals(1,debits.count());
    }
    @Test void multiItemFailureRollsBackAllStockAndLedger() {
        var a=product(2);var b=product(0);var o=order(a,1);line(o,b,1);
        failure(()->tx(()->inventory.debitConfirmedOrder(o.getId())),"OUT_OF_STOCK");assertEquals(2,stock(a));assertEquals(0,stock(b));assertEquals(0,debits.count());
    }
    @Test void multiItemSuccessAndCancellationRestoreExactlyOnce() {
        var a=product(2);var b=product(3);var o=order(a,1);line(o,b,2);
        tx(()->inventory.debitConfirmedOrder(o.getId()));assertEquals(1,stock(a));assertEquals(1,stock(b));cancel(o);cancel(o);
        assertEquals(2,stock(a));assertEquals(3,stock(b));assertTrue(debits.findAll().stream().allMatch(InventoryDebit::isRestored));
    }
    @Test void concurrentRestorationIsIdempotent() throws Exception {
        var p=product(1);var o=order(p,1);tx(()->inventory.debitConfirmedOrder(o.getId()));
        // Lưu trạng thái hủy trước các lần thử trả tồn đồng thời, độc lập.
        o.setStatus(OrderStatus.CANCELLED);orders.save(o);
        parallel(()->tx(()->inventory.restoreCancelledOrder(o.getId())),()->tx(()->inventory.restoreCancelledOrder(o.getId())));assertEquals(1,stock(p));
    }
    @Test void callerRollbackRestoresStockAndDoesNotLeaveLedger() {
        var p=product(1);var o=order(p,1);
        assertThrows(IllegalStateException.class,()->tx(()->{inventory.debitConfirmedOrder(o.getId());throw new IllegalStateException("order failed");}));assertEquals(1,stock(p));assertEquals(0,debits.count());
    }
    @Test void changedRetryWrongStateAndUnpublishedProductsAreRejected() {
        var p=product(5);var o=order(p,1);tx(()->inventory.debitConfirmedOrder(o.getId()));line(o,p,1);
        failure(()->tx(()->inventory.debitConfirmedOrder(o.getId())),"INVENTORY_RETRY_CONFLICT");assertEquals(4,stock(p));
        failure(()->tx(()->inventory.restoreCancelledOrder(o.getId())),"ORDER_STATE_INVALID");
        var pending=order(p,1);pending.setStatus(OrderStatus.PENDING);orders.save(pending);failure(()->tx(()->inventory.debitConfirmedOrder(pending.getId())),"ORDER_STATE_INVALID");
        p.setStatus(ProductStatus.ARCHIVED);products.save(p);var hidden=order(p,1);failure(()->tx(()->inventory.debitConfirmedOrder(hidden.getId())),"PRODUCT_UNAVAILABLE");
    }
    @Test void internalOrderOperationsRequireCallerTransaction() {
        assertThrows(org.springframework.transaction.IllegalTransactionStateException.class,()->inventory.debitConfirmedOrder(1L));
        assertThrows(org.springframework.transaction.IllegalTransactionStateException.class,()->inventory.restoreCancelledOrder(1L));
        assertThrows(org.springframework.transaction.IllegalTransactionStateException.class,()->addresses.shippingSnapshot(buyer.getId(),1L));
    }
    @Test void oversizedAddressAndStockBodiesAreBounded() throws Exception {
        mvc.perform(post("/api/account/addresses").with(as(buyer)).with(csrf()).contentType("application/json").content("x".repeat(8193)))
            .andExpect(status().isPayloadTooLarge()).andExpect(jsonPath("$.code").value("REQUEST_TOO_LARGE"));
        var p=product(1);
        mvc.perform(put("/api/seller/inventory/"+p.getId()).with(as(seller)).with(csrf()).contentType("application/json").content("x".repeat(8193)))
            .andExpect(status().isPayloadTooLarge());assertEquals(1,stock(p));
    }
    @Test void simultaneousSellerAdjustmentsDoNotOverwriteEachOther() throws Exception {
        var p=product(1);
        var outcomes=parallel(()->inventory.adjust(seller.getId(),p.getId(),2,1),()->inventory.adjust(seller.getId(),p.getId(),3,1));
        assertEquals(1,outcomes.stream().filter("OK"::equals).count());assertEquals(1,outcomes.stream().filter("STOCK_CHANGED"::equals).count());assertTrue(stock(p)==2 || stock(p)==3);
    }
    @Test void inventoryValidationRejectsZeroTooLargeAndRestoredRetry() {
        var p=product(5);
        for(int qty:List.of(0,1000001)) {var o=order(p,qty);failure(()->tx(()->inventory.debitConfirmedOrder(o.getId())),"VALIDATION_FAILED");}
        var o=order(p,1);tx(()->inventory.debitConfirmedOrder(o.getId()));cancel(o);
        o.setStatus(OrderStatus.CONFIRMED);orders.save(o);failure(()->tx(()->inventory.debitConfirmedOrder(o.getId())),"INVENTORY_RETRY_CONFLICT");assertEquals(5,stock(p));
    }
    @Test void incompatibleCallerIsolationIsRejectedBeforeMutation() {
        var p=product(1);var o=order(p,1);var template=new TransactionTemplate(manager);
        template.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ);
        failure(()->template.executeWithoutResult(s->inventory.debitConfirmedOrder(o.getId())),"TRANSACTION_ISOLATION_REQUIRED");
        failure(()->template.executeWithoutResult(s->addresses.create(buyer.getId(),address(false))),"TRANSACTION_ISOLATION_REQUIRED");
        assertEquals(1,stock(p));assertEquals(0,debits.count());assertTrue(addresses.list(buyer.getId()).isEmpty());
    }
}
