package com.bonsaimarket.backend.media;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity @Table(name="product_gallery_images") @Getter @Setter @NoArgsConstructor
public class ProductGalleryImage {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long productId;
    @Column(length=50) private String storageKey;
    @Column(length=50) private String thumbnailKey;
    @Column(nullable=false,length=500) private String imageUrl;
    @Column(nullable=false) private int sortOrder;
    @Column(nullable=false) private boolean isPrimary;
    @Column(length=30) private String contentType;
    private Long fileSizeBytes;
    private Integer width;
    private Integer height;
    @Column(length=64) private String checksum;
    @Column(nullable=false) private LocalDateTime createdAt;
    @Column(nullable=false) private LocalDateTime updatedAt;
}
