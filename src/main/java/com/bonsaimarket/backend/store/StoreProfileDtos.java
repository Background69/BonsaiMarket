package com.bonsaimarket.backend.store;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import static com.bonsaimarket.backend.marketplace.MarketplaceValidation.PHONE;

public final class StoreProfileDtos {
    private StoreProfileDtos() { }
    public record StoreWrite(@NotBlank @Size(max=200) String name,@NotNull @Size(max=4000) String description,
                             @NotNull @Pattern(regexp=PHONE,message="Số điện thoại công khai không hợp lệ.") String phone,
                             @NotNull @Size(max=500) String address,@NotNull @Min(0) Long revision,
                             @Size(max=500) String logoUrl,@Size(max=500) String bannerUrl,@Size(max=12000) String aboutShop) {
        public StoreWrite(String name,String description,String phone,String address,Long revision) {this(name,description,phone,address,revision,null,null,null);}
    }
    public record StoreView(Long id,String name,String description,String phone,String address,String logoUrl,long revision,String bannerUrl,String aboutShop) { }
}
