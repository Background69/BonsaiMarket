package com.bonsaimarket.backend.store;

import com.bonsaimarket.backend.marketplace.*;
import com.bonsaimarket.backend.user.UserRole;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service @Transactional(isolation=Isolation.READ_COMMITTED)
public class StoreLocations {
    private final EntityManager em;
    private final MarketplaceAccess access;
    public StoreLocations(EntityManager em, MarketplaceAccess access) {this.em=em;this.access=access;}
    public record Write(@NotNull @Pattern(regexp="AREA|EXACT") String mode, boolean published, boolean consent,
        @NotBlank @Size(max=120) String region, @NotBlank @Size(max=120) String ward,
        @NotNull @Size(max=500) String publicAddress, BigDecimal latitude, BigDecimal longitude, @Min(0) long revision) {}
    public record View(Long storeId,String mode,boolean published,String region,String ward,String publicAddress,
        BigDecimal latitude,BigDecimal longitude,LocalDateTime consentAt,LocalDateTime updatedAt,long revision) {}
    public record PublicView(Long storeId,String name,String description,String logoUrl,String mode,String region,String ward,
        String publicAddress,BigDecimal latitude,BigDecimal longitude) {}
    public record Page(List<PublicView> content,int page,int size,boolean hasNext) {}
    public record Settings(boolean eligible,View location) {}

    // A legacy store/role is not proof of approval. Never reuse SellerApplicationService.current's synthesized status.
    public boolean eligible(Store s) {
        var o=s.getOwner();
        return s.getStatus()==StoreStatus.ACTIVE && Boolean.TRUE.equals(o.getIsActive()) && o.getEmailVerifiedAt()!=null
            && (o.getRole()==UserRole.SELLER || o.getRole()==UserRole.ADMIN)
            && em.createQuery("select count(a) from SellerApplication a where a.applicant.id=:owner and a.status='APPROVED' and a.processedAt is not null and a.processedBy is not null and a.processedBy<>:owner",Long.class)
                .setParameter("owner",o.getId()).getSingleResult()>0;
    }
    private Store owned(Long actor) {
        access.seller(actor);
        return em.createQuery("select s from Store s where s.owner.id=:id",Store.class).setParameter("id",actor)
            .getResultStream().findFirst().orElseThrow(MarketplaceException::missing);
    }
    @Transactional(readOnly=true) public Settings settings(Long actor) {
        var s=owned(actor);
        var row=em.find(StorePublicLocation.class,s.getId());
        return new Settings(eligible(s),row==null?null:view(row));
    }
    public View save(Long actor,Write b) {
        var s=access.lock(Store.class,owned(actor).getId());
        var existing=em.find(StorePublicLocation.class,s.getId());
        // Withdrawal stays available after approval is revoked.
        if((b.published() || existing==null) && !eligible(s)) throw new MarketplaceException(403,"SELLER_APPROVAL_REQUIRED","Cần hồ sơ người bán đã được duyệt và gian hàng đang hoạt động để công khai vị trí.");
        if(b.published() && !b.consent()) throw MarketplaceException.invalid("Cần đồng ý công khai vị trí đã chọn.");
        if(b.mode().equals("AREA")) {
            if(b.latitude()!=null || b.longitude()!=null || !b.publicAddress().isBlank()) throw MarketplaceException.invalid("Khu vực hoạt động không có pin hoặc địa chỉ chi tiết.");
        } else {
            if(b.publicAddress().isBlank() || b.latitude()==null || b.longitude()==null
                || b.latitude().abs().compareTo(new BigDecimal("90"))>0 || b.longitude().abs().compareTo(new BigDecimal("180"))>0
                || b.latitude().signum()==0 && b.longitude().signum()==0 || b.latitude().scale()>7 || b.longitude().scale()>7)
                throw MarketplaceException.invalid("Nhập địa chỉ công khai và tọa độ WGS84 hợp lệ, tối đa 7 số thập phân.");
        }
        var row=existing;
        if(row==null) {MarketplaceAccess.revision(0,b.revision());row=new StorePublicLocation();row.setStoreId(s.getId());}
        else MarketplaceAccess.revision(row.getRevision(),b.revision());
        row.setMode(b.mode());row.setPublished(b.published());row.setRegion(b.region().strip());row.setWard(b.ward().strip());
        row.setPublicAddress(b.publicAddress().strip());row.setLatitude(b.latitude());row.setLongitude(b.longitude());
        var now=access.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        row.setConsentAt(b.published()?now:null);row.setUpdatedAt(now);row.setRevision(row.getRevision()+1);em.persist(row);
        em.flush();return view(row);
    }
    @Transactional(readOnly=true) public Page discover(String search,String region,Long categoryId,Long storeId,int page) {
        if(page<0||page>1000||search!=null&&search.length()>120||region!=null&&region.length()>120||categoryId!=null&&categoryId<1||storeId!=null&&storeId<1)
            throw MarketplaceException.invalid("Bộ lọc vị trí không hợp lệ.");
        String q="select l,s from StorePublicLocation l, Store s where l.storeId=s.id and l.published=true and s.status=com.bonsaimarket.backend.store.StoreStatus.ACTIVE and s.owner.isActive=true and s.owner.emailVerifiedAt is not null and s.owner.role in (com.bonsaimarket.backend.user.UserRole.SELLER,com.bonsaimarket.backend.user.UserRole.ADMIN) and exists(select a.id from SellerApplication a where a.applicant=s.owner and a.status='APPROVED' and a.processedAt is not null and a.processedBy is not null and a.processedBy<>s.owner.id)";
        Map<String,Object> args=new LinkedHashMap<>();
        if(search!=null&&!search.isBlank()){q+=" and (lower(s.name) like :q escape '!' or lower(l.region) like :q escape '!' or lower(l.ward) like :q escape '!')";args.put("q","%"+search.strip().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%");}
        if(region!=null&&!region.isBlank()){q+=" and l.region=:region";args.put("region",region.strip());}
        if(storeId!=null){q+=" and s.id=:store";args.put("store",storeId);}
        if(categoryId!=null){q+=" and exists(select p.id from Product p where p.store=s and p.category.id=:cat and p.category.isActive=true and p.status=com.bonsaimarket.backend.product.ProductStatus.ACTIVE)";args.put("cat",categoryId);}
        var query=em.createQuery(q+" order by s.id",Object[].class);args.forEach(query::setParameter);
        var rows=query.setFirstResult(page*50).setMaxResults(51).getResultList();
        return new Page(rows.stream().limit(50).map(r->{var l=(StorePublicLocation)r[0];var s=(Store)r[1];return new PublicView(s.getId(),s.getName(),s.getDescription(),s.getLogoUrl(),l.getMode(),l.getRegion(),l.getWard(),l.getPublicAddress(),l.getLatitude(),l.getLongitude());}).toList(),page,50,rows.size()>50);
    }
    private View view(StorePublicLocation l) {return new View(l.getStoreId(),l.getMode(),l.isPublished(),l.getRegion(),l.getWard(),l.getPublicAddress(),l.getLatitude(),l.getLongitude(),l.getConsentAt(),l.getUpdatedAt(),l.getRevision());}
}
