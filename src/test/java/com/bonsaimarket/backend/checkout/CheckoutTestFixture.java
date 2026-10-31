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

/** Dữ liệu thương mại cô lập và hãng vận chuyển giả lập dùng chung; không chứa test method. */
abstract class CheckoutTestFixture {
    @Autowired CheckoutService checkout;@Autowired CartService cart;@Autowired ShippingService shipping;@Autowired ShippingSettings settings;
    @Autowired AddressService addresses;@Autowired OrderManagementService management;@Autowired JdbcTemplate db;@Autowired MockMvc mvc;@Autowired ObjectMapper json;
    @Autowired UserRepository users;@Autowired StoreRepository stores;@Autowired ProductRepository products;@Autowired CategoryRepository categories;
    @MockitoBean ShippingProvider provider;
    User buyer,other,seller,admin;Store store;Product product;int shop;
    @BeforeEach void setup(){
        buyer=user(UserRole.CUSTOMER);other=user(UserRole.CUSTOMER);seller=user(UserRole.SELLER);admin=user(UserRole.ADMIN);
        store=new Store();store.setOwner(seller);store.setName("Gian hàng Checkout");store.setStatus(StoreStatus.ACTIVE);store.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));store.setUpdatedAt(store.getCreatedAt());store=stores.save(store);
        db.update("insert into seller_applications(applicant_id,status,name,description,phone,address,reason,processed_by,submitted_at,processed_at,revision) values(?,'APPROVED','Test','Test','0900000000','PRIVATE','',?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0)",seller.getId(),admin.getId());
        var c=new Category();c.setName("Phụ kiện");c.setSlug(UUID.randomUUID().toString());c.setIsActive(true);c.setSortOrder(0);c.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));c.setUpdatedAt(c.getCreatedAt());c=categories.save(c);
        product=new Product();product.setName("Phụ kiện kiểm thử");product.setSlug(UUID.randomUUID().toString());product.setStore(store);product.setCategory(c);product.setStatus(ProductStatus.ACTIVE);product.setStock(10);product.setPrice(new BigDecimal("100000"));product.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));product.setUpdatedAt(product.getCreatedAt());product=products.save(product);
        shop=10000+store.getId().intValue();settings.setEnabled(true);settings.setToken("test-double-only");settings.getShopIds().put(store.getId(),shop);settings.getCodEnabledStoreIds().add(store.getId());
        when(provider.pickup(shop)).thenReturn(new Pickup(shop,1452,"21007","Test pickup","0900000000","PRIVATE PICKUP"));
        when(provider.validateAddress(eq(shop),any())).thenAnswer(a->a.getArgument(1));
        when(provider.quoteCod(eq(shop),any(),any(),any(),anyInt(),anyInt())).thenReturn(List.of(new Rate(2,"Dịch vụ thử nghiệm",new BigDecimal("24000"))));
        when(provider.create(eq(shop),any())).thenAnswer(a->{Create r=a.getArgument(1);return new Remote("STAGE"+store.getId(),r.clientReference(),shop,null,null);});
        when(provider.find(eq(shop),any(),anyString())).thenAnswer(a->new Remote("STAGE"+store.getId(),a.getArgument(2),shop,"ready_to_pick",Instant.now()));
        checkout.savePackage(seller.getId(),UUID.randomUUID().toString(),new CheckoutService.PackageWrite(List.of(new CheckoutService.Content(product.getId(),1)),new ShippingService.ParcelWrite(800,25,20,15,true,0),true,0));
        cart.add(buyer.getId(),product.getId(),1);
    }
    User user(UserRole role){var u=new User();u.setName("Checkout QA");u.setEmail(UUID.randomUUID()+"@example.invalid");u.setPasswordHash("!");u.setIsActive(true);u.setRole(role);u.setEmailVerifiedAt(LocalDateTime.now(ZoneOffset.UTC));u.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));u.setUpdatedAt(u.getCreatedAt());return users.save(u);}
    RequestPostProcessor as(User u){return authentication(UsernamePasswordAuthenticationToken.authenticated(AuthPrincipal.of(u),null,List.of(new SimpleGrantedAuthority("ROLE_"+u.getRole()))));}
    AddressDtos.Write delivery(){return new AddressDtos.Write("","Người nhận thử nghiệm","0901234567","Hồ Chí Minh","Quận 1","Phường Bến Nghé","PRIVATE DELIVERY",false);}
    CheckoutService.QuoteView quote(User u){return checkout.quote(u.getId(),new CheckoutService.QuoteInput(store.getId(),checkout.preview(u.getId()).fingerprint(),null,delivery())).getFirst();}
    CheckoutService.Place body(CheckoutService.QuoteView q){return new CheckoutService.Place(q.id(),UUID.randomUUID().toString(),"COD","Ghi chú");}
    void code(String expected,Runnable action){assertEquals(expected,assertThrows(MarketplaceException.class,action::run).code);}
    int stock(){return products.findById(product.getId()).orElseThrow().getStock();}
}
