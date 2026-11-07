package com.bonsaimarket.backend.media;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import static com.bonsaimarket.backend.media.ProductGalleryDtos.*;

@RestController @RequestMapping("/api")
public class ProductGalleryController {
    private final ProductGalleryService service;
    public ProductGalleryController(ProductGalleryService service){this.service=service;}
    @GetMapping("/seller/products/{id}/gallery") public ResponseEntity<View> seller(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable Long id){return response(service.seller(actor.id(),id));}
    @PostMapping("/seller/products/{id}/gallery/initialize") public ResponseEntity<View> initialize(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable Long id){return response(service.initialize(actor.id(),id));}
    @PostMapping(value="/seller/products/{id}/gallery",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Upload> upload(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable Long id,@RequestPart("file") MultipartFile file,@RequestParam java.util.UUID requestKey,org.springframework.web.multipart.MultipartHttpServletRequest request){
        if(request.getMultiFileMap().values().stream().mapToInt(java.util.List::size).sum()!=1)throw com.bonsaimarket.backend.marketplace.MarketplaceException.invalid("Mỗi request chứa đúng một ảnh; giao diện điều phối toàn bộ nhóm đã chọn.");
        return response(service.upload(actor.id(),id,file,requestKey.toString()));
    }
    @PutMapping("/seller/products/{id}/gallery/order") public ResponseEntity<View> order(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable Long id,@Valid @RequestBody Reorder body){return response(service.reorder(actor.id(),id,body));}
    @PutMapping("/seller/products/{id}/gallery/{imageId}/primary") public ResponseEntity<View> primary(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable Long id,@PathVariable Long imageId,@Valid @RequestBody Revision body){return response(service.setPrimary(actor.id(),id,imageId,body.revision()));}
    @DeleteMapping("/seller/products/{id}/gallery/{imageId}") public ResponseEntity<View> remove(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable Long id,@PathVariable Long imageId,@RequestParam long revision){if(revision<0)throw com.bonsaimarket.backend.marketplace.MarketplaceException.invalid("Revision không hợp lệ.");return response(service.remove(actor.id(),id,imageId,revision));}
    @GetMapping("/products/{id}/gallery") public ResponseEntity<View> gallery(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable Long id){return response(service.publicGallery(id,actor==null?null:actor.id()));}
    @GetMapping({"/products/{id}/gallery/{imageId}/content","/products/{id}/gallery/{imageId}/thumbnail"})
    public ResponseEntity<byte[]> content(@AuthenticationPrincipal AuthPrincipal actor,@PathVariable Long id,@PathVariable Long imageId,jakarta.servlet.http.HttpServletRequest request){
        var image=service.read(id,imageId,request.getRequestURI().endsWith("/thumbnail"),actor==null?null:actor.id());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.contentType())).cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").header("Content-Disposition","inline").body(image.bytes());
    }
    private <T> ResponseEntity<T> response(T body){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);}
}
