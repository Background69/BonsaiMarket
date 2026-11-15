package com.bonsaimarket.backend.media;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductGalleryRepository extends JpaRepository<ProductGalleryImage,Long> {
    List<ProductGalleryImage> findByProductIdOrderBySortOrderAscIdAsc(Long productId);
}
