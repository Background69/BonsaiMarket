package com.bonsaimarket.backend.catalog;

import com.bonsaimarket.backend.category.Category;
import com.bonsaimarket.backend.category.CategoryRepository;
import com.bonsaimarket.backend.product.Product;
import com.bonsaimarket.backend.product.ProductRepository;
import com.bonsaimarket.backend.product.ProductStatus;
import com.bonsaimarket.backend.product.ProductSpecifications;
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
    private final CatalogSearch search;
    private final RelatedStoresService relatedStores;
    private final StoreDiscovery discovery;
    private final ProductRecommendations recommendations;

    public CatalogController(ProductRepository products, CategoryRepository categories, StoreRepository stores, CatalogSearch search,RelatedStoresService relatedStores,StoreDiscovery discovery,ProductRecommendations recommendations) {
        this.products = products;
        this.categories = categories;
        this.stores = stores;
        this.search = search;
        this.relatedStores = relatedStores;
        this.discovery=discovery;
        this.recommendations=recommendations;
    }

    @GetMapping("/categories")
    public List<CategoryDto> categories() {
        return categories.findByIsActiveTrueOrderBySortOrderAscIdAsc().stream().map(CategoryDto::from).toList();
    }

    @GetMapping("/products")
    public ProductPage products(@org.springframework.web.bind.annotation.RequestParam java.util.Map<String, String> params) {
        var page = search.search(params);
        return new ProductPage(page.getContent().stream().map(ProductDto::from).toList(), page.getNumber(), page.getSize(),
            page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }

    @GetMapping("/products/{id}")
    public ProductDto product(@PathVariable Long id) {
        return products.findByIdAndStatus(id, ProductStatus.ACTIVE)
                .filter(p -> p.getStore() != null && p.getStore().getStatus() == StoreStatus.ACTIVE
                    && p.getCategory() != null && Boolean.TRUE.equals(p.getCategory().getIsActive()))
                .map(ProductDto::from)
                .orElseThrow(() -> new CatalogNotFoundException("Product not found"));
    }

    @GetMapping("/stores")
    public Object stores(@org.springframework.web.bind.annotation.RequestParam java.util.Map<String,String> params) {
        var result=discovery.search(params.isEmpty()?java.util.Map.of("size","50"):params);
        // Preserve the legacy array contract for existing catalog selectors; directory uses paging.
        return params.isEmpty()?result.content():result;
    }

    @GetMapping("/products/recommendations")
    public List<ProductDto> recommendations(@org.springframework.web.bind.annotation.RequestParam(required=false) List<Long> exclude) {
        return recommendations.find(null,exclude==null?List.of():exclude);
    }

    @GetMapping("/products/{id}/recommendations")
    public List<ProductDto> recommendations(@PathVariable Long id) {return recommendations.find(id,List.of());}

    @GetMapping("/products/{id}/related-stores")
    public RelatedStoresService.Result relatedStores(@PathVariable Long id) {return relatedStores.find(id);}

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

    public record ProductPage(List<ProductDto> content, int page, int size, long totalElements, int totalPages, boolean first, boolean last) { }

    public record StoreDto(Long id, String name, String description, String logoUrl, String phone, String address,
                           String status,String bannerUrl,String aboutShop) {
        static StoreDto from(Store store) {
            return new StoreDto(store.getId(), store.getName(), store.getDescription(), store.getLogoUrl(),
                    store.getPhone(), store.getAddress(), store.getStatus().name(),store.getBannerUrl(),store.getAboutShop());
        }
    }

    public record ProductDto(Long id, String name, String slug, String description, BigDecimal price,
                             BigDecimal comparePrice, Integer stock, String imageUrl, String status,
                             CategoryDto category, StoreDto store,ProductSpecifications specifications) {
        static ProductDto from(Product product) {
            return new ProductDto(product.getId(), product.getName(), product.getSlug(), product.getDescription(),
                    product.getPrice(), product.getComparePrice(), product.getStock(), product.getImageUrl(),
                    product.getStatus().name(), product.getCategory() == null ? null : CategoryDto.from(product.getCategory()),
                    product.getStore() == null ? null : StoreDto.from(product.getStore()),ProductSpecifications.read(product.getTechnicalSpecsJson()));
        }
    }

    static class CatalogNotFoundException extends RuntimeException {
        CatalogNotFoundException(String message) {
            super(message);
        }
    }
}
