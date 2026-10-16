package com.bonsaimarket.backend.profile;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import static com.bonsaimarket.backend.marketplace.MarketplaceValidation.PHONE;

public final class ProfileDtos {
    private ProfileDtos() { }
    public record ProfileWrite(@NotBlank @Size(max=150) String name,@NotNull @Pattern(regexp=PHONE,message="Số điện thoại không hợp lệ.") String phone,
                               @NotNull @Size(max=2000) String bio) { }
    public record ProfileView(String name,String email,String phone,String bio,boolean emailVerified) { }
}
