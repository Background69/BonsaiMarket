package com.bonsaimarket.backend.cart;

import com.bonsaimarket.backend.marketplace.*;
import com.bonsaimarket.backend.product.*;
import com.bonsaimarket.backend.user.User;
import com.bonsaimarket.backend.store.StoreStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service @Transactional(isolation=Isolation.READ_COMMITTED)
public class CartService {
    private final EntityManager em;
    private final MarketplaceAccess access;
    public CartService(EntityManager em,MarketplaceAccess access){this.em=em;this.access=access;}
    public record Line(Long id,Long productId,String name,String imageUrl,int quantity,BigDecimal unitPrice,
        Long storeId,String storeName,boolean available,int stock) {}
    public record View(List<Line> items,int quantity) {}
    public User buyer(Long actor){
        var u=access.actor(actor);
        if(u.getEmailVerifiedAt()==null)throw new MarketplaceException(403,"EMAIL_VERIFICATION_REQUIRED","Xác thực email trước khi mua hàng.");
        return u;
    }
    public Cart locked(Long actor){
        buyer(actor);access.lock(User.class,actor); // shared with addresses and checkout idempotency
        var c=em.createQuery("select c from Cart c where c.user.id=:id",Cart.class).setParameter("id",actor).getResultStream().findFirst().orElse(null);
        if(c==null){c=new Cart();c.setUser(em.getReference(User.class,actor));c.setCreatedAt(access.now());c.setUpdatedAt(access.now());em.persist(c);em.flush();}
        return c;
    }
    public List<CartItem> items(Cart c){return em.createQuery("select i from CartItem i join fetch i.product p join fetch p.store where i.cart.id=:id order by p.id",CartItem.class).setParameter("id",c.getId()).setMaxResults(101).getResultList();}
    @Transactional(readOnly=true) public View view(Long actor){
        buyer(actor);var rows=em.createQuery("select c from Cart c where c.user.id=:id",Cart.class).setParameter("id",actor).getResultList();
        if(rows.isEmpty())return new View(List.of(),0);
        var lines=items(rows.getFirst()).stream().map(i->{var p=i.getProduct();return new Line(i.getId(),p.getId(),p.getName(),p.getImageUrl(),i.getQuantity(),p.getPrice(),p.getStore().getId(),p.getStore().getName(),available(p),p.getStock()==null?0:p.getStock());}).toList();
        return new View(lines,lines.stream().mapToInt(Line::quantity).sum());
    }
    public View add(Long actor,Long productId,int quantity){
        var c=locked(actor);var p=access.lock(Product.class,productId);validate(p,quantity);
        var rows=items(c);if(rows.size()>=100&&rows.stream().noneMatch(i->i.getProduct().getId().equals(productId)))throw MarketplaceException.invalid("Giỏ hàng tối đa 100 sản phẩm.");
        var line=rows.stream().filter(i->i.getProduct().getId().equals(productId)).findFirst().orElse(null);
        // IDENTITY inserts immediately on MySQL: populate the valid quantity before persist.
        if(line==null){line=new CartItem();line.setCart(c);line.setProduct(p);line.setQuantity(quantity);em.persist(line);}
        else{int total=Math.addExact(line.getQuantity(),quantity);validate(p,total);line.setQuantity(total);}
        c.setUpdatedAt(access.now());em.flush();return view(actor);
    }
    public View update(Long actor,Long id,int quantity){
        var c=locked(actor);var line=items(c).stream().filter(i->i.getId().equals(id)).findFirst().orElseThrow(MarketplaceException::missing);
        if(quantity==0)em.remove(line);else{var p=access.lock(Product.class,line.getProduct().getId());validate(p,quantity);line.setQuantity(quantity);}
        c.setUpdatedAt(access.now());em.flush();return view(actor);
    }
    public static boolean available(Product p){return p.getStatus()==ProductStatus.ACTIVE&&p.getStore().getStatus()==StoreStatus.ACTIVE&&Boolean.TRUE.equals(p.getCategory().getIsActive())&&Boolean.TRUE.equals(p.getStore().getOwner().getIsActive());}
    public static void validate(Product p,int quantity){
        if(!available(p))throw new MarketplaceException(409,"PRODUCT_UNAVAILABLE","Sản phẩm hoặc gian hàng không còn khả dụng.");
        if(quantity<1||quantity>1_000_000)throw MarketplaceException.invalid("Số lượng không hợp lệ.");
        if(p.getStock()==null||quantity>p.getStock())throw new MarketplaceException(409,"OUT_OF_STOCK","Sản phẩm không đủ tồn kho.");
    }
}
