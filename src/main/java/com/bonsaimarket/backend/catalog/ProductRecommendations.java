package com.bonsaimarket.backend.catalog;

import com.bonsaimarket.backend.product.*;
import com.bonsaimarket.backend.store.StoreStatus;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.bonsaimarket.backend.catalog.CatalogController.*;

@Service
public class ProductRecommendations {
    private final EntityManager em;
    private final ProductRepository products;
    public ProductRecommendations(EntityManager em,ProductRepository products){this.em=em;this.products=products;}
    @Transactional(readOnly=true)
    public List<ProductDto> find(Long currentId,List<Long> exclude) {
        Product current=currentId==null?null:products.findByIdAndStatus(currentId,ProductStatus.ACTIVE)
            .filter(p->p.getStore().getStatus()==StoreStatus.ACTIVE && Boolean.TRUE.equals(p.getCategory().getIsActive()))
            .orElseThrow(()->new CatalogNotFoundException("Product not found"));
        if(exclude.size()>8 || exclude.stream().anyMatch(id->id==null || id<=0)) throw new IllegalArgumentException("Tối đa 8 mã sản phẩm hợp lệ.");
        // One row per store before global ranking prevents a large store from occupying the list.
        String distance=current==null?"":"abs(p.price-:price),";
        String sql="select id from (select p.id,p.created_at,"+(current==null?"":"abs(p.price-:price) as distance,")+
            "row_number() over(partition by p.store_id order by "+distance+"p.created_at desc,p.id desc) as rn "+
            "from products p join stores s on s.id=p.store_id join users u on u.id=s.owner_user_id join categories c on c.id=p.category_id "+
            "where p.status='ACTIVE' and p.stock>0 and s.status='ACTIVE' and c.is_active=true and u.is_active=true and u.role in ('SELLER','ADMIN') "+
            (current==null?"":"and p.category_id=:category and p.store_id<>:store and p.id<>:current ")+
            (exclude.isEmpty()?"":"and p.id not in (:exclude) ")+
            ") ranked where rn=1 order by "+(current==null?"":"distance,")+"created_at desc,id desc";
        var query=em.createNativeQuery(sql,Long.class).setMaxResults(8);
        if(current!=null) query.setParameter("price",current.getPrice()).setParameter("category",current.getCategory().getId()).setParameter("store",current.getStore().getId()).setParameter("current",currentId);
        if(!exclude.isEmpty())query.setParameter("exclude",exclude);
        List<Long> ids=query.getResultList();
        if(ids.isEmpty())return List.of();
        var found=em.createQuery("select p from Product p join fetch p.store join fetch p.category where p.id in :ids",Product.class).setParameter("ids",ids).getResultList();
        return ids.stream().map(id->found.stream().filter(p->p.getId().equals(id)).findFirst().orElseThrow()).map(ProductDto::from).toList();
    }
}
