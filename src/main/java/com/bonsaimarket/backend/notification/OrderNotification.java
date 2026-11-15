package com.bonsaimarket.backend.notification;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
@Entity @Table(name="order_notification_outbox") @Getter @Setter @NoArgsConstructor
public class OrderNotification {
    @Column(nullable=false,length=10) private String channel="EMAIL";
    @Column(name="recipient_user_id") private Long recipientUserId;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="recipient_user_id",insertable=false,updatable=false)
    @org.hibernate.annotations.OnDelete(action=org.hibernate.annotations.OnDeleteAction.CASCADE)
    private com.bonsaimarket.backend.user.User recipientUser;
    private LocalDateTime readAt;
    @Column(length=500) private String targetLink;
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
