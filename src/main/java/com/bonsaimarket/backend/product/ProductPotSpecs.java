package com.bonsaimarket.backend.product;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

/** Loaded only for detail/content or an explicit catalog filter, never by listing mapping. */
@Entity @Table(name="product_pot_specs") @Getter @Setter @NoArgsConstructor
public class ProductPotSpecs {
    @Id private Long productId;
    @Column(length=160) private String material;
    @Column(length=20) private String shape;
    @Column(length=100) private String color;
    @Column(precision=6,scale=1) private BigDecimal diameterCm;
    @Column(precision=6,scale=1) private BigDecimal lengthCm;
    @Column(precision=6,scale=1) private BigDecimal widthCm;
    @Column(precision=6,scale=1) private BigDecimal heightCm;
    @Column(length=20) private String drainageStatus;
    private Integer drainageHoleCount;
    private Integer weightGrams;
    private Boolean hasTray;
    @Column(length=1000) private String suitablePlants;
    @Column(length=2000) private String usageInstructions;
}
