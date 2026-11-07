package com.bonsaimarket.backend.media;

import com.bonsaimarket.backend.marketplace.*;
import com.bonsaimarket.backend.product.*;
import com.bonsaimarket.backend.store.StoreStatus;
import com.bonsaimarket.backend.user.UserRole;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

/** Reuses the existing media policy: Admin may manage images, Seller must own the product. */
@Component
public class ProductImageAccess {
    private final EntityManager em;private final MarketplaceAccess access;
    public ProductImageAccess(EntityManager em,MarketplaceAccess access){this.em=em;this.access=access;}
    public Product owned(Long actor,Long id,boolean lock){
        var user=access.seller(actor);var product=lock?access.lock(Product.class,id):em.find(Product.class,id);
        if(product==null||user.getRole()!=UserRole.ADMIN&&!product.getStore().getOwner().getId().equals(actor))throw MarketplaceException.missing();
        if(lock&&product.getStore().getStatus()!=StoreStatus.ACTIVE)throw new MarketplaceException(409,"STORE_INACTIVE","Gian hàng chưa hoạt động.");
        return product;
    }
    public Product readable(Long id,Long actor){
        var product=em.find(Product.class,id);if(product==null)throw MarketplaceException.missing();
        if(product.getStatus()==ProductStatus.ACTIVE&&product.getStore().getStatus()==StoreStatus.ACTIVE&&Boolean.TRUE.equals(product.getCategory().getIsActive()))return product;
        if(actor==null)throw MarketplaceException.missing();var user=access.actor(actor);
        if(user.getRole()!=UserRole.ADMIN&&!(user.getRole()==UserRole.SELLER&&product.getStore().getOwner().getId().equals(actor)))throw MarketplaceException.missing();
        return product;
    }
}
