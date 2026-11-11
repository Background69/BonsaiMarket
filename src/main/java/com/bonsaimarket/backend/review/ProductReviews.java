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
import java.util.ArrayList;
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
    public record Write(Long orderItemId, java.math.BigDecimal rating, String comment) {}
    public record Entry(long id, String authorName, int rating, String comment, LocalDateTime createdAt, boolean verifiedPurchase) {}
    public record Purchase(long orderItemId, String orderCode, LocalDateTime purchasedAt) {}
    public record Eligibility(List<Purchase> purchases, String message) {}
    public record RatingBucket(int stars, long count, long verifiedCount) {}
    public record Listing(ContentPage<Entry> reviews, long ratingCount, Double averageRating,
                          long verifiedRatingCount, Double verifiedAverageRating, long unverifiedRatingCount,
                          List<RatingBucket> ratingDistribution) {}
    private static final String PURCHASE_JOINS = " from reviews r left join order_items i on i.id=r.order_item_id"
        + " left join orders o on o.id=i.order_id left join products p on p.id=i.product_id";
    private static final String VERIFIED = "i.product_id=r.product_id and o.buyer_user_id=r.user_id"
        + " and o.status='COMPLETED' and p.store_id=o.store_id";
    private static final String UNREVIEWED_PURCHASE = "o.buyer_user_id=? and o.status='COMPLETED' and p.store_id=o.store_id"
        + " and not exists (select 1 from reviews r where r.order_item_id=i.id)";
    private void publicProduct(long id) {
        products.findByIdAndStatus(id,ProductStatus.ACTIVE)
            .filter(p->p.getStore().getStatus()==StoreStatus.ACTIVE && Boolean.TRUE.equals(p.getCategory().getIsActive())
                && Boolean.TRUE.equals(p.getStore().getOwner().getIsActive())).orElseThrow(MarketplaceException::missing);
    }
    @Transactional(readOnly=true)
    public Listing list(long productId, int page) {
        publicProduct(productId); MarketplaceAccess.page(page);
        var grouped = db.query("select r.rating,count(*) total,sum(case when " + VERIFIED + " then 1 else 0 end) verified"
            + PURCHASE_JOINS + " where r.product_id=? group by r.rating",
            (r,n) -> new RatingBucket(r.getInt("rating"), r.getLong("total"), r.getLong("verified")), productId);
        long total = 0, verifiedTotal = 0;
        double score = 0, verifiedScore = 0;
        List<RatingBucket> distribution = new ArrayList<>();
        for (int stars = 5; stars >= 1; stars--) {
            int value = stars;
            var bucket = grouped.stream().filter(b -> b.stars() == value).findFirst().orElse(new RatingBucket(stars, 0, 0));
            distribution.add(bucket);
            total += bucket.count(); verifiedTotal += bucket.verifiedCount();
            score += stars * (double) bucket.count(); verifiedScore += stars * (double) bucket.verifiedCount();
        }
        var rows=db.query("select r.*,u.name author_name,case when " + VERIFIED + " then true else false end verified_purchase"
            + PURCHASE_JOINS + " join users u on u.id=r.user_id where r.product_id=? order by r.created_at desc,r.id desc limit 10 offset ?",
            (r,n)->new Entry(r.getLong("id"),r.getString("author_name"),r.getInt("rating"),r.getString("comment"),r.getObject("created_at",LocalDateTime.class),r.getBoolean("verified_purchase")),productId,page*10);
        return new Listing(ContentPage.of(rows,page,total),total,total == 0 ? null : score / total,
            verifiedTotal,verifiedTotal == 0 ? null : verifiedScore / verifiedTotal,total - verifiedTotal,distribution);
    }
    @Transactional(readOnly=true)
    public Eligibility eligibility(long productId, Long actor) {
        access.actor(actor); publicProduct(productId);
        var rows=db.query("select i.id,o.code,o.created_at from order_items i join orders o on o.id=i.order_id join products p on p.id=i.product_id where i.product_id=? and " + UNREVIEWED_PURCHASE + " order by o.created_at desc,i.id desc",
            (r,n)->new Purchase(r.getLong(1),r.getString(2),r.getObject(3,LocalDateTime.class)),productId,actor);
        return new Eligibility(rows,rows.isEmpty()?"Bạn có thể đánh giá sau khi đơn hoàn thành. Mỗi lượt mua chỉ được đánh giá một lần.":"Chọn lượt mua đã hoàn thành để đánh giá.");
    }
    @Transactional(readOnly=true)
    public long countEligiblePurchases(Long actor) {
        access.actor(actor);
        return db.queryForObject("select count(*) from order_items i join orders o on o.id=i.order_id"
            + " join products p on p.id=i.product_id join stores s on s.id=p.store_id"
            + " join categories c on c.id=p.category_id join users owner on owner.id=s.owner_user_id"
            + " where " + UNREVIEWED_PURCHASE
            + " and p.status='ACTIVE' and s.status='ACTIVE' and c.is_active=true and owner.is_active=true",Long.class,actor);
    }
    public void create(long productId, Long actor, Write input) {
        MarketplaceTransactions.requireReadCommitted(em);
        access.actor(actor); publicProduct(productId);
        if(input.orderItemId()==null || input.rating()==null)
            throw MarketplaceException.invalid("Chọn lượt mua và số sao từ 1 đến 5.");
        int rating;
        try { rating = input.rating().intValueExact(); }
        catch (ArithmeticException invalid) { throw MarketplaceException.invalid("Số sao phải là số nguyên từ 1 đến 5."); }
        if (rating < 1 || rating > 5) throw MarketplaceException.invalid("Chọn số sao từ 1 đến 5.");
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
            productId,actor,line.getId(),rating,comment,access.now(),access.now());
    }
}
