package com.bonsaimarket.backend.checkout;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity @Table(name="checkout_requests") @Getter @Setter @NoArgsConstructor
public class CheckoutRequest {
    @Id private String id;
    private Long buyerId;
    private String requestKey;
    private String requestHash;
    private String quoteId;
    private Long orderId;
    private LocalDateTime createdAt;
}
