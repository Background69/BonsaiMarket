package com.bonsaimarket.backend.shipping;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity @Table(name="shipments",uniqueConstraints={@UniqueConstraint(columnNames="order_id"),@UniqueConstraint(columnNames="client_reference"),@UniqueConstraint(columnNames="tracking_code")}) @Getter @Setter @NoArgsConstructor
public class Shipment {
    @Id @Column(length=36) private String id;
    private Long orderId;
    @Column(length=36) private String quoteId;
    private int shopId;
    @Column(length=50) private String clientReference;
    @Column(length=50) private String trackingCode;
    @Enumerated(EnumType.STRING) @Column(length=30) private ShipmentState state;
    private LocalDateTime lastEventAt;
    private LocalDateTime lastSyncAt;
    private LocalDateTime syncAfter;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
