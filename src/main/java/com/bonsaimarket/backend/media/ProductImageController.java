package com.bonsaimarket.backend.media;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;

@RestController @RequestMapping("/api")
public class ProductImageController {
    private final ProductImageService service;
    public ProductImageController(ProductImageService service) {this.service=service;}
    @GetMapping("/seller/products/{id}/image") public ProductImageService.ImageView current(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable Long id) {return service.current(actor.id(),id);}
    @PostMapping(value="/seller/products/{id}/image",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductImageService.ImageView upload(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable Long id,@RequestPart("file") MultipartFile file) {return service.upload(actor.id(),id,file);}
    @DeleteMapping("/seller/products/{id}/image") public ProductImageService.ImageView delete(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable Long id) {return service.delete(actor.id(),id);}
    @GetMapping("/products/{id}/image") public ResponseEntity<byte[]> image(@PathVariable Long id,@AuthenticationPrincipal AuthPrincipal actor) {
        var result=service.read(id,actor==null?null:actor.id());return ResponseEntity.ok().contentType(MediaType.parseMediaType(result.contentType()))
            .cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").header("Content-Disposition","inline").body(result.bytes());
    }
}
