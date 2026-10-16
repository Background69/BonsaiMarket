package com.bonsaimarket.backend.report;

import com.bonsaimarket.backend.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity @Table(name="marketplace_reports",uniqueConstraints=@UniqueConstraint(columnNames={"reporter_id","target_type","target_id"}))
@Getter @Setter @NoArgsConstructor
public class MarketplaceReport {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="reporter_id",nullable=false) private User reporter;
    @Column(name="target_type",nullable=false,length=20) private String targetType;
    @Column(name="target_id",nullable=false) private Long targetId;
    @Column(nullable=false,length=20) private String reason;
    @Column(nullable=false,length=2000) private String description;
    @Column(nullable=false,length=20) private String status;
    @Column(nullable=false,length=1000) private String resolution="";
    private Long processedBy;
    @Column(nullable=false) private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    @Version private long revision;
}
