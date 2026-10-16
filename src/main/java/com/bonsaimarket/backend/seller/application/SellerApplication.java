package com.bonsaimarket.backend.seller.application;

import com.bonsaimarket.backend.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity @Table(name="seller_applications",uniqueConstraints=@UniqueConstraint(columnNames="applicant_id"))
@Getter @Setter @NoArgsConstructor
public class SellerApplication {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="applicant_id",nullable=false) private User applicant;
    @Column(nullable=false,length=20) private String status;
    @Column(nullable=false,length=200) private String name;
    @Column(nullable=false,length=4000) private String description;
    @Column(nullable=false,length=30) private String phone;
    @Column(nullable=false,length=500) private String address;
    @Column(nullable=false,length=1000) private String reason="";
    private Long processedBy;
    @Column(nullable=false) private LocalDateTime submittedAt;
    private LocalDateTime processedAt;
    @Version private long revision;
}
