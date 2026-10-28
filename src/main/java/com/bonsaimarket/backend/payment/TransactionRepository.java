package com.bonsaimarket.backend.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTransactionCode(String transactionCode);
    List<Transaction> findByOrderIdOrderByCreatedAtDesc(Long orderId);
}
