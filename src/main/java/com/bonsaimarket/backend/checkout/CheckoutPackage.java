package com.bonsaimarket.backend.checkout;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** Mẫu kiện do người bán đo phải khớp toàn bộ sản phẩm; không suy đoán kích thước cây. */
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
