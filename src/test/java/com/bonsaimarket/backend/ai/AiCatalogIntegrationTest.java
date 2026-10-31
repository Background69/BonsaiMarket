package com.bonsaimarket.backend.ai;

import com.bonsaimarket.backend.product.*;
import com.bonsaimarket.backend.store.*;
import com.bonsaimarket.backend.category.*;
import com.bonsaimarket.backend.user.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

/** Truy vấn repository thật trên H2 dùng riêng; không chứng minh Flyway/MySQL/nhà cung cấp. */
@SpringBootTest(properties={"spring.profiles.active=marketplace-test", "spring.datasource.url=jdbc:h2:mem:ai-catalog;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
        "spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=create-drop"})
@Transactional
class AiCatalogIntegrationTest {
    @Autowired CatalogContextService catalog;
    @Autowired ProductRepository products;
    @Autowired StoreRepository stores;
    @Autowired CategoryRepository categories;
    @Autowired UserRepository users;

    Store shop(String name, boolean activeOwner) {
        var owner = new User(); owner.setName(name); owner.setEmail(name+"@example.test");
        owner.setPasswordHash("disabled-fixture");owner.setRole(UserRole.SELLER);owner.setIsActive(activeOwner); users.save(owner);
        var store = new Store(); store.setName(name); store.setOwner(owner); store.setStatus(StoreStatus.ACTIVE);
        return stores.save(store);
    }
    Product item(Store store, String name, boolean activeCategory, int stock) {
        var category = new Category();category.setName(name);category.setSlug(name);category.setIsActive(activeCategory);categories.save(category);
        var p = new Product();p.setName(name);p.setSlug(name);p.setStatus(ProductStatus.ACTIVE);p.setStore(store);p.setCategory(category);
        p.setPrice(new BigDecimal("450000.50"));p.setStock(stock);return products.save(p);
    }

    @Test void explicitProductRetainsSoldOutWhileListingsRequireAvailableStock() {
        var p=item(shop("stock",true),"Tung",true,0);
        var exact=catalog.retrieve("Sản phẩm #"+p.getId()+" còn hàng không?");
        assertEquals(1,exact.products().size());assertEquals(0,exact.products().getFirst().stock());
        assertTrue(catalog.retrieve("Tung còn hàng").products().isEmpty());
    }
    @Test void hiddenCategoryDraftAndInactiveStoreNeverEnterContext() {
        var store=shop("visibility",true);
        var hidden=item(store,"hidden",false,2);
        assertTrue(catalog.retrieve("Sản phẩm #"+hidden.getId()).products().isEmpty());
        var draft=item(store,"draft",true,2);draft.setStatus(ProductStatus.DRAFT);products.saveAndFlush(draft);
        assertTrue(catalog.retrieve("Sản phẩm #"+draft.getId()).products().isEmpty());
        var active=item(store,"active",true,2);store.setStatus(StoreStatus.INACTIVE);stores.saveAndFlush(store);
        assertTrue(catalog.retrieve("Sản phẩm #"+active.getId()).products().isEmpty());
    }
    @Test void exactStoreLookupBeyondFirstPageUsesDirectoryOwnerVisibility() {
        for(int i=0;i<11;i++)shop("public"+i,true);
        var target=shop("target",true);var privateOwner=shop("private",false);
        assertEquals(target.getId(),catalog.retrieve("Gian hàng #"+target.getId()).stores().getFirst().id());
        assertTrue(catalog.retrieve("Gian hàng #"+privateOwner.getId()).stores().isEmpty());
        assertTrue(catalog.retrieve("Tìm gian hàng").stores().size()<=10);
        assertEquals(1,catalog.retrieve("Tìm gian hàng target").stores().size());
    }
}
