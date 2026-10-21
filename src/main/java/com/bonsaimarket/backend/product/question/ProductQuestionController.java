package com.bonsaimarket.backend.product.question;
import com.bonsaimarket.backend.auth.AuthPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api")
public class ProductQuestionController {
    private final ProductQuestions service;
    public ProductQuestionController(ProductQuestions service){this.service=service;}
    @GetMapping("/products/{id}/questions") public Object list(@PathVariable long id,@RequestParam(defaultValue="0") int page,@AuthenticationPrincipal AuthPrincipal actor){return service.list(id,page,actor==null?null:actor.id());}
    @PostMapping("/account/products/{id}/questions") public Object ask(@PathVariable long id,@AuthenticationPrincipal AuthPrincipal actor,@RequestBody ProductQuestions.Write body){return service.ask(id,actor.id(),body);}
    @PutMapping("/seller/product-questions/{id}/answer") public Object answer(@PathVariable long id,@AuthenticationPrincipal AuthPrincipal actor,@RequestBody ProductQuestions.Write body){return service.answer(id,actor.id(),body);}
    @DeleteMapping("/account/product-questions/{id}") public void remove(@PathVariable long id,@AuthenticationPrincipal AuthPrincipal actor,@RequestParam long revision){service.remove(id,actor.id(),revision);}
}
