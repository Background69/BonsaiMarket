package com.bonsaimarket.backend.shipping;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity @Table(name="shipping_parcels") @Getter @Setter @NoArgsConstructor
public class ShippingParcel {
    @Id @Column(name="order_id") private Long orderId;
    private int weightGrams;
    private int lengthCm;
    private int widthCm;
    private int heightCm;
    private boolean carrierAcceptedGoods;
    private long revision;
    private LocalDateTime updatedAt;
    public ShippingProvider.Parcel snapshot(){return new ShippingProvider.Parcel(weightGrams,lengthCm,widthCm,heightCm);}
}
