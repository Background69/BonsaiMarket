package com.bonsaimarket.backend.review;

import com.bonsaimarket.backend.community.ContentAccess.ContentPage;
import com.bonsaimarket.backend.marketplace.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewManagement {
    private final JdbcTemplate db;
    private final MarketplaceAccess access;
    public ReviewManagement(JdbcTemplate db, MarketplaceAccess access) { this.db=db; this.access=access; }
    public record Entry(long id, long productId, String productName, String storeName, int rating,
        String comment, LocalDateTime createdAt, String sentimentStatus, String sentimentLabel,
        String modelVersion, String contentVersion, Boolean demoOnly) {}

    @Transactional(readOnly=true)
    public ContentPage<Entry> list(Long actor, boolean admin, String filter, int page) {
        if (admin) access.admin(actor); else access.seller(actor);
        MarketplaceAccess.page(page);
        if (!Set.of("ALL","UNANALYZED","PENDING","PROCESSING","FAILED","positive","neutral","negative","mixed").contains(filter))
            throw MarketplaceException.invalid("Bộ lọc cảm xúc không hợp lệ.");
        // Fingerprint equality keeps stale results invisible even if content changes outside this feature.
        String from = " from reviews r join products p on p.id=r.product_id join stores st on st.id=p.store_id"
            + " left join review_sentiment s on s.review_id=r.id and s.content_version=SHA2(coalesce(r.comment,''),256)"
            + " and exists (select 1 from order_items i join orders o on o.id=i.order_id where i.id=r.order_item_id"
            + " and i.product_id=r.product_id and o.buyer_user_id=r.user_id and o.status='COMPLETED' and o.store_id=p.store_id) where 1=1";
        var args = new ArrayList<Object>();
        if (!admin) { from += " and st.owner_user_id=?"; args.add(actor); }
        if (SentimentClient.LABELS.contains(filter)) { from += " and s.status='ANALYZED' and s.label=?"; args.add(filter); }
        else if (filter.equals("UNANALYZED")) from += " and (s.review_id is null or s.status='STALE')";
        else if (!filter.equals("ALL")) { from += " and s.status=?"; args.add(filter); }
        long total = db.queryForObject("select count(*)"+from,Long.class,args.toArray());
        args.add(page*10);
        List<Entry> rows = db.query("select r.id,p.id product_id,p.name product_name,st.name store_name,r.rating,r.comment,r.created_at,"
            + "s.status,s.label,s.model_version,s.content_version,s.demo_only"+from+" order by r.created_at desc,r.id desc limit 10 offset ?",
            (r,n) -> new Entry(r.getLong("id"),r.getLong("product_id"),r.getString("product_name"),r.getString("store_name"),r.getInt("rating"),
                r.getString("comment"),r.getObject("created_at",LocalDateTime.class),
                r.getString("status")==null || "STALE".equals(r.getString("status")) ? "UNANALYZED" : r.getString("status"),
                r.getString("label"),r.getString("model_version"),r.getString("content_version"),r.getObject("demo_only",Boolean.class)),args.toArray());
        return ContentPage.of(rows,page,total);
    }
}
