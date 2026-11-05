package com.bonsaimarket.backend.community;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import com.bonsaimarket.backend.report.ReportService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/admin/community")
public class CommunityModerationController {
    private final CommunityModeration moderation;private final ReportService reports;
    public CommunityModerationController(CommunityModeration moderation,ReportService reports){this.moderation=moderation;this.reports=reports;}
    @GetMapping("/content") public Object list(@AuthenticationPrincipal AuthPrincipal actor,@RequestParam(defaultValue="POST") String kind,@RequestParam(defaultValue="PUBLIC") String status,@RequestParam(defaultValue="") String search,@RequestParam(defaultValue="0") int page){return moderation.list(actor.id(),kind,status,search,page);}
    @GetMapping("/content/{kind}/{id}") public Object detail(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable String kind,@PathVariable long id){return moderation.detail(actor.id(),kind,id);}
    @PatchMapping("/content/{kind}/{id}") public Object decide(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable String kind,@PathVariable long id,@RequestBody CommunityModeration.Decision body){return moderation.decide(actor.id(),kind,id,body);}
    @GetMapping("/content/{kind}/{id}/history") public Object history(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable String kind,@PathVariable long id,@RequestParam(defaultValue="0") int page){return moderation.history(actor.id(),kind,id,page);}
    @GetMapping("/reports") public Object reports(@AuthenticationPrincipal AuthPrincipal actor,@RequestParam(defaultValue="OPEN") String status,@RequestParam(defaultValue="0") int page){return reports.community(actor.id(),status,page);}
    @GetMapping("/reports/{id}/history") public Object reportHistory(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable long id,@RequestParam(defaultValue="0") int page){return moderation.reportHistory(actor.id(),id,page);}
}
