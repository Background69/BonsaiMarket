package com.bonsaimarket.backend.media;

import com.bonsaimarket.backend.marketplace.MarketplaceException;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Compatibility facade. All image writes now use the gallery transaction and shared storage. */
@Service @Transactional(isolation=Isolation.READ_COMMITTED,timeout=60)
public class ProductImageService {
    private final ProductGalleryService gallery;private final ProductImageAccess guard;private final ProductImageStorage storage;private final EntityManager em;
    public ProductImageService(ProductGalleryService gallery,ProductImageAccess guard,ProductImageStorage storage,EntityManager em){this.gallery=gallery;this.guard=guard;this.storage=storage;this.em=em;}
    public record ImageView(Long productId,String imageUrl,long maxBytes){}
    public record ImageBytes(byte[] bytes,String contentType){}
    @Transactional(readOnly=true) public ImageView current(Long actor,Long id){var result=gallery.seller(actor,id);return new ImageView(id,result.imageUrl(),result.maxBytes());}
    public ImageView upload(Long actor,Long id,MultipartFile file){var result=gallery.upload(actor,id,file,UUID.randomUUID().toString(),true).gallery();return new ImageView(id,result.imageUrl(),result.maxBytes());}
    public ImageView delete(Long actor,Long id){var result=gallery.removeLegacyCover(actor,id);return new ImageView(id,result.imageUrl(),result.maxBytes());}
    @Transactional(readOnly=true) public String redirect(Long id,Long actor,String version){
        guard.readable(id,actor);if(version!=null)return null;
        var view=gallery.publicGallery(id,actor);var primary=view.images().stream().filter(ProductGalleryDtos.Image::primary).findFirst().orElse(null);
        if(primary==null)return null;
        if(primary.id()!=null){var row=em.find(ProductGalleryImage.class,primary.id());if(row.getStorageKey()!=null)return null;}
        String url=primary.imageUrl();
        if(url==null||url.startsWith("/api/products/"+id+"/image"))return null;
        try{var uri=java.net.URI.create(url);return url.startsWith("/")&&!url.startsWith("//")||"https".equalsIgnoreCase(uri.getScheme())||"http".equalsIgnoreCase(uri.getScheme())?url:null;}catch(IllegalArgumentException e){return null;}
    }
    @Transactional(readOnly=true) public ImageBytes read(Long id,Long actor){return read(id,actor,null);}
    @Transactional(readOnly=true) public ImageBytes read(Long id,Long actor,String version){
        var product=guard.readable(id,actor);var old=em.find(ProductImage.class,id);
        if(version!=null){
            if(!version.matches("[0-9a-f-]{36}")||old==null||!old.getFileKey().equals(version+".png"))throw MarketplaceException.missing();
            return new ImageBytes(storage.read(old.getFileKey()),old.getContentType());
        }
        var primary=gallery.publicGallery(id,actor).images().stream().filter(ProductGalleryDtos.Image::primary).findFirst().orElse(null);
        if(primary!=null&&primary.id()!=null)return gallery.read(id,primary.id(),false,actor);
        if(!product.isGalleryInitialized()&&old!=null)return new ImageBytes(storage.read(old.getFileKey()),old.getContentType());
        throw MarketplaceException.missing();
    }
}
