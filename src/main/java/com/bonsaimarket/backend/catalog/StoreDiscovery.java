package com.bonsaimarket.backend.catalog;

import com.bonsaimarket.backend.store.Store;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.bonsaimarket.backend.catalog.CatalogController.*;

@Service
public class StoreDiscovery {
    private final EntityManager em;
    public StoreDiscovery(EntityManager em) { this.em=em; }
    public record StorePage(List<StoreDto> content,int page,int size,long totalElements,int totalPages) {}
    @Transactional(readOnly=true)
    public StorePage search(Map<String,String> input) {
        if(!Set.of("search","categoryId","page","size").containsAll(input.keySet())) throw new IllegalArgumentException("Bộ lọc không hợp lệ.");
        var q=CatalogSearch.Parameters.parse(input);
        String from=" from Store s join s.owner o where s.status=com.bonsaimarket.backend.store.StoreStatus.ACTIVE and o.isActive=true and o.role in (com.bonsaimarket.backend.user.UserRole.SELLER,com.bonsaimarket.backend.user.UserRole.ADMIN)";
        var args=new LinkedHashMap<String,Object>();
        String publicProduct="p.store=s and p.status=com.bonsaimarket.backend.product.ProductStatus.ACTIVE and p.category.isActive=true";
        if(q.categoryId()!=null) {from+=" and exists(select p.id from Product p where "+publicProduct+" and p.category.id=:category)";args.put("category",q.categoryId());}
        if(!q.search().isEmpty()) {
            int n=0;
            for(String token:q.search().split(" ")) {
                String key="word"+n++;
                from+=" and (lower(s.name) like :"+key+" escape '!' or lower(s.description) like :"+key+" escape '!' or lower(s.address) like :"+key+" escape '!' or exists(select p.id from Product p where "+publicProduct+" and lower(p.category.name) like :"+key+" escape '!'))";
                args.put(key,"%"+token.toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%");
            }
        }
        var count=em.createQuery("select count(s)"+from,Long.class);
        var rows=em.createQuery("select s"+from+" order by s.id asc",Store.class);
        args.forEach((k,v)->{count.setParameter(k,v);rows.setParameter(k,v);});
        long total=count.getSingleResult();
        return new StorePage(rows.setFirstResult(q.page()*q.size()).setMaxResults(q.size()).getResultStream().map(StoreDto::from).toList(),q.page(),q.size(),total,(int)((total+q.size()-1)/q.size()));
    }
}
