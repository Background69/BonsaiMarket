package com.bonsaimarket.backend.community;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;

@RestController @RequestMapping("/api")
public class CommunityImageController {
    private final CommunityImages images;
    public CommunityImageController(CommunityImages images){this.images=images;}
    @PostMapping("/account/community/posts/{id}/images") public Object upload(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable long id,@RequestParam long revision,@RequestParam String requestKey,@RequestPart("file") MultipartFile file){return images.upload(actor.id(),id,revision,requestKey,file);}
    @DeleteMapping("/account/community/posts/{id}/images/{image}") public Object delete(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable long id,@PathVariable String image,@RequestParam long revision){return images.delete(actor.id(),id,image,revision);}
    @GetMapping("/community/posts/{id}/images/{image}") public ResponseEntity<byte[]> read(@PathVariable long id,@PathVariable String image){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").contentType(MediaType.IMAGE_PNG).body(images.read(id,image));}
}
