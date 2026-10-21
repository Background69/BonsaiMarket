package com.bonsaimarket.backend.product;

import com.bonsaimarket.backend.marketplace.*;
import com.bonsaimarket.backend.store.StoreStatus;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@Transactional(isolation=Isolation.READ_COMMITTED)
public class ProductContentService {
    private final EntityManager em;
    private final MarketplaceAccess access;
    public ProductContentService(EntityManager em,MarketplaceAccess access) {this.em=em;this.access=access;}
    public record Write(@NotNull @Size(max=16000) String description,
                        @NotNull @Valid ProductSpecifications specifications,@NotNull @Min(0) Long revision) { }
    public record View(Long productId,String name,String description,ProductSpecifications specifications,long revision) { }
    private Product owned(Long actor,Long id) {
        access.seller(actor);
        var p=em.find(Product.class,id);
        // Even ADMIN uses ownership here. This is the seller content integration, not an admin moderation API.
        if(p==null || p.getStore()==null || !p.getStore().getOwner().getId().equals(actor)) throw MarketplaceException.missing();
        return p;
    }
    @Transactional(readOnly=true) public View get(Long actor,Long id) {return view(owned(actor,id));}
    public View update(Long actor,Long id,Write body) {
        var row=access.lock(Product.class,owned(actor,id).getId());
        if(!row.getStore().getOwner().getId().equals(actor)) throw MarketplaceException.missing();
        if(row.getStore().getStatus()!=StoreStatus.ACTIVE) throw new MarketplaceException(409,"STORE_INACTIVE","Gian hàng chưa hoạt động.");
        MarketplaceAccess.revision(row.getContentRevision(),body.revision());
        String json=body.specifications().merge(row.getTechnicalSpecsJson());
        row.setDescription(body.description());row.setTechnicalSpecsJson(json);
        row.setContentRevision(row.getContentRevision()+1);row.setUpdatedAt(access.now());em.flush();
        return view(row);
    }
    private View view(Product p) {return new View(p.getId(),p.getName(),p.getDescription(),ProductSpecifications.read(p.getTechnicalSpecsJson()),p.getContentRevision());}
}
