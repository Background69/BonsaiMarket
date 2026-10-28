package com.bonsaimarket.backend.checkout;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Lưu lần xem lại đơn và khóa chống tạo đơn lặp, kể cả sau khi khởi động lại. */
@Entity
@Table(name = "checkout_attempts")
@Getter @Setter @NoArgsConstructor
public class CheckoutAttempt {
    @Id @Column(length = 36) private String id;
    @Column(name = "buyer_user_id", nullable = false) private Long buyerUserId;
    @Column(name = "request_json", nullable = false, columnDefinition = "text") private String requestJson;
    @Column(name = "review_json", nullable = false, columnDefinition = "text") private String reviewJson;
    @Column(name = "receipt_json", columnDefinition = "text") private String receiptJson;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;
}
