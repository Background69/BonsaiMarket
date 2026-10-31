package com.bonsaimarket.backend.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InventoryDebitRepository extends JpaRepository<InventoryDebit,Long> {
    // Cần đọc dữ liệu mới nhất kể cả khi bên gọi đã mở snapshot REPEATABLE READ.
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    List<InventoryDebit> findByOrderIdOrderByProductIdAsc(Long orderId);
}
