package com.bonsaimarket.backend.catalog;

import com.bonsaimarket.backend.product.*;
import com.bonsaimarket.backend.store.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.bonsaimarket.backend.catalog.CatalogController.*;

@Service
public class RelatedStoresService {
    private final EntityManager em;
    private final ProductRepository products;
    public RelatedStoresService(EntityManager em,ProductRepository products) {this.em=em;this.products=products;}
    public record RelatedStore(StoreDto store,long matchingProductCount,List<ProductDto> products) { }
    public record Result(CategoryDto category,List<RelatedStore> stores) { }

    @Transactional(readOnly=true)
    public Result find(Long productId) {
        var current=products.findByIdAndStatus(productId,ProductStatus.ACTIVE)
            .filter(p->p.getStore()!=null && p.getStore().getStatus()==StoreStatus.ACTIVE
                && p.getCategory()!=null && Boolean.TRUE.equals(p.getCategory().getIsActive()))
            .orElseThrow(()->new CatalogNotFoundException("Product not found"));
        Long category=current.getCategory().getId();
        // Tổng hợp trong DB; bên gọi không được tăng giới hạn cố định.
        var matches=em.createQuery("""
            select s.id,count(p.id) from Product p join p.store s join s.owner o join p.category c
            where p.status=com.bonsaimarket.backend.product.ProductStatus.ACTIVE
            and s.status=com.bonsaimarket.backend.store.StoreStatus.ACTIVE and c.isActive=true
            and o.isActive=true and o.role in (com.bonsaimarket.backend.user.UserRole.SELLER,com.bonsaimarket.backend.user.UserRole.ADMIN)
            and c.id=:category and s.id<>:current
            group by s.id order by s.id asc
            """,Object[].class).setParameter("category",category).setParameter("current",current.getStore().getId()).setMaxResults(6).getResultList();
        if(matches.isEmpty()) return new Result(CategoryDto.from(current.getCategory()),List.of());
        var ids=matches.stream().map(r->((Number)r[0]).longValue()).toList();
        // Window function trên MySQL 8.4/H2 lấy tối đa ba sản phẩm phù hợp mỗi gian hàng.
        var productIds=em.createNativeQuery("""
            select id from (
                select p.id,p.store_id,row_number() over(partition by p.store_id order by p.id asc) as rn
                from products p where p.category_id=:category and p.status='ACTIVE' and p.store_id in (:stores)
            ) ranked where rn<=3 order by store_id,id
            """,Long.class).setParameter("category",category).setParameter("stores",ids).getResultList();
        List<Product> preview=em.createQuery("select p from Product p join fetch p.store join fetch p.category where p.id in :ids order by p.id",Product.class)
            .setParameter("ids",productIds).getResultList();
        var shops=em.createQuery("select s from Store s where s.id in :ids",Store.class).setParameter("ids",ids).getResultList();
        var result=new ArrayList<RelatedStore>();
        for(var match:matches) {
            Long id=((Number)match[0]).longValue();
            var shop=shops.stream().filter(s->s.getId().equals(id)).findFirst().orElseThrow();
            result.add(new RelatedStore(StoreDto.from(shop),((Number)match[1]).longValue(),
                preview.stream().filter(p->p.getStore().getId().equals(id)).map(ProductDto::from).toList()));
        }
        return new Result(CategoryDto.from(current.getCategory()),result);
    }
}
