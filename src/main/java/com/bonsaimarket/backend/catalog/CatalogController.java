package com.bonsaimarket.backend.catalog;

import com.bonsaimarket.backend.category.Category;
import com.bonsaimarket.backend.category.CategoryRepository;
import com.bonsaimarket.backend.product.Product;
import com.bonsaimarket.backend.product.ProductRepository;
import com.bonsaimarket.backend.product.ProductStatus;
import com.bonsaimarket.backend.store.Store;
import com.bonsaimarket.backend.store.StoreRepository;
import com.bonsaimarket.backend.store.StoreStatus;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final StoreRepository stores;

    public CatalogController(ProductRepository products, CategoryRepository categories, StoreRepository stores) {
        this.products = products;
        this.categories = categories;
        this.stores = stores;
    }

    @GetMapping("/categories")
    public List<CategoryDto> categories() {
        return categories.findByIsActiveTrueOrderBySortOrderAscIdAsc().stream().map(CategoryDto::from).toList();
    }

    @GetMapping("/products")
    public List<ProductDto> products() {
        return products.findByStatusOrderByIdAsc(ProductStatus.ACTIVE).stream().map(ProductDto::from).toList();
    }

    @GetMapping("/products/{id}")
    public ProductDto product(@PathVariable Long id) {
        return products.findByIdAndStatus(id, ProductStatus.ACTIVE).map(ProductDto::from)
                .orElseThrow(() -> new CatalogNotFoundException("Product not found"));
    }

    @GetMapping("/stores")
    public List<StoreDto> stores() {
        return stores.findByStatusOrderByIdAsc(StoreStatus.ACTIVE).stream().map(StoreDto::from).toList();
    }

    @GetMapping("/stores/{id}")
    public StoreDto store(@PathVariable Long id) {
        return stores.findByIdAndStatus(id, StoreStatus.ACTIVE).map(StoreDto::from)
                .orElseThrow(() -> new CatalogNotFoundException("Store not found"));
    }

    public record CategoryDto(Long id, String name, String slug, String description) {
        static CategoryDto from(Category category) {
            return new CategoryDto(category.getId(), category.getName(), category.getSlug(), category.getDescription());
        }
    }

    public record StoreDto(Long id, String name, String description, String logoUrl, String phone, String address, String status) {
        static StoreDto from(Store store) {
            return new StoreDto(store.getId(), store.getName(), store.getDescription(), store.getLogoUrl(),
                    store.getPhone(), store.getAddress(), store.getStatus().name());
        }
    }

    public record ProductDto(Long id, String name, String slug, String description, BigDecimal price,
                             BigDecimal comparePrice, Integer stock, String imageUrl, String status,
                             CategoryDto category, StoreDto store) {
        static ProductDto from(Product product) {
            return new ProductDto(product.getId(), product.getName(), product.getSlug(), product.getDescription(),
                    product.getPrice(), product.getComparePrice(), product.getStock(), product.getImageUrl(),
                    product.getStatus().name(), product.getCategory() == null ? null : CategoryDto.from(product.getCategory()),
                    product.getStore() == null ? null : StoreDto.from(product.getStore()));
        }
    }

    static class CatalogNotFoundException extends RuntimeException {
        CatalogNotFoundException(String message) { super(message); }
    }
}
