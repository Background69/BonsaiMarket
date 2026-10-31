package com.bonsaimarket.backend.review;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api")
public class ProductReviewController {
    private final ProductReviews reviews;
    public ProductReviewController(ProductReviews reviews) {this.reviews=reviews;}
    @GetMapping("/products/{id}/reviews") public Object list(@PathVariable long id,@RequestParam(defaultValue="0") int page) {return reviews.list(id,page);}
    @GetMapping("/account/products/{id}/review-eligibility") public Object eligibility(@PathVariable long id,@AuthenticationPrincipal AuthPrincipal actor) {return reviews.eligibility(id,actor.id());}
    @PostMapping("/account/products/{id}/reviews") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public void create(@PathVariable long id,@AuthenticationPrincipal AuthPrincipal actor,@RequestBody ProductReviews.Write body) {reviews.create(id,actor.id(),body);}
}
