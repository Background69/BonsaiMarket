package com.bonsaimarket.backend.store;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {
    List<Store> findByStatusOrderByIdAsc(StoreStatus status);
    Optional<Store> findByIdAndStatus(Long id, StoreStatus status);
}
