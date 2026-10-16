package com.bonsaimarket.backend.marketplace;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity @Table(name="marketplace_audit") @Getter @Setter @NoArgsConstructor
public class MarketplaceAudit {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long actorId;
    @Column(nullable=false,length=40) private String action;
    @Column(nullable=false,length=30) private String targetType;
    @Column(nullable=false) private Long targetId;
    @Column(nullable=false,length=1000) private String reason;
    @Column(nullable=false) private LocalDateTime createdAt;
    public static void record(EntityManager em,MarketplaceAccess access,Long actor,String action,String target,Long id,String reason) {
        var row=new MarketplaceAudit();row.actorId=actor;row.action=action;row.targetType=target;row.targetId=id;
        row.reason=MarketplaceAccess.text(reason);row.createdAt=access.now();em.persist(row);
    }
}
