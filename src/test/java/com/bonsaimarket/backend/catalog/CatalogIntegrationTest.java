package com.bonsaimarket.backend.catalog;

import com.bonsaimarket.backend.product.*;
import com.bonsaimarket.backend.store.*;
import com.bonsaimarket.backend.category.*;
import com.bonsaimarket.backend.user.*;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Truy vấn DB và MVC trên H2 cô lập; chưa kiểm tra collation hoặc query plan MySQL. */
@SpringBootTest(properties={"spring.profiles.active=marketplace-test", "spring.datasource.url=jdbc:h2:mem:catalog;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.generate_statistics=true"})
@AutoConfigureMockMvc
class CatalogIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CatalogSearch search;
    @Autowired ProductRepository products;
    @Autowired StoreRepository stores;
    @Autowired CategoryRepository categories;
    @Autowired UserRepository users;
    @Autowired EntityManagerFactory factory;
    Store store; Category category;
    @BeforeEach void setup() {
        products.deleteAll(); stores.deleteAll(); categories.deleteAll(); users.deleteAll();
        var owner = new User(); owner.setName("Seller"); owner.setEmail("catalog@example.com"); owner.setPasswordHash("hash"); owner.setRole(UserRole.SELLER); owner.setIsActive(true); users.save(owner);
        category = new Category(); category.setName("Bonsai"); category.setSlug("bonsai"); category.setIsActive(true); category.setSortOrder(0); categories.save(category);
        store = new Store(); store.setName("Store"); store.setOwner(owner); store.setStatus(StoreStatus.ACTIVE); stores.save(store);
    }
    Product product(String name, int price, int stock) {
        var p = new Product(); p.setName(name); p.setSlug("item-" + products.count()); p.setDescription("Cây để bàn và chậu gốm");
        p.setCategory(category); p.setStore(store); p.setStatus(ProductStatus.ACTIVE); p.setStock(stock);
        p.setPrice(BigDecimal.valueOf(price)); p.setCreatedAt(LocalDateTime.of(2026,1,1,0,0)); return products.save(p);
    }
    @Test void exactPartialMixedCaseUnicodeAndWhitespaceSearch() throws Exception {
        product("Mai Chiếu Thủy", 300000, 1);
        for (String term : new String[]{"Mai Chiếu Thủy", "Chiếu", "mAI cHIẾU", "  Mai   Chiếu\tThủy  ", "", "  "})
            mvc.perform(get("/api/products").param("search",term)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }
    @Test void searchDescriptionCategoryAndSlug() {
        product("Plant", 100, 1);
        for (String term : new String[]{"chậu gốm", "Bonsai", "item-0"}) assertEquals(1, search.search(Map.of("search",term)).getTotalElements());
    }
    @Test void emptyNoResultsAndSpecialCharactersAreLiteral() throws Exception {
        product("Mai", 100, 1);
        for (String term : new String[]{"missing", "%", "_", "' OR 1=1 --", "!", "\\"})
            mvc.perform(get("/api/products").param("q",term)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }
    @Test void excludesUnpublishedInactiveStoreAndCategoryInListAndDetail() throws Exception {
        var p = product("Mai",100,1);
        for (ProductStatus state : new ProductStatus[]{ProductStatus.DRAFT,ProductStatus.PENDING,ProductStatus.ARCHIVED,ProductStatus.REJECTED}) {
            p.setStatus(state); products.save(p);
            assertEquals(0,search.search(Map.of()).getTotalElements()); mvc.perform(get("/api/products/"+p.getId())).andExpect(status().isNotFound());
        }
        p.setStatus(ProductStatus.ACTIVE);products.save(p);store.setStatus(StoreStatus.INACTIVE);stores.save(store);
        assertEquals(0,search.search(Map.of()).getTotalElements());mvc.perform(get("/api/products/"+p.getId())).andExpect(status().isNotFound());
        assertTrue(products.findAiPublicCatalog("",null,null,false,null,org.springframework.data.domain.PageRequest.of(0,10)).isEmpty());
        store.setStatus(StoreStatus.ACTIVE);stores.save(store);category.setIsActive(false);categories.save(category);
        assertEquals(0,search.search(Map.of()).getTotalElements());mvc.perform(get("/api/products/"+p.getId())).andExpect(status().isNotFound());
    }
    @Test void categoryPriceStockAndStoreFiltersCompose() {
        product("Mai",100,1); product("Mai",200,0); product("Other",300,5);
        assertEquals(1,search.search(Map.of("search","Mai","category","bonsai","categoryId",category.getId().toString(),"store",store.getId().toString(),"min_price","100","max_price","150","in_stock","true")).getTotalElements());
        assertEquals(1,search.search(Map.of("in_stock","false")).getTotalElements());
        assertEquals(2,search.search(Map.of("min_price","200")).getTotalElements());
        assertEquals(2,search.search(Map.of("max_price","200")).getTotalElements());
        assertEquals(0,search.search(Map.of("category","missing")).getTotalElements());
        assertEquals(0,search.search(Map.of("store","999999")).getTotalElements());
    }
    @Test void invalidInputsAre400AndDoNotExposeInternalErrors() throws Exception {
        for (var entry : Map.ofEntries(Map.entry("min_price","-1"),Map.entry("max_price","NaN"),Map.entry("sort","price desc;drop table products"),Map.entry("page","-1"),Map.entry("size","0"),Map.entry("categoryId","x"),Map.entry("store","0"),Map.entry("in_stock","yes"),Map.entry("condition","NEW"),Map.entry("search","a".repeat(161))).entrySet())
            mvc.perform(get("/api/products").param(entry.getKey(),entry.getValue())).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_CATALOG_QUERY"));
        mvc.perform(get("/api/products").param("min_price","200").param("max_price","100")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/products").param("search","a ".repeat(13))).andExpect(status().isBadRequest());
    }
    @Test void boundedPageSizesAndOffsets() throws Exception {
        for (String size : new String[]{"51","2147483648","bad"}) mvc.perform(get("/api/products").param("size",size)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/products").param("page","10001")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/products").param("page","0").param("size","50")).andExpect(status().isOk()).andExpect(jsonPath("$.size").value(50));
    }
    @Test void priceSortAndUniqueTieBreakers() {
        var a=product("Mai",200,1); var b=product("Mai",100,1); var c=product("Mai",200,1);
        assertEquals(java.util.List.of(b.getId(),a.getId(),c.getId()),search.search(Map.of("sort","price_asc")).stream().map(Product::getId).toList());
        assertEquals(java.util.List.of(a.getId(),c.getId(),b.getId()),search.search(Map.of("sort","price_desc")).stream().map(Product::getId).toList());
    }
    @Test void newestUsesTimestampRatherThanId() {
        var earlier=product("Mai",100,1); earlier.setCreatedAt(LocalDateTime.of(2026,10,1,0,0));products.save(earlier);
        product("Mai",200,1);
        assertEquals(earlier.getId(),search.search(Map.of("sort","latest")).getContent().getFirst().getId());
    }
    @Test void firstMiddleLastAndBeyondPageSearchRemainStable() {
        for(int i=0;i<65;i++) product("Mai "+i,100,1);
        var ids=new java.util.HashSet<Long>();
        for(int page=0;page<4;page++) {var data=search.search(Map.of("search","Mai","page",""+page));assertEquals(65,data.getTotalElements());assertEquals(page==3?5:20,data.getNumberOfElements());data.forEach(p->assertTrue(ids.add(p.getId())));}
        assertEquals(65,ids.size());assertTrue(search.search(Map.of("page","4")).isEmpty());
    }
    @Test void pageFetchDoesNotNPlusOneOrReturnEntities() throws Exception {
        for(int i=0;i<25;i++) product("Mai "+i,100,1);
        var stats=factory.unwrap(SessionFactory.class).getStatistics();stats.clear();
        mvc.perform(get("/api/products")).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(20)).andExpect(jsonPath("$.totalElements").value(25))
            .andExpect(jsonPath("$.content[0].store.owner").doesNotExist()).andExpect(jsonPath("$.content[0].technicalSpecsJson").doesNotExist());
        assertTrue(stats.getPrepareStatementCount() <= 2,"page + count only");
    }
}
