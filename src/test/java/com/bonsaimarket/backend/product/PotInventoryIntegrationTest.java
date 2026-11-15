package com.bonsaimarket.backend.product;

import com.bonsaimarket.backend.category.*;
import com.bonsaimarket.backend.catalog.CatalogSearch;
import com.bonsaimarket.backend.inventory.InventoryService;
import com.bonsaimarket.backend.marketplace.MarketplaceException;
import com.bonsaimarket.backend.store.*;
import com.bonsaimarket.backend.user.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

/** Auth/ownership and database filtering scenarios; H2 only when explicitly run. Not MySQL proof. */
@SpringBootTest(properties={"spring.profiles.active=pot-inventory-test","spring.datasource.url=jdbc:h2:mem:pot-inventory;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=create-drop"})
@Transactional
class PotInventoryIntegrationTest {
    @Autowired EntityManager em;
    @Autowired InventoryService inventory;
    @Autowired ProductContentService content;
    @Autowired CatalogSearch catalog;
    @Autowired CategoryManagementService categories;
    User seller,other,admin;Store store,foreignStore;Category category;
    @BeforeEach void setup(){
        seller=user(UserRole.SELLER);other=user(UserRole.SELLER);admin=user(UserRole.ADMIN);
        store=store(seller);foreignStore=store(other);category=new Category();category.setName("Chậu riêng");category.setSlug("pots-"+UUID.randomUUID());category.setIsActive(true);category.setSpecificationType("POT");em.persist(category);
    }
    User user(UserRole role){var user=new User();user.setName("Test");user.setEmail(UUID.randomUUID()+"@example.invalid");user.setPasswordHash("unused");user.setIsActive(true);user.setRole(role);user.setEmailVerifiedAt(LocalDateTime.now());em.persist(user);return user;}
    Store store(User owner){var row=new Store();row.setOwner(owner);row.setName("Test");row.setStatus(StoreStatus.ACTIVE);em.persist(row);return row;}
    Product product(Store owner,String name,int stock){var p=new Product();p.setStore(owner);p.setCategory(category);p.setName(name);p.setSlug("p-"+UUID.randomUUID());p.setPrice(new BigDecimal("100000"));p.setStock(stock);p.setStatus(ProductStatus.ACTIVE);em.persist(p);em.flush();return p;}
    PotSpecifications specs(){return new PotSpecifications("Gốm","ROUND","Trắng",new BigDecimal("12.0"),null,null,new BigDecimal("8.0"),"YES",1,450,false,"Sen đá","Lau sạch trước khi dùng.");}
    @Test void inventoryFiltersBeforePaginationAndNeverUsesAnotherSellersRows(){
        for(int i=0;i<43;i++)product(store,"Chậu "+i,i%2);
        product(foreignStore,"Chậu khác",99);
        var page=inventory.list(seller.getId(),1,5,"Chậu","available","ACTIVE",category.getId(),"stock_desc");
        assertEquals(21,page.totalElements());assertEquals(5,page.content().size());assertEquals(5,page.totalPages());assertTrue(page.content().stream().allMatch(p->p.storeId().equals(store.getId())&&p.stock()>0));
        assertEquals(0,inventory.list(seller.getId(),0,20,"%","all","all",null,"id_asc").totalElements());
        assertThrows(MarketplaceException.class,()->inventory.list(seller.getId(),0,20,"","all","all",null,"stock desc;drop table products"));
    }
    @Test void potContentUsesOwnershipRevisionAndCanonicalLegacyMirrors(){
        var p=product(store,"Chậu",5);
        var updated=content.update(seller.getId(),p.getId(),new ProductContentService.Write("## Mô tả\n\nChậu **gốm**.",ProductSpecifications.EMPTY,0L,specs(),"MARKDOWN"));
        assertEquals(1,updated.revision());assertEquals("Gốm",updated.specifications().potMaterial());assertEquals(0,new BigDecimal("12").compareTo(updated.specifications().potDiameterCm()));assertEquals(specs(),updated.potSpecs());
        assertThrows(MarketplaceException.class,()->content.update(seller.getId(),p.getId(),new ProductContentService.Write("Stale",ProductSpecifications.EMPTY,0L,specs(),"MARKDOWN")));
        assertThrows(MarketplaceException.class,()->content.update(other.getId(),p.getId(),new ProductContentService.Write("Foreign",ProductSpecifications.EMPTY,1L,specs(),"PLAIN")));
        assertEquals(updated.description(),content.get(seller.getId(),p.getId()).description());
    }
    @Test void oldClientCannotOverwriteCanonicalPotFields(){
        var p=product(store,"Chậu",2);content.update(seller.getId(),p.getId(),new ProductContentService.Write("Text",ProductSpecifications.EMPTY,0L,specs(),"PLAIN"));
        assertEquals("POT_SPECIFICATIONS_REQUIRED",assertThrows(MarketplaceException.class,()->content.update(seller.getId(),p.getId(),new ProductContentService.Write("Old",ProductSpecifications.EMPTY,1L))).code);
    }
    @Test void categoryWithoutMeasurementsCanStillEditDescription(){
        var p=product(store,"Chậu cũ",2);var updated=content.update(seller.getId(),p.getId(),new ProductContentService.Write("Thông tin cũ",ProductSpecifications.EMPTY,0L));
        assertNull(updated.potSpecs());assertTrue(updated.potCategory());
    }
    @Test void descriptionTemplateAndHtmlCannotBePublishedAsFormattedContent(){
        var p=product(store,"Chậu",2);
        for(String text:List.of("<script>alert(1)</script>","## Giới thiệu\n[Thông tin đóng gói thực tế]"))
            assertThrows(MarketplaceException.class,()->content.update(seller.getId(),p.getId(),new ProductContentService.Write(text,ProductSpecifications.EMPTY,0L,null,"MARKDOWN")));
    }
    @Test void catalogPotFiltersReturnFilteredCountsAcrossPages(){
        for(int i=0;i<12;i++){var p=product(store,"Chậu "+i,5);var row=new ProductPotSpecs();row.setProductId(p.getId());specs().apply(row);em.persist(row);}
        product(store,"Không khai báo chậu",5);em.flush();
        var result=catalog.search(Map.of("pot_material","Gốm","pot_shape","ROUND","pot_min_diameter","10","pot_max_diameter","14","pot_drainage","YES","size","5","page","1"));
        assertEquals(12,result.getTotalElements());assertEquals(3,result.getTotalPages());assertEquals(5,result.getNumberOfElements());
        assertThrows(IllegalArgumentException.class,()->catalog.search(Map.of("pot_min_diameter","NaN")));
    }
    @Test void categoryOlderClientsPreserveImageAndType(){
        category.setImageUrl("/images/categories/chau-gom-men.svg");em.flush();
        var updated=categories.update(admin.getId(),category.getId(),new CategoryDtos.CategoryWrite("Chậu riêng","Text",true,0L));
        assertEquals("POT",updated.specificationType());assertEquals("/images/categories/chau-gom-men.svg",updated.imageUrl());
        assertThrows(MarketplaceException.class,()->categories.update(seller.getId(),category.getId(),new CategoryDtos.CategoryWrite("Foreign","",true,1L)));
    }
}
