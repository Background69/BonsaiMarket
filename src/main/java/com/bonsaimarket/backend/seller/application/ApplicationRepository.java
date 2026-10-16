package com.bonsaimarket.backend.seller.application;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<SellerApplication,Long> {
    Optional<SellerApplication> findByApplicantId(Long id);
    Page<SellerApplication> findByStatus(String status,Pageable page);
}
