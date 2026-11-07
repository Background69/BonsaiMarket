package com.bonsaimarket.backend.media;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity @Table(name="product_gallery_uploads",uniqueConstraints=@UniqueConstraint(columnNames={"product_id","request_key"})) @Getter @Setter @NoArgsConstructor
public class ProductGalleryUpload {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long productId;
    @Column(nullable=false,length=36) private String requestKey;
    @Column(nullable=false,length=64) private String checksum;
    private Long imageId;
    @Column(nullable=false,length=20) private String status;
    @Column(nullable=false) private LocalDateTime createdAt;
}
