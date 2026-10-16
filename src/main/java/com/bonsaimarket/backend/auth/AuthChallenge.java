package com.bonsaimarket.backend.auth;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="auth_challenges",uniqueConstraints=@UniqueConstraint(columnNames={"user_id","purpose"})) @Getter @Setter @NoArgsConstructor
public class AuthChallenge {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id",nullable=false) private Long userId;
    @Column(nullable=false) private String purpose;
    @Column(nullable=false) private String nonce;
    @Column(name="code_mac",nullable=false) private String codeMac;
    @Column(name="expires_at",nullable=false) private LocalDateTime expiresAt;
    @Column(name="sent_at") private LocalDateTime sentAt;
    @Column(nullable=false) private int attempts;
    @Column(name="consumed_at") private LocalDateTime consumedAt;
}
