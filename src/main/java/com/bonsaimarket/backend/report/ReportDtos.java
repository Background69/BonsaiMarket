package com.bonsaimarket.backend.report;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import static com.bonsaimarket.backend.marketplace.MarketplaceValidation.PHONE;

public final class ReportDtos {
    private ReportDtos() { }
    public record ReportHistoryView(Long id,String action,String reason,LocalDateTime createdAt) { }
    public record ReportWrite(@NotBlank @Pattern(regexp="PRODUCT|STORE|USER|COMMUNITY_POST|COMMUNITY_COMMENT") String targetType,@NotNull @Positive Long targetId,
                              @NotBlank @Pattern(regexp="MISLEADING|PROHIBITED|FRAUD|OTHER|SPAM|HARASSMENT") String reason,
                              @NotNull @Size(max=2000) String description,
                              @Size(max=20) String contextType,@Positive Long contextId,@Size(max=36) String requestKey) {
        public ReportWrite(String targetType,Long targetId,String reason,String description){this(targetType,targetId,reason,description,null,null,null);}
    }
    public record CustomerContext(Long customerId,String customerName,Long orderId,String orderCode,boolean eligible) { }
    public record ReportDecision(@NotBlank @Pattern(regexp="IN_REVIEW|RESOLVED|DISMISSED") String status,
                                 @NotBlank @Size(max=1000) String reason,@NotNull @Min(0) Long revision) { }
    public record ReportReceipt(Long id,String status) { }
    public record ReportView(Long id,String targetType,Long targetId,String reason,String description,String status,
                              String resolution,LocalDateTime createdAt,LocalDateTime processedAt,long revision,
                              Long reporterId,String reporterName,CustomerContext customerContext) { }
}
