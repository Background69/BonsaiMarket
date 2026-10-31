package com.bonsaimarket.backend.payment;
import com.bonsaimarket.backend.auth.AuthPrincipal;
import com.bonsaimarket.backend.marketplace.*;
import com.bonsaimarket.backend.order.Order;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
@RestController @Transactional(readOnly=true)
public class CodReportController {
    private final CodReports reports;
    public CodReportController(CodReports reports){this.reports=reports;}
    @GetMapping("/api/seller/orders/{id}/cod-reports") public Object seller(@AuthenticationPrincipal AuthPrincipal a,@PathVariable Long id){return reports.view(a.id(),id,false);}
    @GetMapping("/api/admin/orders/{id}/cod-reports") public Object admin(@AuthenticationPrincipal AuthPrincipal a,@PathVariable Long id){return reports.view(a.id(),id,true);}
}
