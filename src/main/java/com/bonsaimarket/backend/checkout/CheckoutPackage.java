package com.bonsaimarket.backend.checkout;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** Seller-measured complete packed contents, matched exactly; no inferred plant dimensions. */
@Entity @Table(name="checkout_packages") @Getter @Setter @NoArgsConstructor
public class CheckoutPackage {
    @Id private String id;
    private Long storeId;
    @Column(columnDefinition="text") private String contents;
    @Column(columnDefinition="text") private String parcel;
    private boolean enabled;
    private long revision;
    private LocalDateTime updatedAt;
}
