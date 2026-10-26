package com.bonsaimarket.backend.shipping;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity @Table(name="shipping_quotes") @Getter @Setter @NoArgsConstructor
public class ShippingQuote {
    @Id @Column(length=36) private String id;
    private Long orderId;
    private Long buyerId;
    private int shopId;
    private long parcelRevision;
    private int serviceType;
    @Column(length=120) private String serviceName;
    @Column(precision=15,scale=2) private BigDecimal fee;
    @Column(length=64) private String orderFingerprint;
    @Column(columnDefinition="text") private String destinationSnapshot;
    @Column(columnDefinition="text") private String pickupSnapshot;
    @Column(columnDefinition="text") private String parcelSnapshot;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime selectedAt;
}
