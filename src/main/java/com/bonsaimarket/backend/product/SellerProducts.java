package com.bonsaimarket.backend.product;

import com.bonsaimarket.backend.category.Category;
import com.bonsaimarket.backend.marketplace.*;
import com.bonsaimarket.backend.store.*;
import com.bonsaimarket.backend.user.User;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Identity/price/lifecycle only. Content, inventory and gallery retain their existing authorities. */
@Service @Transactional(isolation=Isolation.READ_COMMITTED)
public class SellerProducts {
    private final EntityManager em;private final MarketplaceAccess access;
    public SellerProducts(EntityManager em,MarketplaceAccess access){this.em=em;this.access=access;}
    public record Create(@NotBlank @Size(max=180) String name,@NotNull @DecimalMin("1") @DecimalMax("9999999999999.99") @Digits(integer=13,fraction=2) BigDecimal price,
                         @NotNull @Positive Long categoryId,@NotNull @Min(0) @Max(1000000) @tools.jackson.databind.annotation.JsonDeserialize(using=com.bonsaimarket.backend.cart.CartQuantityDeserializer.class) Integer stock,@NotNull UUID requestKey) {}
    public record Edit(@NotBlank @Size(max=180) String name,@NotNull @DecimalMin("1") @DecimalMax("9999999999999.99") @Digits(integer=13,fraction=2) BigDecimal price,
                       @NotNull @Positive Long categoryId,@NotNull @Min(0) @tools.jackson.databind.annotation.JsonDeserialize(using=com.bonsaimarket.backend.media.GalleryRevisionDeserializer.class) Long revision) {}
    public record Revision(@NotNull @Min(0) Long revision) {}
    public record View(Long id,String name,BigDecimal price,Long categoryId,int stock,String status,long revision) {}
    private Product owned(long actor,long id,boolean lock){access.seller(actor);var p=em.find(Product.class,id);if(p==null||!p.getStore().getOwner().getId().equals(actor))throw MarketplaceException.missing();if(lock)p=access.lock(Product.class,id);if(p.getStore().getStatus()!=StoreStatus.ACTIVE)throw new MarketplaceException(409,"STORE_INACTIVE","Gian hàng chưa hoạt động.");return p;}
    private Category category(long id){var c=em.find(Category.class,id);if(c==null||!Boolean.TRUE.equals(c.getIsActive()))throw MarketplaceException.invalid("Chọn danh mục đang hoạt động.");return c;}
    private View view(Product p){return new View(p.getId(),p.getName(),p.getPrice(),p.getCategory().getId(),p.getStock(),p.getStatus().name(),p.getContentRevision());}
    @Transactional(readOnly=true) public View get(long actor,long id){return view(owned(actor,id,false));}
    public View create(long actor,Create body){
        access.seller(actor);access.lock(User.class,actor); // Serializes the natural idempotency key for this seller.
        String slug="seller-"+actor+"-"+body.requestKey();
        String fingerprint=body.name().strip()+"|"+body.price().stripTrailingZeros().toPlainString()+"|"+body.categoryId()+"|"+body.stock();
        String digest;
        try{digest=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(fingerprint.getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
        var old=em.createQuery("select p from Product p where p.slug=:slug",Product.class).setParameter("slug",slug).getResultStream().findFirst();
        if(old.isPresent()){if(!Objects.equals(old.get().getMetadataJson(),"{\"sellerCreateHash\":\""+digest+"\"}"))throw new MarketplaceException(409,"REQUEST_CONFLICT","Mã tạo sản phẩm đã dùng cho dữ liệu khác.");return view(old.get());}
        var store=em.createQuery("select s from Store s where s.owner.id=:id",Store.class).setParameter("id",actor).getResultStream().findFirst().orElseThrow(MarketplaceException::missing);
        if(store.getStatus()!=StoreStatus.ACTIVE)throw new MarketplaceException(409,"STORE_INACTIVE","Gian hàng chưa hoạt động.");
        var p=new Product();p.setStore(store);p.setCategory(category(body.categoryId()));p.setName(body.name().strip());p.setPrice(body.price());p.setStock(body.stock());p.setSlug(slug);p.setDescription("");p.setStatus(ProductStatus.DRAFT);p.setTechnicalSpecsJson("{}");p.setMetadataJson("{\"sellerCreateHash\":\""+digest+"\"}");p.setCreatedAt(access.now());p.setUpdatedAt(access.now());em.persist(p);em.flush();
        MarketplaceAudit.record(em,access,actor,"PRODUCT_CREATE","PRODUCT",p.getId(),"Tạo bản nháp sản phẩm");return view(p);
    }
    public View edit(long actor,long id,Edit body){
        var p=owned(actor,id,true);MarketplaceAccess.revision(p.getContentRevision(),body.revision());
        if(p.getStatus()==ProductStatus.ARCHIVED)throw new MarketplaceException(409,"PRODUCT_ARCHIVED","Sản phẩm đã lưu trữ.");
        if(!p.getCategory().getId().equals(body.categoryId())&&!Set.of(ProductStatus.DRAFT,ProductStatus.REJECTED).contains(p.getStatus()))throw new MarketplaceException(409,"CATEGORY_CHANGE_RESTRICTED","Chỉ đổi danh mục ở bản nháp hoặc sản phẩm bị từ chối.");
        if(!p.getName().equals(body.name().strip())||p.getPrice().compareTo(body.price())!=0||!p.getCategory().getId().equals(body.categoryId())){
            p.setName(body.name().strip());p.setPrice(body.price());p.setCategory(category(body.categoryId()));
            // Published material changes require a new submission; rejected content cannot self-restore.
            if(p.getStatus()==ProductStatus.ACTIVE||p.getStatus()==ProductStatus.PENDING)p.setStatus(ProductStatus.DRAFT);
            p.setContentRevision(p.getContentRevision()+1);p.setUpdatedAt(access.now());MarketplaceAudit.record(em,access,actor,"PRODUCT_EDIT","PRODUCT",id,"Sửa tên, giá hoặc danh mục; đơn đã đặt giữ snapshot cũ");
        }
        em.flush();return view(p);
    }
    public View submit(long actor,long id,long revision){
        var p=owned(actor,id,true);MarketplaceAccess.revision(p.getContentRevision(),revision);
        if(!Set.of(ProductStatus.DRAFT,ProductStatus.REJECTED).contains(p.getStatus()))throw new MarketplaceException(409,"PRODUCT_STATE_INVALID","Chỉ gửi duyệt bản nháp hoặc sản phẩm bị từ chối.");
        ProductPublication.requireReady(p);
        category(p.getCategory().getId());p.setStatus(ProductStatus.PENDING);p.setContentRevision(p.getContentRevision()+1);p.setUpdatedAt(access.now());MarketplaceAudit.record(em,access,actor,"PRODUCT_SUBMIT","PRODUCT",id,"Người bán gửi sản phẩm để quản trị duyệt");em.flush();return view(p);
    }
}
