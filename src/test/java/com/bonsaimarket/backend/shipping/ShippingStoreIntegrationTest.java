package com.bonsaimarket.backend.shipping;

import com.bonsaimarket.backend.auth.*;
import com.bonsaimarket.backend.category.*;
import com.bonsaimarket.backend.management.OrderManagementService;
import com.bonsaimarket.backend.marketplace.MarketplaceException;
import com.bonsaimarket.backend.order.*;
import com.bonsaimarket.backend.order.Order;
import com.bonsaimarket.backend.product.*;
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
import static com.bonsaimarket.backend.shipping.ShippingService.Scope.*;

/** H2/JPA/MockMvc and provider double only. No GHN sandbox, real payment or MySQL claims. */
@SpringBootTest(properties={"spring.profiles.active=shipping-map-test","spring.datasource.url=jdbc:h2:mem:shipping-map;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
    "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
    "spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
class ShippingStoreIntegrationTest {
    @Autowired MockMvc mvc;@Autowired JdbcTemplate db;@Autowired ObjectMapper json;
    @Autowired ShippingService shipping;@Autowired ShippingSettings settings;@Autowired StoreLocations locations;@Autowired OrderManagementService management;
    @Autowired UserRepository users;@Autowired StoreRepository stores;@Autowired OrderRepository orders;@Autowired OrderItemRepository items;
    @Autowired ProductRepository products;@Autowired CategoryRepository categories;
    @MockitoBean ShippingProvider provider;@MockitoBean ShippingPaymentBoundary payment;@MockitoBean AuthMailSender mail;
    User seller,admin,buyer,other;Store store;Product product;Order order;int shop;String code;
    @BeforeEach void setup(){
        seller=user(UserRole.SELLER);admin=user(UserRole.ADMIN);buyer=user(UserRole.CUSTOMER);other=user(UserRole.CUSTOMER);
        store=new Store();store.setOwner(seller);store.setName("Vườn thử nghiệm "+seller.getId());store.setDescription("<img src=x onerror=alert(1)>");store.setAddress("PRIVATE STORE REGISTRATION");store.setPhone("0900000000");store.setStatus(StoreStatus.ACTIVE);store=stores.save(store);
        db.update("insert into seller_applications(applicant_id,status,name,description,phone,address,reason,processed_by,submitted_at,processed_at,revision) values(?,'APPROVED','Test','Test','0900000000','PRIVATE APPLICATION','',?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0)",seller.getId(),admin.getId());
        var c=new Category();c.setName("Bonsai");c.setSlug(UUID.randomUUID().toString());c.setIsActive(true);c=categories.save(c);
        product=new Product();product.setName("Phụ kiện được chấp nhận để thử nghiệm");product.setSlug(UUID.randomUUID().toString());product.setStore(store);product.setCategory(c);product.setStatus(ProductStatus.ACTIVE);product.setStock(10);product.setPrice(new BigDecimal("100000"));product=products.save(product);
        order=new Order();order.setCode(UUID.randomUUID().toString());order.setStore(store);order.setBuyer(buyer);order.setStatus(OrderStatus.PENDING);order.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));order.setUpdatedAt(order.getCreatedAt());order.setSubtotal(new BigDecimal("100000"));order.setShippingFee(new BigDecimal("25000"));order.setTotal(new BigDecimal("125000"));order.setShippingMethod("STANDARD");
        order.setShippingAddressSnapshot(json.writeValueAsString(new Destination("Người nhận thử nghiệm","0901234567","Hồ Chí Minh","Quận 1","Phường Bến Nghé","PRIVATE BUYER DELIVERY",202,1444,"20308",false)));order=orders.save(order);
        var i=new OrderItem();i.setOrder(order);i.setProduct(product);i.setProductNameSnapshot(product.getName());i.setQuantity(1);i.setUnitPrice(product.getPrice());i.setLineTotal(product.getPrice());items.save(i);
        management.transition(seller.getId(),false,order.getId(),"CONFIRMED");order=orders.findById(order.getId()).orElseThrow();
        shop=store.getId().intValue()+1000;code="STAGE"+order.getId();settings.setEnabled(true);settings.setToken("test-only-token-never-real");settings.setWebhookSecret("test-only-shared-secret-32-characters");settings.getShopIds().put(store.getId(),shop);
        when(provider.pickup(shop)).thenReturn(new Pickup(shop,1452,"21007","Kho thử nghiệm","0900000000","PRIVATE PICKUP WAREHOUSE"));
        when(provider.validateAddress(eq(shop),any())).thenAnswer(a->a.getArgument(1));
        when(provider.quote(eq(shop),any(),any(),any(),anyInt())).thenReturn(List.of(new Rate(2,"Hàng nhẹ",new BigDecimal("25000"))));
        when(payment.authorize(any(),any())).thenThrow(new MarketplaceException(409,"PAYMENT_CONTRACT_REQUIRED","Payment contract missing"));
        when(provider.create(eq(shop),any())).thenAnswer(a->{Create r=a.getArgument(1);return new Remote(code,r.clientReference(),shop,null,null);});
        when(provider.find(eq(shop),any(),anyString())).thenAnswer(a->remote(a.getArgument(2),"ready_to_pick",Instant.now()));
    }
    User user(UserRole role){var u=new User();u.setName("Shipping QA");u.setEmail(UUID.randomUUID()+"@example.invalid");u.setPasswordHash("!");u.setIsActive(true);u.setRole(role);u.setEmailVerifiedAt(LocalDateTime.now(ZoneOffset.UTC));return users.save(u);}
    RequestPostProcessor as(User u){return authentication(UsernamePasswordAuthenticationToken.authenticated(AuthPrincipal.of(u),null,List.of(new SimpleGrantedAuthority("ROLE_"+u.getRole()))));}
    String path(String role,String suffix){return "/api/"+role+"/orders/"+order.getId()+"/shipping"+suffix;}
    void parcel(){shipping.saveParcel(seller.getId(),order.getId(),new ShippingService.ParcelWrite(800,25,20,15,true,0));}
    ShippingService.QuoteView quote(){parcel();return shipping.quotes(buyer.getId(),order.getId()).getFirst();}
    void selected(){var q=quote();shipping.select(buyer.getId(),order.getId(),q.id());}
    void openTestPaymentGate(){doReturn(new ShippingPaymentBoundary.Authorization(1,0)).when(payment).authorize(any(),any());}
    void created(){selected();openTestPaymentGate();shipping.create(seller.getId(),order.getId());}
    Remote remote(String reference,String state,Instant time){return new Remote(code,reference,shop,state,time);}
    String reference(){return db.queryForObject("select client_reference from shipments where order_id=?",String.class,order.getId());}
    String state(){return db.queryForObject("select state from shipments where order_id=?",String.class,order.getId());}
    long events(){return db.queryForObject("select count(*) from shipment_events e join shipments s on s.id=e.shipment_id where s.order_id=?",Long.class,order.getId());}
    void assertStock(int expected){assertEquals(expected,products.findById(product.getId()).orElseThrow().getStock());}
    void event(String status,Instant time){shipping.webhook(shop,code,reference(),"switch_status",status,time);}
    @Test void quoteRequiresMeasuredParcelAndDoesNotReturnZeroOnFailure(){
        assertEquals("MISSING_PARCEL",assertThrows(MarketplaceException.class,()->shipping.quotes(buyer.getId(),order.getId())).code);
        parcel();when(provider.quote(eq(shop),any(),any(),any(),anyInt())).thenThrow(new MarketplaceException(503,"CARRIER_UNAVAILABLE","Provider unavailable"));
        assertEquals(503,assertThrows(MarketplaceException.class,()->shipping.quotes(buyer.getId(),order.getId())).status);
        assertEquals(0,db.queryForObject("select count(*) from shipping_quotes where order_id=?",Integer.class,order.getId()));
    }
    @Test void validQuoteIsSnapshotCachedAndSelectionDoesNotRewriteOrder(){
        var q=quote();assertEquals(new BigDecimal("25000"),q.fee());assertEquals("STAGING",q.environment());
        assertEquals(q.id(),shipping.quotes(buyer.getId(),order.getId()).getFirst().id());verify(provider,times(1)).quote(eq(shop),any(),any(),any(),anyInt());
        shipping.select(buyer.getId(),order.getId(),q.id());assertNotNull(shipping.overview(buyer.getId(),order.getId(),BUYER).selection().selectedAt());
        assertEquals(new BigDecimal("125000.00"),orders.findById(order.getId()).orElseThrow().getTotal());verify(provider,never()).create(anyInt(),any());
    }
    @Test void expiredQuoteAndChangedParcelAreRejected(){
        var q=quote();db.update("update shipping_quotes set expires_at=? where id=?",LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1),q.id());
        assertEquals("QUOTE_EXPIRED",assertThrows(MarketplaceException.class,()->shipping.select(buyer.getId(),order.getId(),q.id())).code);
        var fresh=shipping.quotes(buyer.getId(),order.getId()).getFirst();shipping.saveParcel(seller.getId(),order.getId(),new ShippingService.ParcelWrite(900,25,20,15,true,1));
        assertEquals("QUOTE_CHANGED",assertThrows(MarketplaceException.class,()->shipping.select(buyer.getId(),order.getId(),fresh.id())).code);
    }
    @Test void invalidAddressUnsupportedRouteAndGoodsPolicyRemainErrors(){
        parcel();when(provider.validateAddress(eq(shop),any())).thenThrow(new MarketplaceException(422,"ADDRESS_INVALID","Not mapped"));assertEquals(422,assertThrows(MarketplaceException.class,()->shipping.quotes(buyer.getId(),order.getId())).status);
        doAnswer(a->a.getArgument(1)).when(provider).validateAddress(eq(shop),any());when(provider.quote(eq(shop),any(),any(),any(),anyInt())).thenReturn(List.of());assertEquals("UNSUPPORTED_ROUTE",assertThrows(MarketplaceException.class,()->shipping.quotes(buyer.getId(),order.getId())).code);
        shipping.saveParcel(seller.getId(),order.getId(),new ShippingService.ParcelWrite(800,25,20,15,false,1));assertEquals("GOODS_ACCEPTANCE_REQUIRED",assertThrows(MarketplaceException.class,()->shipping.quotes(buyer.getId(),order.getId())).code);
    }
    @Test void addressesAndCarrierConfigurationCannotBeForgedViaBody() throws Exception {
        mvc.perform(put(path("seller","/parcel")).with(as(seller)).with(csrf()).contentType("application/json").content("{\"weightGrams\":0,\"lengthCm\":10,\"widthCm\":10,\"heightCm\":10,\"shopId\":999,\"paid\":true}"))
            .andExpect(status().isBadRequest());verifyNoInteractions(provider);
        settings.getShopIds().remove(store.getId());parcel();assertEquals("CARRIER_SHOP_REQUIRED",assertThrows(MarketplaceException.class,()->shipping.quotes(buyer.getId(),order.getId())).code);
    }
    @Test void buyerSellerAdminScopeAndCsrfAreEnforced() throws Exception {
        mvc.perform(get(path("account","")).with(as(other))).andExpect(status().isNotFound());
        mvc.perform(get(path("seller","")).with(as(user(UserRole.SELLER)))).andExpect(status().isNotFound());
        mvc.perform(get(path("account","")).with(as(buyer))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(get(path("admin","")).with(as(admin))).andExpect(status().isOk());
        mvc.perform(post(path("seller","/shipments")).with(as(buyer)).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post(path("seller","/shipments")).with(as(seller))).andExpect(status().isForbidden());
        mvc.perform(post(path("account","/quotes")).with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(post(path("account","/quotes")).with(as(buyer))).andExpect(status().isForbidden());verifyNoInteractions(provider);
    }
    @Test void authoritativePaymentBoundaryIsClosedForConfirmedAndLegacyStates(){
        selected();assertEquals("PAYMENT_CONTRACT_REQUIRED",assertThrows(MarketplaceException.class,()->shipping.create(seller.getId(),order.getId())).code);
        assertFalse(shipping.overview(seller.getId(),order.getId(),SELLER).canCreate());verify(provider,never()).create(anyInt(),any());
        assertEquals(0,db.queryForObject("select count(*) from shipments where order_id=?",Integer.class,order.getId()));
        assertEquals("PAYMENT_CONTRACT_REQUIRED",assertThrows(MarketplaceException.class,()->new ShippingPaymentBoundary().authorize(order,new ShippingQuote())).code);
    }
    @Test void pendingOrderAndMismatchWithConfirmedTotalCannotShip(){
        selected();openTestPaymentGate();db.update("update orders set status='PENDING' where id=?",order.getId());assertEquals("ORDER_NOT_READY",assertThrows(MarketplaceException.class,()->shipping.create(seller.getId(),order.getId())).code);
        db.update("update orders set status='CONFIRMED' where id=?",order.getId());db.update("update shipping_quotes set fee=26000 where order_id=?",order.getId());assertEquals("ORDER_TOTAL_MISMATCH",assertThrows(MarketplaceException.class,()->shipping.create(seller.getId(),order.getId())).code);verify(provider,never()).create(anyInt(),any());
    }
    @Test void creationPersistsCodeAndDuplicateRequestNeverCreatesAgain(){
        created();shipping.create(seller.getId(),order.getId());verify(provider,times(1)).create(eq(shop),any());assertEquals("WAITING_PICKUP",state());assertStock(9);
        assertEquals(code,shipping.overview(buyer.getId(),order.getId(),BUYER).shipment().trackingCode());
    }
    @Test void lostCreateResponseReconcilesByClientReferenceWithoutRepeatingCreate(){
        selected();openTestPaymentGate();when(provider.create(eq(shop),any())).thenThrow(new MarketplaceException(503,"CARRIER_UNAVAILABLE","Timeout after acceptance"));
        assertThrows(MarketplaceException.class,()->shipping.create(seller.getId(),order.getId()));assertEquals("UNKNOWN",state());
        shipping.create(seller.getId(),order.getId());assertEquals("WAITING_PICKUP",state());assertEquals(code,shipping.overview(buyer.getId(),order.getId(),BUYER).shipment().trackingCode());verify(provider,times(1)).create(eq(shop),any());verify(provider).find(eq(shop),isNull(),eq(reference()));
    }
    @Test void malformedCreateResponseAndPickupChangeStayUnknown(){
        selected();openTestPaymentGate();when(provider.create(eq(shop),any())).thenReturn(new Remote("FORGED", "WRONG",shop+1,null,null));assertThrows(MarketplaceException.class,()->shipping.create(seller.getId(),order.getId()));assertEquals("UNKNOWN",state());assertNull(shipping.overview(buyer.getId(),order.getId(),BUYER).shipment().trackingCode());
    }
    @Test void pickupChangeAfterQuoteCannotCreateFromAnotherWarehouse(){
        selected();openTestPaymentGate();when(provider.pickup(shop)).thenReturn(new Pickup(shop,1452,"21007","New warehouse","0900000000","Different pickup"));
        assertEquals("PICKUP_CHANGED",assertThrows(MarketplaceException.class,()->shipping.create(seller.getId(),order.getId())).code);
        assertEquals("UNKNOWN",state());verify(provider,never()).create(anyInt(),any());assertStock(9);
    }
    @Test void validParcelCannotOverrideShopTrackingOrPaymentAndEmailRevocationHidesLocation() throws Exception {
        mvc.perform(put(path("seller","/parcel")).with(as(seller)).with(csrf()).contentType("application/json")
            .content("{\"weightGrams\":800,\"lengthCm\":25,\"widthCm\":20,\"heightCm\":15,\"carrierAcceptedGoods\":true,\"revision\":0,\"shopId\":999999,\"trackingCode\":\"FORGED\",\"paid\":true}"))
            .andExpect(status().isOk());
        var q=shipping.quotes(buyer.getId(),order.getId()).getFirst();shipping.select(buyer.getId(),order.getId(),q.id());
        assertEquals("PAYMENT_CONTRACT_REQUIRED",assertThrows(MarketplaceException.class,()->shipping.create(seller.getId(),order.getId())).code);
        verify(provider).quote(eq(shop),any(),any(),any(),anyInt());verify(provider,never()).create(anyInt(),any());
        locations.save(seller.getId(),location(true,true,0));db.update("update users set email_verified_at=null where id=?",seller.getId());
        assertTrue(locations.discover(null,null,null,store.getId(),0).content().isEmpty());
    }
    @Test void concurrentCreateAndCancellationShareOrderLockWithoutLongCarrierTransaction() throws Exception {
        selected();openTestPaymentGate();var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var pool=Executors.newSingleThreadExecutor();
        when(provider.create(eq(shop),any())).thenAnswer(a->{entered.countDown();assertTrue(release.await(5,TimeUnit.SECONDS));Create r=a.getArgument(1);return new Remote(code,r.clientReference(),shop,null,null);});
        try{var first=pool.submit(()->shipping.create(seller.getId(),order.getId()));assertTrue(entered.await(5,TimeUnit.SECONDS));
            assertEquals(ShipmentState.CREATING,shipping.create(seller.getId(),order.getId()).shipment().state());
            assertEquals("SHIPMENT_REQUIRES_RECONCILIATION",assertThrows(MarketplaceException.class,()->management.cancelByBuyer(buyer.getId(),order.getId())).code);assertStock(9);
            release.countDown();first.get(8,TimeUnit.SECONDS);verify(provider,times(1)).create(eq(shop),any());
        }finally{release.countDown();pool.shutdownNow();}
    }
    @Test void webhookDeduplicatesRetainsLateFactsAndDoesNotInventOrderProgress(){
        created();Instant t=Instant.now().plusSeconds(20);event("delivered",t);long count=events();event("delivered",t);assertEquals(count,events());
        event("transporting",t.minusSeconds(10));assertEquals("DELIVERED",state());assertEquals(count+1,events());
        assertEquals(OrderStatus.CONFIRMED,orders.findById(order.getId()).orElseThrow().getStatus());assertStock(9);
    }
    @Test void invalidCarrierStatusFutureTimestampAndForeignReferenceAreRejected(){
        created();assertThrows(MarketplaceException.class,()->event("fake_status",Instant.now()));assertThrows(MarketplaceException.class,()->event("delivered",Instant.now().plusSeconds(1000)));
        assertThrows(MarketplaceException.class,()->shipping.webhook(shop+1,code,reference(),"switch_status","picked",Instant.now()));assertThrows(MarketplaceException.class,()->shipping.webhook(shop,"FORGED",reference(),"switch_status","picked",Instant.now()));assertEquals("WAITING_PICKUP",state());
    }
    @Test void forgedWebhookBrowserOriginAndMalformedPayloadCannotWrite() throws Exception {
        created();String body=json.writeValueAsString(Map.of("ShopID",shop,"OrderCode",code,"ClientOrderCode",reference(),"Type","switch_status","Status","delivered","Time",Instant.now().toString()));long before=events();
        mvc.perform(post("/api/shipping/webhooks/ghn").contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/shipping/webhooks/ghn").header("X-GHN-Webhook-Secret",settings.getWebhookSecret()).header("Origin","http://browser.invalid").contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/shipping/webhooks/ghn").header("X-GHN-Webhook-Secret",settings.getWebhookSecret()).contentType("application/json").content("{bad" )).andExpect(status().isBadRequest());assertEquals(before,events());
        mvc.perform(post("/api/shipping/webhooks/ghn").header("X-GHN-Webhook-Secret",settings.getWebhookSecret()).contentType("application/json").content(body)).andExpect(status().isNoContent());assertEquals(before+1,events());
    }
    @Test void unknownShipmentAndOversizedWebhookAreRejected() throws Exception {
        shipping.webhook(shop,"UNKNOWN","UNKNOWN","update_cod","delivered",Instant.now());
        assertThrows(MarketplaceException.class,()->shipping.webhook(shop,"UNKNOWN","UNKNOWN","switch_status","delivered",Instant.now()));
        mvc.perform(post("/api/shipping/webhooks/ghn").header("X-GHN-Webhook-Secret",settings.getWebhookSecret()).contentType("application/json").content(" ".repeat(33000))).andExpect(status().isPayloadTooLarge());
    }
    @Test void trackingProjectionDoesNotLeakPickupDeliveryOrProviderSecrets() throws Exception {
        created();var result=mvc.perform(get(path("account","")).with(as(buyer))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        for(String privateValue:List.of("PRIVATE PICKUP","PRIVATE BUYER","0901234567",settings.getToken(),settings.getWebhookSecret()))assertFalse(result.contains(privateValue));
        mvc.perform(get("/api/stores/locations?storeId="+store.getId())).andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
    }
    @Test void pollingIsBoundedAndOutageDoesNotBecomeEmptySuccess(){
        created();assertEquals(429,assertThrows(MarketplaceException.class,()->shipping.synchronize(seller.getId(),order.getId(),SELLER)).status);
        db.update("update shipments set sync_after=null where order_id=?",order.getId());when(provider.find(eq(shop),any(),anyString())).thenThrow(new MarketplaceException(503,"CARRIER_UNAVAILABLE","Outage"));assertEquals(503,assertThrows(MarketplaceException.class,()->shipping.synchronize(seller.getId(),order.getId(),SELLER)).status);assertEquals("WAITING_PICKUP",state());assertNotNull(shipping.overview(buyer.getId(),order.getId(),BUYER).shipment().lastSyncAt());
    }
    @Test void cancelledCarrierBeforePickupAllowsExistingInventoryRestoreAtMostOnce(){
        created();when(provider.cancel(shop,code)).thenReturn(true);when(provider.find(eq(shop),any(),anyString())).thenAnswer(a->remote(a.getArgument(2),"cancel",Instant.now().plusSeconds(20)));
        shipping.cancel(seller.getId(),order.getId());assertEquals("CANCELLED",state());assertStock(9);
        management.cancelByBuyer(buyer.getId(),order.getId());assertStock(10);assertThrows(MarketplaceException.class,()->management.cancelByBuyer(buyer.getId(),order.getId()));assertStock(10);verifyNoInteractions(mail);
    }
    @Test void carrierCancelFailureBlocksStockRestoreAndPostPickupCancelIsRejected(){
        created();event("picked",Instant.now().plusSeconds(10));assertThrows(MarketplaceException.class,()->shipping.cancel(seller.getId(),order.getId()));verify(provider,never()).cancel(anyInt(),anyString());assertStock(9);
        assertThrows(MarketplaceException.class,()->management.cancelByBuyer(buyer.getId(),order.getId()));
    }
    @Test void cancelRejectedByCarrierRemainsUnknownAndReturnNeverRestoresStock(){
        created();when(provider.cancel(shop,code)).thenReturn(false);assertThrows(MarketplaceException.class,()->shipping.cancel(seller.getId(),order.getId()));assertEquals("UNKNOWN",state());
        event("returning",Instant.now().plusSeconds(15));event("returned",Instant.now().plusSeconds(20));assertStock(9);assertThrows(MarketplaceException.class,()->management.cancelByBuyer(buyer.getId(),order.getId()));verifyNoInteractions(mail);
    }
    StoreLocations.Write location(boolean published,boolean consent,long revision){return new StoreLocations.Write("EXACT",published,consent,"Tỉnh thử nghiệm "+store.getId(),"Phường thử nghiệm","SHOWROOM PUBLIC",new BigDecimal("10.7750000"),new BigDecimal("106.7000000"),revision);}
    @Test void verifiedSellerLocationRequiresSeparateConsentAndIsReadBack(){
        assertThrows(MarketplaceException.class,()->locations.save(seller.getId(),location(true,false,0)));
        var saved=locations.save(seller.getId(),location(true,true,0));assertNotNull(saved.consentAt());assertEquals(saved,locations.settings(seller.getId()).location());
        var publicView=locations.discover(null,null,null,store.getId(),0).content().getFirst();assertEquals("SHOWROOM PUBLIC",publicView.publicAddress());
        String data=json.writeValueAsString(publicView);assertFalse(data.contains("PRIVATE"));assertFalse(data.contains("0900000000"));assertFalse(data.contains("owner"));
    }
    @Test void sellerRoleAloneCannotBypassApprovalOrActiveAccount(){
        db.update("update seller_applications set status='REJECTED' where applicant_id=?",seller.getId());assertEquals(403,assertThrows(MarketplaceException.class,()->locations.save(seller.getId(),location(true,true,0))).status);
        assertThrows(MarketplaceException.class,()->locations.save(seller.getId(),location(false,false,0)));
    }
    @Test void revokedVerificationHiddenStoreAndDisabledVisibilityAreExcluded(){
        locations.save(seller.getId(),location(true,true,0));assertEquals(1,locations.discover(null,null,null,store.getId(),0).content().size());
        db.update("update users set role='CUSTOMER' where id=?",seller.getId());assertTrue(locations.discover(null,null,null,store.getId(),0).content().isEmpty());
        db.update("update users set role='SELLER' where id=?",seller.getId());db.update("update stores set status='INACTIVE' where id=?",store.getId());assertTrue(locations.discover(null,null,null,store.getId(),0).content().isEmpty());
        db.update("update stores set status='ACTIVE' where id=?",store.getId());locations.save(seller.getId(),location(false,false,1));assertTrue(locations.discover(null,null,null,store.getId(),0).content().isEmpty());
    }
    @Test void invalidCoordinatesAndFalseExactPinForAreaAreRejected(){
        assertThrows(MarketplaceException.class,()->locations.save(seller.getId(),new StoreLocations.Write("EXACT",true,true,"Tỉnh","Phường","Public",new BigDecimal("91"),BigDecimal.TEN,0)));
        assertThrows(MarketplaceException.class,()->locations.save(seller.getId(),new StoreLocations.Write("EXACT",true,true,"Tỉnh","Phường","Public",BigDecimal.ZERO,BigDecimal.ZERO,0)));
        assertThrows(MarketplaceException.class,()->locations.save(seller.getId(),new StoreLocations.Write("AREA",true,true,"Tỉnh","Phường","Public",BigDecimal.TEN,BigDecimal.TEN,0)));
        locations.save(seller.getId(),new StoreLocations.Write("AREA",true,true,"Tỉnh","Phường","",null,null,0));assertNull(locations.discover(null,null,null,store.getId(),0).content().getFirst().latitude());
    }
    @Test void locationUpdatesUseRevisionAndPublicFiltersUseRealCategory(){
        locations.save(seller.getId(),location(true,true,0));locations.save(seller.getId(),location(true,true,1));assertThrows(MarketplaceException.class,()->locations.save(seller.getId(),location(true,true,1)));
        assertEquals(1,locations.discover("Vườn thử nghiệm "+seller.getId(),"Tỉnh thử nghiệm "+store.getId(),product.getCategory().getId(),store.getId(),0).content().size());
        assertTrue(locations.discover(null,"Wrong region",null,store.getId(),0).content().isEmpty());assertTrue(locations.discover(null,null,999999L,store.getId(),0).content().isEmpty());
    }
    @Test void locationPermissionAndCsrfCannotBeBypassedViaOwnerId() throws Exception {
        var body=json.writeValueAsString(location(true,true,0));mvc.perform(put("/api/seller/store/location").with(as(buyer)).with(csrf()).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(put("/api/seller/store/location").with(as(seller)).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(put("/api/seller/store/location").with(as(user(UserRole.SELLER))).with(csrf()).contentType("application/json").content(body)).andExpect(status().isNotFound());
        mvc.perform(put("/api/seller/store/location").with(as(seller)).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk());
    }
}
