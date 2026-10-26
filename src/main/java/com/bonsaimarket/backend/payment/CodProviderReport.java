package com.bonsaimarket.backend.payment;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import lombok.*;
@Entity @Table(name="cod_provider_reports") @Getter @Setter @NoArgsConstructor
public class CodProviderReport {
    @Id private String id;
    private Long orderId;
    private String shipmentId;
    private String dedupKey;
    private BigDecimal reportedAmount;
    private BigDecimal reportedFee;
    private LocalDateTime transferAt;
    private LocalDateTime reportedAt;
    private LocalDateTime receivedAt;
    private String comparison;
}
