package com.bonsaimarket.backend.shipping;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity @Table(name="shipment_events",uniqueConstraints=@UniqueConstraint(columnNames={"shipment_id","dedup_key"})) @Getter @Setter @NoArgsConstructor
public class ShipmentEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(length=36) private String shipmentId;
    @Column(length=64) private String dedupKey;
    @Column(length=40) private String carrierStatus;
    @Enumerated(EnumType.STRING) @Column(length=30) private ShipmentState state;
    private LocalDateTime eventAt;
    private LocalDateTime receivedAt;
    @Column(length=12) private String source;
}
