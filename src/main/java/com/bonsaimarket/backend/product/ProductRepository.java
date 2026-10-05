package com.bonsaimarket.backend.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @EntityGraph(attributePaths = {"category", "store"})
    List<Product> findByStatusOrderByIdAsc(ProductStatus status);

    @EntityGraph(attributePaths = {"category", "store"})
    Optional<Product> findByIdAndStatus(Long id, ProductStatus status);
}
