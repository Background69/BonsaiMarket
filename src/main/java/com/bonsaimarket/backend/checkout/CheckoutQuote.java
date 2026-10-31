package com.bonsaimarket.backend.checkout;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import lombok.*;

@Entity @Table(name="checkout_quotes") @Getter @Setter @NoArgsConstructor
public class CheckoutQuote {
    @Id private String id;
    private Long buyerId;
    private Long storeId;
    private String packageId;
    private long packageRevision;
    private String cartFingerprint;
    @Column(columnDefinition="text") private String addressInput;
    @Column(columnDefinition="text") private String destinationSnapshot;
    @Column(columnDefinition="text") private String pickupSnapshot;
    @Column(columnDefinition="text") private String parcelSnapshot;
    private int shopId;
    private int serviceType;
    private String serviceName;
    private BigDecimal fee;
    private BigDecimal subtotal;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
