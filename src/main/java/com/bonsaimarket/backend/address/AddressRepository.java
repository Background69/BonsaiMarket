package com.bonsaimarket.backend.address;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Long> {
    java.util.List<Address> findByUserIdOrderByIdAsc(Long userId);
    java.util.Optional<Address> findByIdAndUserId(Long id, Long userId);
}
