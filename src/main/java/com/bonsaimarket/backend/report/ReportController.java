package com.bonsaimarket.backend.report;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import com.bonsaimarket.backend.marketplace.ManagementPage;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import static com.bonsaimarket.backend.report.ReportDtos.*;

@RestController @RequestMapping("/api")
public class ReportController {
    private final ReportService reports;
    public ReportController(ReportService reports) { this.reports=reports; }
    @GetMapping("/seller/orders/{id}/customer-report-context") public CustomerContext customerContext(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable Long id){return reports.customerContext(actor.id(),id);}
    @GetMapping("/admin/reports/{id}/history") public java.util.List<ReportHistoryView> history(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable Long id) {return reports.history(actor.id(),id);}
    @PostMapping("/account/reports") @ResponseStatus(HttpStatus.CREATED) public ReportReceipt report(@AuthenticationPrincipal AuthPrincipal actor,@Valid @RequestBody ReportWrite body) {return reports.create(actor.id(),body);}
    @GetMapping("/admin/reports") public ManagementPage<ReportView> reports(@AuthenticationPrincipal AuthPrincipal actor,@RequestParam(defaultValue="OPEN") String status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="") String targetType,@RequestParam(required=false) Long targetId) {return reports.filtered(actor.id(),status,page,targetType,targetId);}
    @GetMapping("/admin/reports/{id}") public ReportView detail(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable Long id){return reports.detail(actor.id(),id);}
    @PatchMapping("/admin/reports/{id}") public ReportView report(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable Long id,@Valid @RequestBody ReportDecision body) {return reports.decide(actor.id(),id,body);}
}
