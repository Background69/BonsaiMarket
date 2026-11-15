package com.bonsaimarket.backend.marketplace;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "marketplace_audit_states")
@Getter
@Setter
@NoArgsConstructor
public class MarketplaceAuditState {
    @Id
    private Long auditId;
    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private MarketplaceAudit audit;
    @Column(length = 160)
    private String beforeState;
    @Column(length = 160)
    private String afterState;
}
