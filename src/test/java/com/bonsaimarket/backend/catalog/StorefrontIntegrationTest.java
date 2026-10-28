package com.bonsaimarket.backend.catalog;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import com.bonsaimarket.backend.category.*;
import com.bonsaimarket.backend.product.*;
import com.bonsaimarket.backend.store.*;
import com.bonsaimarket.backend.user.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Repository H2 và MockMvc cô lập; không xác nhận backend/MySQL đang chạy thực tế. */
@SpringBootTest(properties={"spring.profiles.active=storefront-test","spring.datasource.url=jdbc:h2:mem:storefront;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
    "spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.properties.hibernate.generate_statistics=true"})
@AutoConfigureMockMvc
class StorefrontIntegrationTest {
    @Autowired MockMvc mvc; @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users; @Autowired StoreRepository stores; @Autowired ProductRepository products; @Autowired CategoryRepository categories;
    @Autowired jakarta.persistence.EntityManagerFactory factory;
    User seller,other,buyer;Store store,otherStore;Category category,alternate;
    @BeforeEach void setup(){
        jdbc.update("delete from products");jdbc.update("delete from stores");jdbc.update("delete from categories");jdbc.update("delete from users");
        seller=user(UserRole.SELLER);other=user(UserRole.SELLER);buyer=user(UserRole.CUSTOMER);
        store=shop(seller,"Vườn Hiên");otherStore=shop(other,"Nắng Sớm");category=category("sen-da");alternate=category("bonsai");
    }
    User user(UserRole role){var u=new User();u.setName("Demo");u.setEmail(UUID.randomUUID()+"@example.invalid");u.setPasswordHash("!");u.setRole(role);u.setIsActive(true);u.setEmailVerifiedAt(LocalDateTime.now());return users.save(u);}
    Store shop(User u,String name){var s=new Store();s.setName(name);s.setStatus(StoreStatus.ACTIVE);s.setOwner(u);return stores.save(s);}
    Category category(String slug){var c=new Category();c.setName(slug);c.setSlug(slug);c.setIsActive(true);return categories.save(c);}
    Product product(Store s,Category c,ProductStatus status){var p=new Product();p.setName("Sen đá");p.setSlug(UUID.randomUUID().toString());p.setStore(s);p.setCategory(c);p.setStatus(status);p.setStock(2);p.setPrice(new BigDecimal("120000"));return products.save(p);}
    RequestPostProcessor as(User u){return authentication(UsernamePasswordAuthenticationToken.authenticated(AuthPrincipal.of(u),null,List.of(new SimpleGrantedAuthority("ROLE_"+u.getRole()))));}
    String body(String specs,long revision){return "{\"description\":\"## Giới thiệu\\nNội dung riêng của người bán.\",\"specifications\":"+specs+",\"revision\":"+revision+"}";}

