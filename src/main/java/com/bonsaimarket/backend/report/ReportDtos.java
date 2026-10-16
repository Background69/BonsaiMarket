package com.bonsaimarket.backend.report;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import static com.bonsaimarket.backend.marketplace.MarketplaceValidation.PHONE;

public final class ReportDtos {
    private ReportDtos() { }
    public record ReportHistoryView(Long id,String action,String reason,LocalDateTime createdAt) { }
    public record ReportWrite(@NotBlank @Pattern(regexp="PRODUCT|STORE") String targetType,@NotNull @Positive Long targetId,
                              @NotBlank @Pattern(regexp="MISLEADING|PROHIBITED|FRAUD|OTHER") String reason,
                              @NotNull @Size(max=2000) String description) { }
    public record ReportDecision(@NotBlank @Pattern(regexp="IN_REVIEW|RESOLVED|DISMISSED") String status,
                                 @NotBlank @Size(max=1000) String reason,@NotNull @Min(0) Long revision) { }
    public record ReportReceipt(Long id,String status) { }
    public record ReportView(Long id,String targetType,Long targetId,String reason,String description,String status,
                              String resolution,LocalDateTime createdAt,LocalDateTime processedAt,long revision) { }
}
