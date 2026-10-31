package com.bonsaimarket.backend.returns;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
@Entity @Table(name="order_returns") @Getter @Setter @NoArgsConstructor
public class OrderReturn {
    @Id private Long orderId;
    private String kind;
    private String status;
    @Column(length=1000) private String reason;
    @Column(length=1000) private String inspection;
    private boolean restock;
    private String carrierRequestStatus;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime receivedAt;
    private LocalDateTime inspectedAt;
    private LocalDateTime updatedAt;
}
