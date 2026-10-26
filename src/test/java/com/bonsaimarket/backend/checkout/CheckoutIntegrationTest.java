package com.bonsaimarket.backend.checkout;

import com.bonsaimarket.backend.address.*;
import com.bonsaimarket.backend.auth.*;
import com.bonsaimarket.backend.cart.*;
import com.bonsaimarket.backend.category.*;
import com.bonsaimarket.backend.management.OrderManagementService;
import com.bonsaimarket.backend.marketplace.MarketplaceException;
import com.bonsaimarket.backend.order.*;
import com.bonsaimarket.backend.product.*;
import com.bonsaimarket.backend.shipping.*;
import com.bonsaimarket.backend.store.*;
import com.bonsaimarket.backend.user.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.bonsaimarket.backend.shipping.ShippingProvider.*;

/** H2/MockMvc, real transaction/payment/inventory services; only the carrier is doubled. */
@SpringBootTest(properties={"spring.profiles.active=checkout-test","spring.datasource.url=jdbc:h2:mem:checkout;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
    "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
    "spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
class CheckoutIntegrationTest extends CheckoutTestFixture {
    @Test void receiptRecoveryIsBuyerScopedAndSurvivesExpiredQuoteAndCancellation() throws Exception {
        var body=body(quote(buyer));var placed=checkout.place(buyer.getId(),body);
        db.update("update checkout_quotes set expires_at=? where id=?",LocalDateTime.now(ZoneOffset.UTC).minusDays(1),body.quoteId());
        management.cancelByBuyer(buyer.getId(),placed.orderId());
        mvc.perform(get("/api/account/checkout/requests/"+body.idempotencyKey()).with(as(buyer)))
            .andExpect(status().isOk()).andExpect(jsonPath("orderId").value(placed.orderId())).andExpect(jsonPath("status").value("CANCELLED"));
        mvc.perform(get("/api/account/checkout/requests/"+body.idempotencyKey()).with(as(other))).andExpect(status().isNotFound());
        mvc.perform(get("/api/account/checkout/requests/"+body.idempotencyKey())).andExpect(status().isUnauthorized());
        assertEquals(placed.orderId(),checkout.place(buyer.getId(),body).orderId());assertEquals(10,stock());
        assertEquals(1,db.queryForObject("select count(*) from orders where buyer_user_id=?",Integer.class,buyer.getId()));
    }
    @Test void cancellationDuringCarrierPreflightPreventsWaybillAndRestoresExactlyOnce() throws Exception {
        var placed=checkout.place(buyer.getId(),body(quote(buyer)));management.transition(seller.getId(),false,placed.orderId(),"CONFIRMED");
        var preflight=new CountDownLatch(1);var resume=new CountDownLatch(1);
        when(provider.quoteCod(eq(shop),any(),any(),any(),anyInt(),anyInt())).thenAnswer(a->{preflight.countDown();assertTrue(resume.await(15,TimeUnit.SECONDS));return List.of(new Rate(2,"Dịch vụ thử nghiệm",new BigDecimal("24000")));});
        var pool=Executors.newSingleThreadExecutor();
        try{var creation=pool.submit(()->{code("ORDER_NOT_READY",()->shipping.create(seller.getId(),placed.orderId()));});
            assertTrue(preflight.await(15,TimeUnit.SECONDS));management.cancelByBuyer(buyer.getId(),placed.orderId());resume.countDown();creation.get(20,TimeUnit.SECONDS);
            assertEquals(10,stock());assertEquals("CANCELLED",management.buyerDetail(buyer.getId(),placed.orderId()).payment().getStatus());
            assertEquals(0,db.queryForObject("select count(*) from shipments where order_id=?",Integer.class,placed.orderId()));verify(provider,never()).create(anyInt(),any());
        }finally{resume.countDown();pool.shutdownNow();}
    }
    @Test void shipmentReservationPreventsConcurrentCancellationAndStockRestore() throws Exception {
        var placed=checkout.place(buyer.getId(),body(quote(buyer)));management.transition(seller.getId(),false,placed.orderId(),"CONFIRMED");
        var reserved=new CountDownLatch(1);var resume=new CountDownLatch(1);
        when(provider.create(eq(shop),any())).thenAnswer(a->{reserved.countDown();assertTrue(resume.await(15,TimeUnit.SECONDS));Create request=a.getArgument(1);return new Remote("STAGE"+store.getId(),request.clientReference(),shop,null,null);});
        var pool=Executors.newSingleThreadExecutor();
        try{var creation=pool.submit(()->shipping.create(seller.getId(),placed.orderId()));assertTrue(reserved.await(15,TimeUnit.SECONDS));
            code("SHIPMENT_REQUIRES_RECONCILIATION",()->management.cancelByBuyer(buyer.getId(),placed.orderId()));assertEquals(9,stock());
            resume.countDown();creation.get(20,TimeUnit.SECONDS);shipping.create(seller.getId(),placed.orderId());verify(provider,times(1)).create(eq(shop),any());
            assertEquals("CONFIRMED",management.buyerDetail(buyer.getId(),placed.orderId()).status());assertEquals(9,stock());
        }finally{resume.countDown();pool.shutdownNow();}
    }
    @Test void placementUsesSnapshotsAndExistingLedgerAndCommitClearsOnlyOrderedLines(){
        var q=quote(buyer);var r=checkout.place(buyer.getId(),body(q));assertEquals("PENDING",r.status());assertEquals("AWAITING_COLLECTION",r.paymentStatus());assertEquals(0,new BigDecimal("124000").compareTo(r.total()));assertEquals(9,stock());assertTrue(cart.view(buyer.getId()).items().isEmpty());
        assertEquals("REQUESTED",db.queryForObject("select collection_status from order_payments where order_id=?",String.class,r.orderId()));assertEquals("UNKNOWN",db.queryForObject("select settlement_status from order_payments where order_id=?",String.class,r.orderId()));assertNull(db.queryForObject("select settled_amount from order_payments where order_id=?",BigDecimal.class,r.orderId()));
        assertTrue(management.buyerDetail(buyer.getId(),r.orderId()).shippingAddress().contains("PRIVATE DELIVERY"));
        management.transition(seller.getId(),false,r.orderId(),"CONFIRMED");assertEquals(9,stock());assertTrue(shipping.overview(seller.getId(),r.orderId(),ShippingService.Scope.SELLER).canCreate());
        // Accepted Checkout quotes remain fulfillable later; no repricing of an already committed contract.
        db.update("update shipping_quotes set expires_at=? where order_id=?",LocalDateTime.now(ZoneOffset.UTC).minusDays(1),r.orderId());
        shipping.create(seller.getId(),r.orderId());shipping.create(seller.getId(),r.orderId());verify(provider,times(1)).create(eq(shop),argThat(c->c.feePayer()==2&&c.codAmount()==100000));
        var overview=shipping.overview(buyer.getId(),r.orderId(),ShippingService.Scope.BUYER);assertEquals("WAITING_PICKUP",overview.shipment().state().name());assertFalse(json.writeValueAsString(overview).contains("PRIVATE PICKUP"));
        db.update("update products set name='Changed',price=200000 where id=?",product.getId());assertEquals("Phụ kiện kiểm thử",management.buyerDetail(buyer.getId(),r.orderId()).items().getFirst().name());
    }
    @Test void retryAfterLostResponseReturnsSameOrderAndDifferentPayloadConflicts(){var b=body(quote(buyer));var a=checkout.place(buyer.getId(),b);var repeated=checkout.place(buyer.getId(),b);assertEquals(a.orderId(),repeated.orderId());assertTrue(repeated.replayed());assertEquals(a.orderId(),checkout.recover(buyer.getId(),b.idempotencyKey()).orderId());code("IDEMPOTENCY_CONFLICT",()->checkout.place(buyer.getId(),new CheckoutService.Place(b.quoteId(),b.idempotencyKey(),"COD","different")));assertEquals(9,stock());}
    @Test void differentKeysCannotConsumeSameQuote(){var q=quote(buyer);checkout.place(buyer.getId(),body(q));code("QUOTE_USED",()->checkout.place(buyer.getId(),body(q)));}
    @Test void stalePriceRequiresReviewAndPreservesCart(){var b=body(quote(buyer));db.update("update products set price=200000 where id=?",product.getId());code("QUOTE_CHANGED",()->checkout.place(buyer.getId(),b));assertEquals(1,cart.view(buyer.getId()).quantity());assertEquals(10,stock());}
    @Test void changedQuantityInvalidatesQuote(){var b=body(quote(buyer));cart.update(buyer.getId(),cart.view(buyer.getId()).items().getFirst().id(),2);code("QUOTE_CHANGED",()->checkout.place(buyer.getId(),b));assertEquals(2,cart.view(buyer.getId()).quantity());}
    @Test void expiredQuotePreservesCart(){var b=body(quote(buyer));db.update("update checkout_quotes set expires_at=? where id=?",LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1),b.quoteId());code("QUOTE_EXPIRED",()->checkout.place(buyer.getId(),b));assertEquals(1,cart.view(buyer.getId()).quantity());}
    @Test void unavailableProductStoreAndStockAreRejected(){var b=body(quote(buyer));db.update("update products set status='DRAFT' where id=?",product.getId());code("PRODUCT_UNAVAILABLE",()->checkout.place(buyer.getId(),b));db.update("update products set status='ACTIVE',stock=0 where id=?",product.getId());code("OUT_OF_STOCK",()->checkout.place(buyer.getId(),b));db.update("update products set stock=10 where id=?",product.getId());db.update("update stores set status='INACTIVE' where id=?",store.getId());code("SELLER_UNAVAILABLE",()->checkout.place(buyer.getId(),b));}
    @Test void emptyCartUnsupportedPaymentAndCrossBuyerQuoteFailClosed(){code("EMPTY_CART",()->quote(other));var q=quote(buyer);code("PAYMENT_METHOD_UNSUPPORTED",()->checkout.place(buyer.getId(),new CheckoutService.Place(q.id(),UUID.randomUUID().toString(),"PREPAID","")));assertEquals(404,assertThrows(MarketplaceException.class,()->checkout.place(other.getId(),body(q))).status);assertEquals(1,cart.view(buyer.getId()).quantity());}
    @Test void savedAddressChangesInvalidateQuoteAndTemporaryAddressDoesNotEditAccount(){var saved=addresses.create(buyer.getId(),delivery());var q=checkout.quote(buyer.getId(),new CheckoutService.QuoteInput(store.getId(),checkout.preview(buyer.getId()).fingerprint(),saved.id(),null)).getFirst();var d=delivery();addresses.update(buyer.getId(),saved.id(),new AddressDtos.Write("",d.recipientName(),d.phone(),d.provinceName(),d.districtName(),d.wardName(),"NEW ADDRESS",false));code("ADDRESS_CHANGED",()->checkout.place(buyer.getId(),body(q)));checkout.place(buyer.getId(),body(quote(buyer)));assertEquals("NEW ADDRESS",addresses.list(buyer.getId()).getFirst().addressDetail());}
    @Test void packageAndCodCapabilityCannotBeInvented(){settings.getCodEnabledStoreIds().remove(store.getId());code("COD_ACCOUNT_REQUIRED",()->quote(buyer));settings.getCodEnabledStoreIds().add(store.getId());cart.update(buyer.getId(),cart.view(buyer.getId()).items().getFirst().id(),2);code("CHECKOUT_PACKAGE_REQUIRED",()->quote(buyer));}
    @Test void lastItemRaceAllowsExactlyOneAndLosingCartSurvives() throws Exception {
        db.update("update products set stock=1 where id=?",product.getId());cart.add(other.getId(),product.getId(),1);var first=body(quote(buyer));var second=body(quote(other));var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try{var futures=List.of(pool.submit(()->race(start,buyer,first)),pool.submit(()->race(start,other,second)));start.countDown();assertEquals(1,(futures.get(0).get(20,TimeUnit.SECONDS)?1:0)+(futures.get(1).get(20,TimeUnit.SECONDS)?1:0));assertEquals(0,stock());assertEquals(1,cart.view(buyer.getId()).quantity()+cart.view(other.getId()).quantity());}finally{pool.shutdownNow();}
    }
    boolean race(CountDownLatch start,User user,CheckoutService.Place b) throws Exception {start.await();try{checkout.place(user.getId(),b);return true;}catch(MarketplaceException e){assertEquals("OUT_OF_STOCK",e.code);return false;}}
    @Test void simultaneousSameKeyCreatesOnce() throws Exception {var b=body(quote(buyer));var pool=Executors.newFixedThreadPool(2);try{var a=pool.submit(()->checkout.place(buyer.getId(),b));var c=pool.submit(()->checkout.place(buyer.getId(),b));assertEquals(a.get(20,TimeUnit.SECONDS).orderId(),c.get(20,TimeUnit.SECONDS).orderId());assertEquals(9,stock());}finally{pool.shutdownNow();}}
    @Test void cancellationRestoresPlacementDebitOnceAndPaymentNeverSettles(){var r=checkout.place(buyer.getId(),body(quote(buyer)));management.cancelByBuyer(buyer.getId(),r.orderId());assertEquals(10,stock());code("ORDER_STATE_INVALID",()->management.cancelByBuyer(buyer.getId(),r.orderId()));assertEquals(10,stock());assertEquals("UNKNOWN",db.queryForObject("select settlement_status from order_payments where order_id=?",String.class,r.orderId()));}
    @Test void sessionCsrfOwnershipAndForgedMoney() throws Exception {
        mvc.perform(post("/api/account/checkout/orders").with(csrf()).contentType("application/json").content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/account/checkout/orders").with(as(buyer)).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        var b=body(quote(buyer));String payload=json.writeValueAsString(b);payload=payload.substring(0,payload.length()-1)+",\"buyerId\":"+other.getId()+",\"total\":1,\"shippingFee\":0,\"paid\":true}";
        mvc.perform(post("/api/account/checkout/orders").with(as(buyer)).with(csrf()).contentType("application/json").content(payload)).andExpect(status().isOk()).andExpect(jsonPath("total").value(124000)).andExpect(jsonPath("paymentStatus").value("AWAITING_COLLECTION"));
        mvc.perform(get("/api/account/cart").with(as(buyer))).andExpect(header().string("Cache-Control","no-store"));
    }
    @Test void multipleStoresAreExplicitlySelectedAndOtherLinesSurvive(){
        var otherSeller=user(UserRole.SELLER);var s=new Store();s.setOwner(otherSeller);s.setName("Gian khác");s.setStatus(StoreStatus.ACTIVE);s.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));s.setUpdatedAt(s.getCreatedAt());s=stores.save(s);
        var p=new Product();p.setStore(s);p.setCategory(product.getCategory());p.setName("Hàng gian khác");p.setSlug(UUID.randomUUID().toString());p.setStatus(ProductStatus.ACTIVE);p.setStock(5);p.setPrice(new BigDecimal("50000"));p.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));p.setUpdatedAt(p.getCreatedAt());p=products.save(p);cart.add(buyer.getId(),p.getId(),2);
        var r=checkout.place(buyer.getId(),body(quote(buyer)));assertEquals(store.getId(),management.buyerDetail(buyer.getId(),r.orderId()).storeId());assertEquals(1,management.buyerDetail(buyer.getId(),r.orderId()).items().size());assertEquals(2,cart.view(buyer.getId()).quantity());assertEquals(p.getId(),cart.view(buyer.getId()).items().getFirst().productId());
        assertEquals(404,assertThrows(MarketplaceException.class,()->management.detail(otherSeller.getId(),false,r.orderId())).status);
    }
    @Test void currentCarrierFeeChangeBlocksBeforeWaybillReservation(){var r=checkout.place(buyer.getId(),body(quote(buyer)));management.transition(seller.getId(),false,r.orderId(),"CONFIRMED");when(provider.quoteCod(eq(shop),any(),any(),any(),anyInt(),anyInt())).thenReturn(List.of(new Rate(2,"Changed",new BigDecimal("28000"))));code("SHIPPING_PRICE_CHANGED",()->shipping.create(seller.getId(),r.orderId()));verify(provider,never()).create(anyInt(),any());assertEquals(0,db.queryForObject("select count(*) from shipments where order_id=?",Integer.class,r.orderId()));management.cancelByBuyer(buyer.getId(),r.orderId());assertEquals(10,stock());}
    @Test void acceptedShippingAndPaymentCannotBeRewritten(){var r=checkout.place(buyer.getId(),body(quote(buyer)));code("CHECKOUT_SELECTION_LOCKED",()->shipping.saveParcel(seller.getId(),r.orderId(),new ShippingService.ParcelWrite(900,20,20,20,true,1)));code("CHECKOUT_SELECTION_LOCKED",()->shipping.quotes(buyer.getId(),r.orderId()));management.transition(seller.getId(),false,r.orderId(),"CONFIRMED");db.update("update order_payments set cod_amount=1,amount_due=24001 where order_id=?",r.orderId());code("PAYMENT_AMOUNT_MISMATCH",()->shipping.create(seller.getId(),r.orderId()));verify(provider,never()).create(anyInt(),any());}
}
