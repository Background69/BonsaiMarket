package com.bonsaimarket.backend.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InventoryDebitRepository extends JpaRepository<InventoryDebit,Long> {
    // A current read is needed even when the caller already opened a REPEATABLE READ snapshot.
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    List<InventoryDebit> findByOrderIdOrderByProductIdAsc(Long orderId);
}
