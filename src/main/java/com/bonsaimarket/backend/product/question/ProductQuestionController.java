package com.bonsaimarket.backend.product.question;
import com.bonsaimarket.backend.auth.AuthPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api")
public class ProductQuestionController {
    private final ProductQuestions service;
    public ProductQuestionController(ProductQuestions service){this.service=service;}
    @GetMapping("/products/{id}/questions") public Object list(@PathVariable long id,@RequestParam(defaultValue="0") int page,@RequestParam(required=false) Long questionId,@AuthenticationPrincipal AuthPrincipal actor){return service.list(id,page,actor==null?null:actor.id(),questionId);}
    @GetMapping("/seller/product-questions") public Object inbox(@AuthenticationPrincipal AuthPrincipal actor,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="UNANSWERED") String answer){return service.inbox(actor.id(),page,answer);}
    @PostMapping("/account/products/{id}/questions") public Object ask(@PathVariable long id,@AuthenticationPrincipal AuthPrincipal actor,@RequestBody ProductQuestions.Write body){return service.ask(id,actor.id(),body);}
    @PutMapping("/account/product-questions/{id}") public Object edit(@PathVariable long id,@AuthenticationPrincipal AuthPrincipal actor,@RequestBody ProductQuestions.Write body){return service.edit(id,actor.id(),body);}
    @PutMapping("/seller/product-questions/{id}/answer") public Object answer(@PathVariable long id,@AuthenticationPrincipal AuthPrincipal actor,@RequestBody ProductQuestions.Write body){return service.answer(id,actor.id(),body);}
    @DeleteMapping("/account/product-questions/{id}") public void remove(@PathVariable long id,@AuthenticationPrincipal AuthPrincipal actor,@RequestParam long revision){service.remove(id,actor.id(),revision);}
    @GetMapping("/products/{productId}/questions/{questionId}/replies") public Object replies(@PathVariable long productId,@PathVariable long questionId,@RequestParam(defaultValue="0") int page){return service.replies(productId,questionId,page);}
    @PostMapping("/account/products/{productId}/questions/{questionId}/replies") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public void reply(@PathVariable long productId,@PathVariable long questionId,@AuthenticationPrincipal AuthPrincipal actor,@RequestBody ProductQuestions.Write body){service.reply(productId,questionId,actor.id(),body);}
}
