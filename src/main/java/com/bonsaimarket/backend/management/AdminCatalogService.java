package com.bonsaimarket.backend.management;

import com.bonsaimarket.backend.marketplace.*;
import com.bonsaimarket.backend.product.*;
import com.bonsaimarket.backend.store.*;
import com.bonsaimarket.backend.community.ContentAccess;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/**
 * Admin read/moderation boundary. Seller price, stock and content APIs stay authoritative.
 */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class AdminCatalogService {
    public record ProductRow(long id, String name, String imageUrl, BigDecimal price, Integer stock, String status,
                             long revision,
                             long storeId, String storeName, long ownerId, String ownerName, String category,
                             String createdAt) {
    }

    public record StoreRow(long id, String name, String description, String status, long revision, long ownerId,
                           String ownerName, long products, long orders, String createdAt) {
    }

    public record ProductDetail(ProductRow product, String description, List<AdminHistory.Entry> history,
                                List<String> actions) {
    }

    public record RelatedOrder(long id, String code, String status, BigDecimal total, String createdAt) {
    }

    public record StoreDetail(StoreRow store, Long applicationId, List<ProductRow> products,
                              List<AdminHistory.Entry> history, List<RelatedOrder> orders) {
    }

    private final EntityManager em;
    private final MarketplaceAccess access;
    private final JdbcTemplate db;
    private final AdminHistory history;

    public AdminCatalogService(EntityManager em, MarketplaceAccess access, JdbcTemplate db, AdminHistory history) {
        this.em = em;
        this.access = access;
        this.db = db;
        this.history = history;
    }

    private ProductRow productRow(Product p) {
        return new ProductRow(p.getId(), p.getName(), p.getImageUrl(), p.getPrice(), p.getStock(), p.getStatus().name(), p.getContentRevision(), p.getStore().getId(), p.getStore().getName(), p.getStore().getOwner().getId(), p.getStore().getOwner().getName(), p.getCategory().getName(), ManagementSupport.iso(p.getCreatedAt()));
    }

    private StoreRow storeRow(Store s) {
        return new StoreRow(s.getId(), s.getName(), s.getDescription(), s.getStatus().name(), s.getRevision(), s.getOwner().getId(), s.getOwner().getName(), count("select count(*) from products where store_id=?", s.getId()), count("select count(*) from orders where store_id=?", s.getId()), ManagementSupport.iso(s.getCreatedAt()));
    }

    private long count(String sql, Object... args) {
        return db.queryForObject(sql, Long.class, args);
    }

    @Transactional(readOnly = true)
    public ManagementPage<ProductRow> products(Long actor, int page, String search, String status, Long category) {
        access.admin(actor);
        MarketplaceAccess.page(page);
        String where = " where 1=1";
        var args = new LinkedHashMap<String, Object>();
        String q = ManagementSupport.like(search);
        if (q != null) {
            where += " and (lower(p.name) like :q or lower(p.store.name) like :q)";
            args.put("q", q);
        }
        if (status != null && !status.isBlank()) {
            try {
                args.put("status", ProductStatus.valueOf(status));
            } catch (IllegalArgumentException e) {
                throw MarketplaceException.invalid("Trạng thái sản phẩm không hợp lệ.");
            }
            where += " and p.status=:status";
        }
        if (category != null) {
            where += " and p.category.id=:category";
            args.put("category", category);
        }
        var total = em.createQuery("select count(p) from Product p" + where, Long.class);
        var query = em.createQuery("select p from Product p" + where + " order by p.id desc", Product.class);
        args.forEach((k, v) -> {
            total.setParameter(k, v);
            query.setParameter(k, v);
        });
        long n = total.getSingleResult();
        return new ManagementPage<>(query.setFirstResult(page * 20).setMaxResults(20).getResultStream().map(this::productRow).toList(), page, 20, n, (int) ((n + 19) / 20));
    }

    @Transactional(readOnly = true)
    public ProductDetail product(Long actor, long id) {
        access.admin(actor);
        var p = em.find(Product.class, id);
        if (p == null) throw MarketplaceException.missing();
        var actions = new ArrayList<String>();
        if (p.getStatus() == ProductStatus.PENDING) {actions.add("APPROVE");actions.add("REJECT");}
        if (p.getStatus() == ProductStatus.ACTIVE) actions.add("HIDE");
        if (p.getStatus() == ProductStatus.REJECTED && lastAction(id).equals("PRODUCT_HIDE")) actions.add("RESTORE");
        if (p.getStatus() == ProductStatus.ACTIVE || p.getStatus() == ProductStatus.REJECTED) actions.add("REMOVE");
        return new ProductDetail(productRow(p), p.getDescription(), history.target(actor, "PRODUCT", id), actions);
    }

    private String lastAction(long id) {
        return em.createQuery("select a.action from MarketplaceAudit a where a.targetType='PRODUCT' and a.targetId=:id and a.action in ('PRODUCT_HIDE','PRODUCT_RESTORE','PRODUCT_REMOVE') order by a.id desc", String.class).setParameter("id", id).setMaxResults(1).getResultStream().findFirst().orElse("");
    }

    public ProductDetail moderate(Long actor, long id, String action, long revision, String input) {
        MarketplaceTransactions.requireReadCommitted(em);
        access.admin(actor);
        String reason = ContentAccess.text(input, 1000);
        var p = access.lock(Product.class, id);
        MarketplaceAccess.revision(p.getContentRevision(), revision);
        if (!product(actor, id).actions().contains(action))
            throw new MarketplaceException(409, "MODERATION_STATE_INVALID", "Thao tác không còn phù hợp. Tải lại sản phẩm để kiểm tra.");
        String before = p.getStatus().name();
        if("APPROVE".equals(action))com.bonsaimarket.backend.product.ProductPublication.requireReady(p);
        p.setStatus(switch (action) {
              case "APPROVE" -> ProductStatus.ACTIVE;
              case "REJECT" -> ProductStatus.REJECTED;
            case "HIDE" -> ProductStatus.REJECTED;
            case "RESTORE" -> ProductStatus.ACTIVE;
            default -> ProductStatus.ARCHIVED;
        });
        p.setContentRevision(p.getContentRevision() + 1);
        p.setUpdatedAt(access.now());
        MarketplaceAudit.transition(em, access, actor, "PRODUCT_" + action, "PRODUCT", id, reason, before, p.getStatus().name());
        em.flush();
        return product(actor, id);
    }

    @Transactional(readOnly = true)
    public ManagementPage<StoreRow> stores(Long actor, int page, String search, String status) {
        access.admin(actor);
        MarketplaceAccess.page(page);
        String where = " where 1=1";
        var args = new LinkedHashMap<String, Object>();
        String q = ManagementSupport.like(search);
        if (q != null) {
            where += " and (lower(s.name) like :q or lower(s.owner.name) like :q)";
            args.put("q", q);
        }
        if (status != null && !status.isBlank()) {
            try {
                args.put("status", StoreStatus.valueOf(status));
            } catch (IllegalArgumentException e) {
                throw MarketplaceException.invalid("Trạng thái gian hàng không hợp lệ.");
            }
            where += " and s.status=:status";
        }
        var total = em.createQuery("select count(s) from Store s" + where, Long.class);
        var query = em.createQuery("select s from Store s" + where + " order by s.id desc", Store.class);
        args.forEach((k, v) -> {
            total.setParameter(k, v);
            query.setParameter(k, v);
        });
        long n = total.getSingleResult();
        return new ManagementPage<>(query.setFirstResult(page * 20).setMaxResults(20).getResultStream().map(this::storeRow).toList(), page, 20, n, (int) ((n + 19) / 20));
    }

    @Transactional(readOnly = true)
    public StoreDetail store(Long actor, long id) {
        access.admin(actor);
        var s = em.find(Store.class, id);
        if (s == null) throw MarketplaceException.missing();
        Long application = db.query("select id from seller_applications where applicant_id=?", (r, n) -> r.getLong(1), s.getOwner().getId()).stream().findFirst().orElse(null);
        var products = em.createQuery("select p from Product p where p.store.id=:id order by p.id desc", Product.class).setParameter("id", id).setMaxResults(10).getResultStream().map(this::productRow).toList();
        var orders = em.createQuery("select o.id,o.code,o.status,o.total,o.createdAt from Order o where o.store.id=:id order by o.createdAt desc,o.id desc", Object[].class).setParameter("id", id).setMaxResults(10).getResultStream().map(r -> new RelatedOrder(((Number) r[0]).longValue(), String.valueOf(r[1]), String.valueOf(r[2]), (BigDecimal) r[3], ManagementSupport.iso((LocalDateTime) r[4]))).toList();
        return new StoreDetail(storeRow(s), application, products, history.target(actor, "STORE", id), orders);
    }

    public StoreDetail lifecycle(Long actor, long id, String status, long revision, String input) {
        MarketplaceTransactions.requireReadCommitted(em);
        access.admin(actor);
        String reason = ContentAccess.text(input, 1000);
        var s = access.lock(Store.class, id);
        MarketplaceAccess.revision(s.getRevision(), revision);
        StoreStatus next;
        try {
            next = StoreStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw MarketplaceException.invalid("Trạng thái gian hàng không hợp lệ.");
        }
        if (s.getStatus() == next)
            throw new MarketplaceException(409, "STORE_STATE_UNCHANGED", "Gian hàng đã có trạng thái này.");
        // Checkout locks products before validating store availability; serialize suspension with that same guard.
        em.createQuery("select p from Product p where p.store.id=:id order by p.id", Product.class).setParameter("id", id).setLockMode(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE).getResultList();
        String before = s.getStatus().name();
        s.setStatus(next);
        s.setRevision(s.getRevision() + 1);
        s.setUpdatedAt(access.now());
        MarketplaceAudit.transition(em, access, actor, next == StoreStatus.ACTIVE ? "STORE_RESTORE" : "STORE_SUSPEND", "STORE", id, reason, before, next.name());
        em.flush();
        return store(actor, id);
    }
}
