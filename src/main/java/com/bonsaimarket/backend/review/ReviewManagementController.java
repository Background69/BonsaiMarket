package com.bonsaimarket.backend.review;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ReviewManagementController {
    private final ReviewManagement reviews;
    public ReviewManagementController(ReviewManagement reviews) { this.reviews=reviews; }
    @GetMapping("/seller/reviews")
    public Object seller(@AuthenticationPrincipal AuthPrincipal actor, @RequestParam(defaultValue="ALL") String sentiment,
            @RequestParam(defaultValue="0") int page) { return reviews.list(actor.id(),false,sentiment,page); }
    @GetMapping("/admin/reviews")
    public Object admin(@AuthenticationPrincipal AuthPrincipal actor, @RequestParam(defaultValue="ALL") String sentiment,
            @RequestParam(defaultValue="0") int page) { return reviews.list(actor.id(),true,sentiment,page); }
}
