package com.bonsaimarket.backend.inventory;

import com.bonsaimarket.backend.order.Order;
import com.bonsaimarket.backend.product.Product;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity @Table(name="inventory_debits", uniqueConstraints=@UniqueConstraint(columnNames={"order_id","product_id"}))
@Getter @Setter @NoArgsConstructor
public class InventoryDebit {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="order_id",nullable=false) private Order order;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="product_id",nullable=false) private Product product;
    @Column(nullable=false) private Integer quantity;
    @Column(nullable=false) private boolean restored;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
}