    @Test void publicDetailsExposeTypedSpecsAndStorefrontWithoutInternalJsonOrAccounts()throws Exception{
        var p=product(store,category,ProductStatus.ACTIVE);p.setTechnicalSpecsJson("{\"heightCm\":12.5,\"light\":\"INDIRECT\",\"secretLegacy\":\"private\"}");products.save(p);
        store.setBannerUrl("https://images.pexels.com/photo.jpg");store.setAboutShop("Giới thiệu\n\nGóc vườn riêng.");stores.save(store);
        mvc.perform(get("/api/products/"+p.getId())).andExpect(status().isOk()).andExpect(jsonPath("$.specifications.heightCm").value(12.5))
            .andExpect(jsonPath("$.store.aboutShop").value(store.getAboutShop())).andExpect(jsonPath("$.store.bannerUrl").value(store.getBannerUrl()))
            .andExpect(jsonPath("$.technicalSpecsJson").doesNotExist()).andExpect(jsonPath("$.specifications.secretLegacy").doesNotExist()).andExpect(jsonPath("$.store.owner").doesNotExist());
        mvc.perform(get("/api/stores/"+store.getId())).andExpect(status().isOk()).andExpect(jsonPath("$.aboutShop").value(store.getAboutShop()));
    }
    @Test void categoryDiscoveryExcludesCurrentDraftInactiveAndOtherCategoriesAndPreservesZeroStock()throws Exception{
        var current=product(store,category,ProductStatus.ACTIVE);var matching=product(otherStore,category,ProductStatus.ACTIVE);matching.setStock(0);products.save(matching);
        product(otherStore,alternate,ProductStatus.ACTIVE);product(otherStore,category,ProductStatus.DRAFT);
        var hidden=shop(user(UserRole.SELLER),"Hidden");hidden.setStatus(StoreStatus.INACTIVE);stores.save(hidden);product(hidden,category,ProductStatus.ACTIVE);
        var unapproved=shop(buyer,"Unapproved");product(unapproved,category,ProductStatus.ACTIVE);
        mvc.perform(get("/api/products/"+current.getId()+"/related-stores")).andExpect(status().isOk()).andExpect(jsonPath("$.category.id").value(category.getId()))
            .andExpect(jsonPath("$.stores.length()").value(1)).andExpect(jsonPath("$.stores[0].store.id").value(otherStore.getId()))
            .andExpect(jsonPath("$.stores[0].matchingProductCount").value(1)).andExpect(jsonPath("$.stores[0].products[0].id").value(matching.getId())).andExpect(jsonPath("$.stores[0].products[0].stock").value(0));
    }
    @Test void discoveryHasFixedBoundsStableOrderAndConstantQueryCount()throws Exception{
        var current=product(store,category,ProductStatus.ACTIVE);
        for(int i=0;i<9;i++){var s=shop(user(UserRole.SELLER),"Shop "+i);for(int j=0;j<5;j++)product(s,category,ProductStatus.ACTIVE);}
        var stats=factory.unwrap(org.hibernate.SessionFactory.class).getStatistics();stats.clear();
        var first=mvc.perform(get("/api/products/"+current.getId()+"/related-stores?limit=9999")).andExpect(status().isOk())
            .andExpect(jsonPath("$.stores.length()").value(6)).andExpect(jsonPath("$.stores[0].matchingProductCount").value(5))
            .andExpect(jsonPath("$.stores[0].products.length()").value(3)).andExpect(jsonPath("$.stores[5].products.length()").value(3)).andReturn().getResponse().getContentAsString();
        assertTrue(stats.getPrepareStatementCount()<=5,"Bounded eager queries: "+stats.getPrepareStatementCount());
        var second=mvc.perform(get("/api/products/"+current.getId()+"/related-stores")).andReturn().getResponse().getContentAsString();assertEquals(first,second);
    }
    @Test void absentHiddenAndInvalidProductIdsDoNotExposeDiscovery()throws Exception{
        var current=product(store,category,ProductStatus.ACTIVE);
        mvc.perform(get("/api/products/"+current.getId()+"/related-stores")).andExpect(status().isOk()).andExpect(jsonPath("$.stores.length()").value(0));
        category.setIsActive(false);categories.save(category);
        mvc.perform(get("/api/products/"+current.getId()+"/related-stores")).andExpect(status().isNotFound());
        mvc.perform(get("/api/products/999999/related-stores")).andExpect(status().isNotFound());
        mvc.perform(get("/api/products/wrong/related-stores")).andExpect(status().isBadRequest());
    }
    @Test void storeScopedFilterAndPaginationCountOnlyPublicMatchingRows()throws Exception{
        for(int i=0;i<5;i++)product(otherStore,category,ProductStatus.ACTIVE);product(otherStore,alternate,ProductStatus.ACTIVE);product(otherStore,category,ProductStatus.DRAFT);product(store,category,ProductStatus.ACTIVE);
        mvc.perform(get("/api/products?store="+otherStore.getId()+"&categoryId="+category.getId()+"&size=2&page=1&sort=price_asc&in_stock=1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(5)).andExpect(jsonPath("$.totalPages").value(3)).andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].store.id").value(otherStore.getId())).andExpect(jsonPath("$.content[0].category.id").value(category.getId()));
    }
    @Test void ownedContentEditsPreserveStockStatusImagesUnknownJsonAndDetectStaleWrites()throws Exception{
        var p=product(store,category,ProductStatus.DRAFT);p.setTechnicalSpecsJson("{\"historical\":{\"keep\":true}}");p.setImageUrl("https://example.org/tree.jpg");products.save(p);
        String url="/api/seller/products/"+p.getId()+"/content";
        mvc.perform(get(url).with(as(other))).andExpect(status().isNotFound());mvc.perform(get(url).with(as(buyer))).andExpect(status().isForbidden());
        mvc.perform(put(url).with(as(seller)).with(csrf()).contentType("application/json").content(body("{\"heightCm\":12.5,\"light\":\"INDIRECT\"}",0)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.revision").value(1)).andExpect(jsonPath("$.specifications.heightCm").value(12.5));
        var saved=products.findById(p.getId()).orElseThrow();assertEquals(2,saved.getStock());assertEquals(ProductStatus.DRAFT,saved.getStatus());assertEquals(p.getImageUrl(),saved.getImageUrl());assertTrue(saved.getTechnicalSpecsJson().contains("historical"));
        mvc.perform(put(url).with(as(seller)).with(csrf()).contentType("application/json").content(body("{}",0))).andExpect(status().isConflict());
        mvc.perform(put(url).with(as(other)).with(csrf()).contentType("application/json").content(body("{}",1))).andExpect(status().isNotFound());
    }
    @Test void invalidMeasurementsEnumTextAndMissingCsrfAreRejected()throws Exception{
        var p=product(store,category,ProductStatus.ACTIVE);String url="/api/seller/products/"+p.getId()+"/content";
        for(String specs:List.of("{\"heightCm\":-1}","{\"heightCm\":2001}","{\"potDiameterCm\":501}","{\"heightCm\":12.55}","{\"light\":\"UNKNOWN\"}","{\"scientificName\":\""+"x".repeat(161)+"\"}"))
            mvc.perform(put(url).with(as(seller)).with(csrf()).contentType("application/json").content(body(specs,0))).andExpect(status().isBadRequest());
        mvc.perform(put(url).with(as(seller)).contentType("application/json").content(body("{}",0))).andExpect(status().isForbidden());
        mvc.perform(put(url).with(as(seller)).with(csrf()).contentType("application/json").content(body("{}",0).replace("Nội dung riêng của người bán.","a".repeat(16001)))).andExpect(status().isBadRequest());
        assertEquals(0,products.findById(p.getId()).orElseThrow().getContentRevision());
    }
    @Test void malformedHistoricalSpecificationsAreNotOverwritten()throws Exception{
        var p=product(store,category,ProductStatus.ACTIVE);p.setTechnicalSpecsJson("legacy: unstructured");products.save(p);
        mvc.perform(get("/api/products/"+p.getId())).andExpect(status().isOk()).andExpect(jsonPath("$.specifications.heightCm").isEmpty());
        mvc.perform(put("/api/seller/products/"+p.getId()+"/content").with(as(seller)).with(csrf()).contentType("application/json").content(body("{}",0))).andExpect(status().isConflict());
        assertEquals("legacy: unstructured",products.findById(p.getId()).orElseThrow().getTechnicalSpecsJson());
    }
    @Test void storeSettingsValidateUrlsPreserveOldClientsAndUseSessionOwnership()throws Exception{
        store.setBannerUrl("https://images.pexels.com/old.jpg");store.setAboutShop("Original");stores.save(store);
        String base="{\"name\":\"Edited\",\"description\":\"Summary\",\"phone\":\"\",\"address\":\"Huế\",\"revision\":0";
        for(String url:List.of("javascript:alert(1)","data:image/png;base64,xx","http://images.pexels.com/a.jpg","https://127.0.0.1/a.jpg","https://localhost/a.jpg","https://user:pass@example.org/a.jpg"))
            mvc.perform(put("/api/seller/store").with(as(seller)).with(csrf()).contentType("application/json").content(base+",\"bannerUrl\":\""+url+"\"}")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/seller/store").with(as(seller)).with(csrf()).contentType("application/json").content(base+"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.bannerUrl").value(store.getBannerUrl())).andExpect(jsonPath("$.aboutShop").value("Original"));
        mvc.perform(put("/api/seller/store").with(as(seller)).with(csrf()).contentType("application/json").content(base.replace(":0",":1")+",\"bannerUrl\":\"\",\"aboutShop\":\"## Vườn\\nThông tin mới\",\"logoUrl\":\"https://images.pexels.com/logo.jpg\",\"ownerId\":"+other.getId()+"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.bannerUrl").isEmpty()).andExpect(jsonPath("$.aboutShop").value("## Vườn\nThông tin mới"));
        assertEquals("Nắng Sớm",stores.findById(otherStore.getId()).orElseThrow().getName());
    }
}
