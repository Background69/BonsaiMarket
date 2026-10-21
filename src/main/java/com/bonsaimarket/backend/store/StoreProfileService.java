package com.bonsaimarket.backend.store;

import com.bonsaimarket.backend.marketplace.MarketplaceAccess;

import com.bonsaimarket.backend.store.*;
import com.bonsaimarket.backend.user.UserRole;
import com.bonsaimarket.backend.marketplace.MarketplaceException;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import static com.bonsaimarket.backend.store.StoreProfileDtos.*;

@Service @Transactional(isolation=Isolation.READ_COMMITTED)
public class StoreProfileService {
    private final EntityManager em; private final MarketplaceAccess access;
    public StoreProfileService(EntityManager em,MarketplaceAccess access) {this.em=em;this.access=access;}
    Store owned(Long actor) {
        access.seller(actor);
        return em.createQuery("select s from Store s where s.owner.id=:id",Store.class).setParameter("id",actor).getResultStream().findFirst().orElseThrow(MarketplaceException::missing);
    }
    @Transactional(readOnly=true) public StoreView get(Long actor) {return view(owned(actor));}
    public StoreView update(Long actor,StoreWrite body) {
        var row=access.lock(Store.class,owned(actor).getId());MarketplaceAccess.revision(row.getRevision(),body.revision());
        if(row.getStatus()!=StoreStatus.ACTIVE) throw new MarketplaceException(409,"STORE_INACTIVE","Gian hàng chưa hoạt động. Liên hệ quản trị viên.");
        // Absent fields from older clients preserve stored content; an empty string explicitly clears a field.
        if(body.logoUrl()!=null) row.setLogoUrl(StoreImageUrls.validate(body.logoUrl()));
        if(body.bannerUrl()!=null) row.setBannerUrl(StoreImageUrls.validate(body.bannerUrl()));
        if(body.aboutShop()!=null) row.setAboutShop(body.aboutShop());
        row.setName(body.name().strip());row.setDescription(body.description().strip());row.setPhone(body.phone().strip());row.setAddress(body.address().strip());row.setUpdatedAt(access.now());row.setRevision(row.getRevision()+1);em.flush();return view(row);
    }
    StoreView view(Store s) {return new StoreView(s.getId(),s.getName(),s.getDescription(),s.getPhone(),s.getAddress(),s.getLogoUrl(),s.getRevision(),s.getBannerUrl(),s.getAboutShop());}
}
