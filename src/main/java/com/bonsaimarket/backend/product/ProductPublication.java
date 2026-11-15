package com.bonsaimarket.backend.product;

import com.bonsaimarket.backend.marketplace.MarketplaceException;
import com.bonsaimarket.backend.store.StoreStatus;
import com.bonsaimarket.backend.user.UserRole;

/** Recheck current eligibility at submission AND approval; a pending gallery can change. */
public final class ProductPublication {
    private ProductPublication() {}
    public static void requireReady(Product p) {
        var store=p.getStore();var owner=store==null?null:store.getOwner();
        if(store==null||store.getStatus()!=StoreStatus.ACTIVE||owner==null||!Boolean.TRUE.equals(owner.getIsActive())
            ||(owner.getRole()!=UserRole.SELLER&&owner.getRole()!=UserRole.ADMIN)
            ||p.getCategory()==null||!Boolean.TRUE.equals(p.getCategory().getIsActive())
            ||p.getDescription()==null||p.getDescription().isBlank()||p.getStock()==null||p.getStock()<=0
            ||p.getImageUrl()==null||p.getImageUrl().isBlank())
            throw new MarketplaceException(409,"PRODUCT_NOT_READY","Cần gian hàng, chủ sở hữu và danh mục hoạt động; lưu mô tả, ảnh và tồn kho dương trước khi gửi hoặc duyệt.");
    }
}
