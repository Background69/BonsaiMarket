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
    private final com.bonsaimarket.backend.product.ProductPotSpecsRepository potSpecs;

    public CatalogController(ProductRepository products, CategoryRepository categories, StoreRepository stores, CatalogSearch search,RelatedStoresService relatedStores,StoreDiscovery discovery,ProductRecommendations recommendations,com.bonsaimarket.backend.product.ProductPotSpecsRepository potSpecs) {
        this.products = products;
        this.categories = categories;
        this.stores = stores;
        this.search = search;
        this.relatedStores = relatedStores;
        this.discovery=discovery;
        this.recommendations=recommendations;
        this.potSpecs=potSpecs;
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
                .map(p -> ProductDto.detail(p,com.bonsaimarket.backend.product.PotSpecifications.from(potSpecs.findById(p.getId()).orElse(null))))
                .orElseThrow(() -> new CatalogNotFoundException("Product not found"));
    }

    @GetMapping("/products/best-sellers")
    public ProductPage bestSellers(){return products(java.util.Map.of("sort","sold_desc","has_sales","true","size","8"));}

    @GetMapping("/stores")
    public Object stores(@org.springframework.web.bind.annotation.RequestParam java.util.Map<String,String> params) {
        var result=discovery.search(params.isEmpty()?java.util.Map.of("size","50"):params);
        // Giữ phản hồi mảng cho bộ chọn danh mục cũ; trang khám phá dùng phân trang.
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

    public record CategoryDto(Long id, String name, String slug, String description,String imageUrl,String specificationType) {
        static CategoryDto from(Category category) {
            return new CategoryDto(category.getId(), category.getName(), category.getSlug(), category.getDescription(),category.getImageUrl(),category.getSpecificationType());
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
                             CategoryDto category, StoreDto store,ProductSpecifications specifications,
                             com.bonsaimarket.backend.product.PotSpecifications potSpecs,String descriptionFormat) {
        static ProductDto from(Product product) {
            return new ProductDto(product.getId(), product.getName(), product.getSlug(), product.getDescription(),
                    product.getPrice(), product.getComparePrice(), product.getStock(), product.getImageUrl(),
                    product.getStatus().name(), product.getCategory() == null ? null : CategoryDto.from(product.getCategory()),
                    product.getStore() == null ? null : StoreDto.from(product.getStore()),ProductSpecifications.read(product.getTechnicalSpecsJson()),null,product.getDescriptionFormat());
        }
        static ProductDto detail(Product p,com.bonsaimarket.backend.product.PotSpecifications pot){
            var dto=from(p);return new ProductDto(dto.id,dto.name,dto.slug,dto.description,dto.price,dto.comparePrice,dto.stock,dto.imageUrl,dto.status,dto.category,dto.store,dto.specifications.withPot(pot),pot,dto.descriptionFormat);
        }
    }

    static class CatalogNotFoundException extends RuntimeException {
        CatalogNotFoundException(String message) {
            super(message);
        }
    }
}
