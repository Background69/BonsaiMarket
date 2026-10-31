package com.bonsaimarket.backend.store;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity @Table(name="store_public_locations") @Getter @Setter @NoArgsConstructor
public class StorePublicLocation {
    @Id @Column(name="store_id") private Long storeId;
    @Column(nullable=false,length=10) private String mode;
    @Column(nullable=false) private boolean published;
    @Column(nullable=false,length=120) private String region;
    @Column(nullable=false,length=120) private String ward;
    @Column(name="public_address",nullable=false,length=500) private String publicAddress;
    @Column(precision=10,scale=7) private BigDecimal latitude;
    @Column(precision=10,scale=7) private BigDecimal longitude;
    private LocalDateTime consentAt;
    private LocalDateTime updatedAt;
    @Column(nullable=false) private long revision;
}
