package com.bonsaimarket.backend.media;

import com.bonsaimarket.backend.product.Product;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="product_images",uniqueConstraints=@UniqueConstraint(columnNames="file_key")) @Getter @Setter @NoArgsConstructor
public class ProductImage {
    @Id @Column(name="product_id") private Long productId;
    @OneToOne(fetch=FetchType.LAZY) @MapsId @JoinColumn(name="product_id") private Product product;
    @Column(name="file_key",nullable=false,length=50) private String fileKey;
    @Column(nullable=false,length=30) private String contentType;
    @Column(nullable=false) private long bytes;
}
