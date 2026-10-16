package com.bonsaimarket.backend.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    org.springframework.data.domain.Page<Category> findByNameContainingIgnoreCase(String name,org.springframework.data.domain.Pageable page);
    List<Category> findByIsActiveTrueOrderBySortOrderAscIdAsc();
}
