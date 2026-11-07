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
                        @NotNull @Valid ProductSpecifications specifications,@NotNull @Min(0) Long revision,
                        @Valid PotSpecifications potSpecs,@Pattern(regexp="PLAIN|MARKDOWN") String descriptionFormat) {
        public Write(String description,ProductSpecifications specifications,Long revision){this(description,specifications,revision,null,null);}
    }
    public record View(Long productId,String name,String description,ProductSpecifications specifications,long revision,
                       PotSpecifications potSpecs,boolean potCategory,String descriptionFormat) { }
    private Product owned(Long actor,Long id) {
        access.seller(actor);
        var p=em.find(Product.class,id);
        // Kể cả ADMIN cũng phải sở hữu gian hàng; đây là API sửa nội dung của người bán.
        if(p==null || p.getStore()==null || !p.getStore().getOwner().getId().equals(actor)) throw MarketplaceException.missing();
        return p;
    }
    @Transactional(readOnly=true) public View get(Long actor,Long id) {return view(owned(actor,id));}
    public View update(Long actor,Long id,Write body) {
        var row=access.lock(Product.class,owned(actor,id).getId());
        if(!row.getStore().getOwner().getId().equals(actor)) throw MarketplaceException.missing();
        if(row.getStore().getStatus()!=StoreStatus.ACTIVE) throw new MarketplaceException(409,"STORE_INACTIVE","Gian hàng chưa hoạt động.");
        MarketplaceAccess.revision(row.getContentRevision(),body.revision());
        if("MARKDOWN".equals(body.descriptionFormat()==null?row.getDescriptionFormat():body.descriptionFormat())){
            if(java.util.regex.Pattern.compile("<\\s*/?\\s*[a-zA-Z][^>]*>").matcher(body.description()).find())throw MarketplaceException.invalid("Mô tả định dạng không nhận HTML. Dùng văn bản và thanh công cụ hỗ trợ.");
            if(java.util.regex.Pattern.compile("\\[(?:Mô tả thiết kế, chất liệu và điểm nổi bật|Đặc điểm thực tế [123]|Hướng dẫn vệ sinh, sử dụng và bảo quản|Thông tin đóng gói thực tế)\\]").matcher(body.description()).find())
                throw MarketplaceException.invalid("Thay nội dung hướng dẫn trong mẫu mô tả bằng thông tin thực tế trước khi lưu.");
        }
        var pot=em.find(ProductPotSpecs.class,id);
        if(body.potSpecs()!=null){
            // Existing products may have partial specs. Requiring material applies only to a newly declared pot record.
            if(pot==null&&(body.potSpecs().material()==null||body.potSpecs().material().isBlank()))throw MarketplaceException.invalid("Cung cấp chất liệu khi khai báo thông số chậu lần đầu.");
            if(pot==null){pot=new ProductPotSpecs();pot.setProductId(id);em.persist(pot);}
            body.potSpecs().apply(pot);
        }else if(pot!=null){
            var canonical=PotSpecifications.from(pot);
            if(!java.util.Objects.equals(body.specifications().potMaterial(),canonical.material())||
                (body.specifications().potDiameterCm()==null?canonical.diameterCm()!=null:canonical.diameterCm()==null||body.specifications().potDiameterCm().compareTo(canonical.diameterCm())!=0))
                throw new MarketplaceException(409,"POT_SPECIFICATIONS_REQUIRED","Thông số chậu đã chuyển sang cấu trúc mới. Tải lại trình sửa nội dung để tránh ghi đè thông số.");
        }
        // Structured data is authoritative. Legacy API fields are derived mirrors for older clients.
        String json=body.specifications().withPot(PotSpecifications.from(pot)).merge(row.getTechnicalSpecsJson());
        row.setDescription(body.description());row.setTechnicalSpecsJson(json);
        if(body.descriptionFormat()!=null)row.setDescriptionFormat(body.descriptionFormat());
        row.setContentRevision(row.getContentRevision()+1);row.setUpdatedAt(access.now());em.flush();
        return view(row);
    }
    private View view(Product p) {
        var pot=PotSpecifications.from(em.find(ProductPotSpecs.class,p.getId()));
        return new View(p.getId(),p.getName(),p.getDescription(),ProductSpecifications.read(p.getTechnicalSpecsJson()).withPot(pot),p.getContentRevision(),pot,
            p.getCategory()!=null&&"POT".equals(p.getCategory().getSpecificationType()),p.getDescriptionFormat());
    }
}
