package com.bonsaimarket.backend.notification;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
@Entity @Table(name="order_notification_outbox") @Getter @Setter @NoArgsConstructor
public class OrderNotification {
    @Id private String id;
    private Long orderId;
    private String eventType;
    private String recipient;
    private String subject;
    @Column(columnDefinition="text") private String body;
    private String dedupKey;
    private String status;
    private int attempts;
    private LocalDateTime nextAttemptAt;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
    private String errorCode;
}
