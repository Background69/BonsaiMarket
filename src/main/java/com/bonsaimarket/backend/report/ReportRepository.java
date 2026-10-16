package com.bonsaimarket.backend.report;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
public interface ReportRepository extends JpaRepository<MarketplaceReport,Long> {
    Page<MarketplaceReport> findByStatus(String status,Pageable page);
    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporter,String type,Long target);
    long countByReporterIdAndCreatedAtAfter(Long reporter,java.time.LocalDateTime since);
}
