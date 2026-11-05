package com.bonsaimarket.backend.media;

import com.bonsaimarket.backend.marketplace.MarketplaceAccess;

import com.bonsaimarket.backend.product.*;
import com.bonsaimarket.backend.store.StoreStatus;
import com.bonsaimarket.backend.user.UserRole;
import com.bonsaimarket.backend.marketplace.MarketplaceException;
import jakarta.persistence.*;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;

/** Một ảnh chính; server tạo tên và nội dung file, không dùng metadata do request gửi. */
@Service @Transactional(isolation=Isolation.READ_COMMITTED)
public class ProductImageService {
    private final EntityManager em;private final MarketplaceAccess access;private final Path root;private final long maxBytes;
    public ProductImageService(EntityManager em,MarketplaceAccess access,@Value("${marketplace.media.root:.local/product-images}") String root,
                               @Value("${marketplace.media.max-bytes:5242880}") long maxBytes) {
        this.em=em;this.access=access;this.root=Path.of(root).toAbsolutePath().normalize();this.maxBytes=maxBytes;
    }
    public record ImageView(Long productId,String imageUrl,long maxBytes) { }
    public record ImageBytes(byte[] bytes,String contentType) { }
    private Product owned(Long actor,Long id) {
        var user=access.seller(actor);var p=access.lock(Product.class,id);
        if(user.getRole()!=UserRole.ADMIN && !p.getStore().getOwner().getId().equals(actor)) throw MarketplaceException.missing();
        if(p.getStore().getStatus()!=StoreStatus.ACTIVE) throw new MarketplaceException(409,"STORE_INACTIVE","Gian hàng chưa hoạt động.");
        return p;
    }
    @Transactional(readOnly=true) public ImageView current(Long actor,Long id) {
        var user=access.seller(actor);var p=em.find(Product.class,id);
        if(p==null || user.getRole()!=UserRole.ADMIN && !p.getStore().getOwner().getId().equals(actor)) throw MarketplaceException.missing();
        return new ImageView(id,p.getImageUrl(),maxBytes);
    }
    public ImageView upload(Long actor,Long id,MultipartFile file) {
        var product=owned(actor,id);
        if(file.isEmpty() || file.getSize()>maxBytes) throw new MarketplaceException(413,"IMAGE_TOO_LARGE","Ảnh phải có dữ liệu và không vượt quá giới hạn dung lượng.");
        String key=UUID.randomUUID()+".png";Path target=path(key);boolean created=false;
        try {
            var decoded=SafeImage.decode(file,maxBytes);
            Files.createDirectories(root);
            // CREATE_NEW tránh ghi đè file có sẵn; mã hóa lại loại script, EXIF và byte thừa.
            try(var out=Files.newOutputStream(target,StandardOpenOption.CREATE_NEW)) {created=true;if(!ImageIO.write(decoded,"png",out)) throw invalid();}
            if(Files.size(target)>maxBytes) {removeFile(target);throw new MarketplaceException(413,"IMAGE_TOO_LARGE","Ảnh sau xử lý vượt giới hạn. Hãy chọn ảnh nhỏ hơn.");}
            var previous=em.find(ProductImage.class,id);String oldKey=previous==null?null:previous.getFileKey();
            // Rollback xóa file vừa ghi; giữ file cũ đến khi DB commit thành công.
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if(status!=STATUS_COMMITTED) removeFile(target);
                    else if(oldKey!=null) removeFile(path(oldKey));
                }
            });
            var image=previous==null?new ProductImage():previous;
            image.setFileKey(key);image.setContentType("image/png");image.setBytes(Files.size(target));
            if(previous==null) {image.setProduct(product);image.setProductId(id);em.persist(image);}
            product.setImageUrl("/api/products/"+id+"/image?v="+key.substring(0,36));product.setUpdatedAt(access.now());em.flush();
            return new ImageView(id,product.getImageUrl(),maxBytes);
        } catch(IOException e) {if(created)removeFile(target);throw new MarketplaceException(503,"MEDIA_UNAVAILABLE","Chưa thể lưu ảnh. Vui lòng thử lại.");}
        catch(RuntimeException e) {if(created)removeFile(target);throw e;}
    }
    public ImageView delete(Long actor,Long id) {
        var p=owned(actor,id);var image=em.find(ProductImage.class,id);
        if(image!=null) {
            String key=image.getFileKey();em.remove(image);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {if(status==STATUS_COMMITTED)removeFile(path(key));}
            });
        }
        // Chỉ bỏ liên kết URL cũ; không xóa file ngoài hệ thống hoặc file dùng chung.
        p.setImageUrl(null);p.setUpdatedAt(access.now());return new ImageView(id,null,maxBytes);
    }
    @Transactional(readOnly=true) public ImageBytes read(Long id,Long actor) {
        var p=em.find(Product.class,id);
        if(p==null) throw MarketplaceException.missing();
        boolean publicImage=p.getStatus()==ProductStatus.ACTIVE && p.getStore().getStatus()==StoreStatus.ACTIVE && Boolean.TRUE.equals(p.getCategory().getIsActive());
        if(!publicImage) {
            if(actor==null) throw MarketplaceException.missing();var user=access.actor(actor);
            if(user.getRole()!=UserRole.ADMIN && !(user.getRole()==UserRole.SELLER && p.getStore().getOwner().getId().equals(actor))) throw MarketplaceException.missing();
        }
        var image=em.find(ProductImage.class,id);if(image==null) throw MarketplaceException.missing();
        try {return new ImageBytes(Files.readAllBytes(path(image.getFileKey())),image.getContentType());}
        catch(IOException e) {throw MarketplaceException.missing();}
    }
    private Path path(String key) {
        if(!key.matches("[0-9a-f-]{36}\\.png")) throw MarketplaceException.missing();
        Path result=root.resolve(key).normalize();if(!result.getParent().equals(root)) throw MarketplaceException.missing();return result;
    }
    private void removeFile(Path path) {
        try {Files.deleteIfExists(path);} catch(IOException e) {org.slf4j.LoggerFactory.getLogger(getClass()).warn("Product image cleanup deferred; run documented orphan maintenance.");}
    }
    private MarketplaceException invalid() {return MarketplaceException.invalid("Chỉ chấp nhận ảnh JPEG hoặc PNG hợp lệ.");}
}
