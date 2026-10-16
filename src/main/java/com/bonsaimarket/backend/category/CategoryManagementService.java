package com.bonsaimarket.backend.category;

import com.bonsaimarket.backend.marketplace.MarketplaceAccess;
import com.bonsaimarket.backend.marketplace.MarketplaceAudit;
import com.bonsaimarket.backend.marketplace.ManagementPage;

import com.bonsaimarket.backend.category.*;
import com.bonsaimarket.backend.marketplace.MarketplaceException;
import jakarta.persistence.*;
import java.text.Normalizer;
import java.util.Locale;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import static com.bonsaimarket.backend.category.CategoryDtos.*;

@Service @Transactional(isolation=Isolation.READ_COMMITTED)
public class CategoryManagementService {
    private final EntityManager em; private final CategoryRepository categories; private final MarketplaceAccess access;
    public CategoryManagementService(EntityManager em,CategoryRepository categories,MarketplaceAccess access) {this.em=em;this.categories=categories;this.access=access;}
    @Transactional(readOnly=true)
    public ManagementPage<CategoryView> list(Long actor,int page,String search) {
        access.admin(actor);
        if(search.length()>150) throw MarketplaceException.invalid("Từ khóa tối đa 150 ký tự.");
        return ManagementPage.of(categories.findByNameContainingIgnoreCase(search,PageRequest.of(MarketplaceAccess.page(page),20,Sort.by("sortOrder","id"))).map(this::view));
    }
    public CategoryView create(Long actor,CategoryWrite body) {
        access.admin(actor);var row=new Category();row.setSortOrder(0);row.setCreatedAt(access.now());apply(row,body);
        categories.saveAndFlush(row);MarketplaceAudit.record(em,access,actor,"CATEGORY_CREATED","CATEGORY",row.getId(),"");return view(row);
    }
    public CategoryView update(Long actor,Long id,CategoryWrite body) {
        access.admin(actor);var row=access.lock(Category.class,id);MarketplaceAccess.revision(row.getRevision(),body.revision());
        // Slug remains stable for existing public filter URLs.
        row.setName(body.name().strip());row.setDescription(MarketplaceAccess.text(body.description()));row.setIsActive(body.active());row.setUpdatedAt(access.now());
        ensureName(row);row.setRevision(row.getRevision()+1);em.flush();MarketplaceAudit.record(em,access,actor,"CATEGORY_UPDATED","CATEGORY",id,"");return view(row);
    }
    public void delete(Long actor,Long id,long revision) {
        access.admin(actor);var row=access.lock(Category.class,id);MarketplaceAccess.revision(row.getRevision(),revision);
        long products=em.createQuery("select count(p) from Product p where p.category.id=:id",Long.class).setParameter("id",id).getSingleResult();
        long children=em.createQuery("select count(c) from Category c where c.parent.id=:id",Long.class).setParameter("id",id).getSingleResult();
        if(products>0 || children>0) throw new MarketplaceException(409,"CATEGORY_IN_USE","Danh mục đang được tham chiếu. Bạn có thể ẩn thay vì xóa.");
        em.remove(row);em.flush();MarketplaceAudit.record(em,access,actor,"CATEGORY_DELETED","CATEGORY",id,"");
    }
    private void apply(Category row,CategoryWrite b) {
        row.setName(b.name().strip());row.setDescription(MarketplaceAccess.text(b.description()));row.setIsActive(b.active());row.setUpdatedAt(access.now());
        String slug=Normalizer.normalize(row.getName().toLowerCase(Locale.ROOT).replace('đ','d'),Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[^a-z0-9]+","-").replaceAll("^-|-$","");
        if(slug.isEmpty()) throw MarketplaceException.invalid("Tên danh mục cần có chữ hoặc số tạo đường dẫn.");
        row.setSlug(slug);ensureName(row);
    }
    private void ensureName(Category row) {
        // Database unique name key is normalized identically on create/edit; a concurrent loser gets 409.
        row.setNameKey(row.getName().toLowerCase(Locale.ROOT));
        long duplicates=em.createQuery("select count(c) from Category c where lower(trim(c.name))=:name and (:id is null or c.id<>:id)",Long.class)
            .setParameter("name",row.getNameKey()).setParameter("id",row.getId()).getSingleResult();
        if(duplicates>0) throw new MarketplaceException(409,"CATEGORY_EXISTS","Tên danh mục đã được sử dụng.");
    }
    private CategoryView view(Category c) {return new CategoryView(c.getId(),c.getName(),c.getSlug(),c.getDescription(),Boolean.TRUE.equals(c.getIsActive()),c.getRevision());}
}
