package com.bonsaimarket.backend.payment;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import lombok.*;

/** Minimal COD intent. No wallet, payout ledger or assumed legacy payment. */
@Entity @Table(name="order_payments") @Getter @Setter @NoArgsConstructor
public class OrderPayment {
    @Id private Long orderId;
    private String quoteId;
    private String method;
    private String status;
    private String currency;
    private BigDecimal amountDue;
    private BigDecimal codAmount;
    private BigDecimal shippingFee;
    private int feePayer;
    private String collectionStatus;
    private String settlementStatus;
    private BigDecimal collectedAmount;
    private BigDecimal settledAmount;
    @Column(length=200) private String storeNameSnapshot;
    private LocalDateTime createdAt;
}
