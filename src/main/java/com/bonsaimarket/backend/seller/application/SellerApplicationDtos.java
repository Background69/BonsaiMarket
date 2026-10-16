package com.bonsaimarket.backend.seller.application;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import static com.bonsaimarket.backend.marketplace.MarketplaceValidation.PHONE;

public final class SellerApplicationDtos {
    private SellerApplicationDtos() { }
    public record ApplicationWrite(@NotBlank @Size(max=200) String name,@NotNull @Size(max=4000) String description,
                                   @NotNull @Pattern(regexp=PHONE) String phone,@NotNull @Size(max=500) String address,
                                   @NotNull @Min(0) Long revision) { }
    public record ApplicationView(Long id,String status,String name,String description,String phone,String address,
                                   String reason,String applicantName,LocalDateTime submittedAt,LocalDateTime processedAt,long revision,Long storeId) { }
    public record Decision(@NotBlank @Pattern(regexp="APPROVED|REJECTED") String status,@NotNull @Size(max=1000) String reason,
                            @NotNull @Min(0) Long revision) { }
}
