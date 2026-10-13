package com.bonsaimarket.backend.auth;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="oauth_identities",uniqueConstraints={@UniqueConstraint(columnNames={"provider","provider_subject"}),@UniqueConstraint(columnNames={"user_id","provider"})}) @Getter @Setter @NoArgsConstructor
public class OAuthIdentity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id",nullable=false) private Long userId;
    @Column(nullable=false) private String provider;
    @Column(name="provider_subject",nullable=false) private String providerSubject;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
}
