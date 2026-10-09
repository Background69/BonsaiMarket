package com.bonsaimarket.backend.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {
    // Bounded AI-only retrieval. Existing public GET methods and their behavior stay intact.
    @EntityGraph(attributePaths = {"category", "store"})
    @Query("""
            select p from Product p join p.store s
            where p.status = com.bonsaimarket.backend.product.ProductStatus.ACTIVE
            and s.status = com.bonsaimarket.backend.store.StoreStatus.ACTIVE
            and (:name = '' or lower(p.name) like lower(concat('%', :name, '%')))
            and (:minPrice is null or p.price >= :minPrice)
            and (:maxPrice is null or p.price <= :maxPrice)
            and (:inStock = false or p.stock > 0)
            and (:productId is null or p.id = :productId)
            order by p.price asc, p.id asc
            """)
    List<Product> findAiPublicCatalog(@Param("name") String name, @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice, @Param("inStock") boolean inStock,
            @Param("productId") Long productId, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "store"})
    List<Product> findByStatusOrderByIdAsc(ProductStatus status);

    @EntityGraph(attributePaths = {"category", "store"})
    Optional<Product> findByIdAndStatus(Long id, ProductStatus status);
}
