package com.bonsaimarket.backend.category;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import com.bonsaimarket.backend.marketplace.ManagementPage;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import static com.bonsaimarket.backend.category.CategoryDtos.*;

@RestController @RequestMapping("/api")
public class CategoryManagementController {
    private final CategoryManagementService categories;
    public CategoryManagementController(CategoryManagementService categories) { this.categories=categories; }
    @GetMapping("/admin/categories") public ManagementPage<CategoryView> categories(@AuthenticationPrincipal AuthPrincipal actor,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="") String search) {return categories.list(actor.id(),page,search);}
    @PostMapping("/admin/categories") @ResponseStatus(HttpStatus.CREATED) public CategoryView create(@AuthenticationPrincipal AuthPrincipal actor,@Valid @RequestBody CategoryWrite body) {return categories.create(actor.id(),body);}
    @PutMapping("/admin/categories/{id}") public CategoryView update(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable Long id,@Valid @RequestBody CategoryWrite body) {return categories.update(actor.id(),id,body);}
    @DeleteMapping("/admin/categories/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable Long id,@RequestParam long revision) {categories.delete(actor.id(),id,revision);}
}
