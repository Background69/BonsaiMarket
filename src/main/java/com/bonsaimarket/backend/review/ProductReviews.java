package com.bonsaimarket.backend.review;

import com.bonsaimarket.backend.community.ContentAccess;
import com.bonsaimarket.backend.community.ContentAccess.ContentPage;
import com.bonsaimarket.backend.marketplace.*;
import com.bonsaimarket.backend.order.*;
import com.bonsaimarket.backend.product.*;
import com.bonsaimarket.backend.store.StoreStatus;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@Transactional(isolation=Isolation.READ_COMMITTED)
public class ProductReviews {
    private final EntityManager em;
    private final JdbcTemplate db;
    private final MarketplaceAccess access;
    private final ProductRepository products;
    public ProductReviews(EntityManager em, JdbcTemplate db, MarketplaceAccess access, ProductRepository products) {
        this.em=em; this.db=db; this.access=access; this.products=products;
    }
    public record Write(Long orderItemId, Integer rating, String comment) {}
    public record Entry(long id, String authorName, int rating, String comment, LocalDateTime createdAt, boolean verifiedPurchase) {}
    public record Purchase(long orderItemId, String orderCode, LocalDateTime purchasedAt) {}
    public record Eligibility(List<Purchase> purchases, String message) {}
    public record Listing(ContentPage<Entry> reviews, long ratingCount, Double averageRating) {}
    private void publicProduct(long id) {
        products.findByIdAndStatus(id,ProductStatus.ACTIVE)
            .filter(p->p.getStore().getStatus()==StoreStatus.ACTIVE && Boolean.TRUE.equals(p.getCategory().getIsActive())
                && Boolean.TRUE.equals(p.getStore().getOwner().getIsActive())).orElseThrow(MarketplaceException::missing);
    }
    @Transactional(readOnly=true)
    public Listing list(long productId, int page) {
        publicProduct(productId); MarketplaceAccess.page(page);
        long total=db.queryForObject("select count(*) from reviews where product_id=?",Long.class,productId);
        Double average=db.queryForObject("select avg(cast(rating as double)) from reviews where product_id=?",Double.class,productId);
        var rows=db.query("select r.*,u.name author_name from reviews r join users u on u.id=r.user_id where r.product_id=? order by r.created_at desc,r.id desc limit 10 offset ?",
            (r,n)->new Entry(r.getLong("id"),r.getString("author_name"),r.getInt("rating"),r.getString("comment"),r.getObject("created_at",LocalDateTime.class),r.getObject("order_item_id")!=null),productId,page*10);
        return new Listing(ContentPage.of(rows,page,total),total,average);
    }
    @Transactional(readOnly=true)
    public Eligibility eligibility(long productId, Long actor) {
        access.actor(actor); publicProduct(productId);
        var rows=db.query("select i.id,o.code,o.created_at from order_items i join orders o on o.id=i.order_id join products p on p.id=i.product_id where i.product_id=? and o.buyer_user_id=? and o.status='COMPLETED' and p.store_id=o.store_id and not exists (select 1 from reviews r where r.order_item_id=i.id) order by o.created_at desc,i.id desc",
            (r,n)->new Purchase(r.getLong(1),r.getString(2),r.getObject(3,LocalDateTime.class)),productId,actor);
        return new Eligibility(rows,rows.isEmpty()?"Bạn có thể đánh giá sau khi đơn hoàn thành. Mỗi lượt mua chỉ được đánh giá một lần.":"Chọn lượt mua đã hoàn thành để đánh giá.");
    }
    public void create(long productId, Long actor, Write input) {
        MarketplaceTransactions.requireReadCommitted(em);
        access.actor(actor); publicProduct(productId);
        if(input.orderItemId()==null || input.rating()==null || input.rating()<1 || input.rating()>5)
            throw MarketplaceException.invalid("Chọn lượt mua và số sao từ 1 đến 5.");
        String comment=ContentAccess.text(input.comment(),2000);
        var found=em.find(OrderItem.class,input.orderItemId());
        if(found==null) throw MarketplaceException.missing();
        // Khóa trên đơn chính, dùng chung với chuyển trạng thái và hủy đơn.
        var order=access.lock(Order.class,found.getOrder().getId());
        var line=access.lock(OrderItem.class,input.orderItemId());
        if(!line.getOrder().getId().equals(order.getId()) || !actor.equals(order.getBuyer().getId()) || line.getProduct().getId()!=productId
            || !line.getProduct().getStore().getId().equals(order.getStore().getId())) throw MarketplaceException.missing();
        if(order.getStatus()!=OrderStatus.COMPLETED) throw new MarketplaceException(409,"REVIEW_NOT_ELIGIBLE","Chỉ được đánh giá khi đơn hàng đã hoàn thành.");
        if(db.queryForObject("select count(*) from reviews where order_item_id=?",Long.class,line.getId())>0)
            throw new MarketplaceException(409,"REVIEW_EXISTS","Lượt mua này đã được đánh giá. Vui lòng tải lại.");
        db.update("insert into reviews(product_id,user_id,order_item_id,rating,comment,created_at,updated_at) values(?,?,?,?,?,?,?)",
            productId,actor,line.getId(),input.rating(),comment,access.now(),access.now());
    }
}
