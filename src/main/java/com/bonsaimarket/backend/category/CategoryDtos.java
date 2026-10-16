package com.bonsaimarket.backend.category;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import static com.bonsaimarket.backend.marketplace.MarketplaceValidation.PHONE;

public final class CategoryDtos {
    private CategoryDtos() { }
    public record CategoryWrite(@NotBlank @Size(max=150) String name,@Size(max=4000) String description,
                                @NotNull Boolean active,@NotNull @Min(0) Long revision) { }
    public record CategoryView(Long id,String name,String slug,String description,boolean active,long revision) { }
}
