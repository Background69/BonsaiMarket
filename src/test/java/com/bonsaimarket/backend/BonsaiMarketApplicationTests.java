package com.bonsaimarket.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.bonsaimarket.backend.category.CategoryRepository;
import com.bonsaimarket.backend.product.ProductRepository;
import com.bonsaimarket.backend.product.ProductStatus;
import com.bonsaimarket.backend.product.Product;
import com.bonsaimarket.backend.category.Category;
import com.bonsaimarket.backend.store.Store;
import com.bonsaimarket.backend.store.StoreStatus;
import java.math.BigDecimal;
import com.bonsaimarket.backend.store.StoreRepository;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// This smoke test checks the web/security context without requiring a local MySQL service.
// Flyway and Hibernate validation run when the application starts against MySQL.
@SpringBootTest(properties = "spring.autoconfigure.exclude="
        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
        + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration,"
        + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
        + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration")
@AutoConfigureMockMvc
class BonsaiMarketApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryRepository categories;

    @MockitoBean
    private ProductRepository products;

    @MockitoBean
    private StoreRepository stores;

    @Test
    void contextLoads() {
    }

    @Test
    void healthIsPublicAndUpWithoutDatabase() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void catalogReadIsPublicWithoutDatabase() throws Exception {
        when(categories.findByIsActiveTrueOrderBySortOrderAscIdAsc()).thenReturn(List.of());
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void missingPublicProductReturns404() throws Exception {
        when(products.findByIdAndStatus(99L, ProductStatus.ACTIVE)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Product not found"));
    }

    @Test
    void productDetailReturnsDtoFields() throws Exception {
        var category = new Category();
        category.setId(1L);
        category.setName("Bonsai");
        var store = new Store();
        store.setId(1L);
        store.setName("Vườn Xanh");
        store.setStatus(StoreStatus.ACTIVE);
        var product = new Product();
        product.setId(1L);
        product.setName("Tùng La Hán");
        product.setPrice(new BigDecimal("850000"));
        product.setStatus(ProductStatus.ACTIVE);
        product.setCategory(category);
        product.setStore(store);
        when(products.findByIdAndStatus(1L, ProductStatus.ACTIVE)).thenReturn(Optional.of(product));

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tùng La Hán"))
                .andExpect(jsonPath("$.category.name").value("Bonsai"))
                .andExpect(jsonPath("$.store.name").value("Vườn Xanh"));
    }

}
