package com.bonsaimarket.backend.management;

import com.bonsaimarket.backend.marketplace.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only payment records, never an inference of collection or settlement.
 */
@Service
@Transactional(readOnly = true)
public class AdminPaymentReadService {
    public record PaymentRow(String id, long orderId, String code, String buyer, String store, BigDecimal amount,
                             String method, String status, String collection, String settlement, String createdAt) {
    }

    private static final String SOURCE = "(select concat('attempt-',t.id) record_id,t.order_id,t.amount,t.payment_method method,concat('',t.status) status,null collection,null settlement,t.created_at from transactions t union all select concat('order-',p.order_id),p.order_id,p.amount_due,p.method,p.status,p.collection_status,p.settlement_status,p.created_at from order_payments p) x join orders o on o.id=x.order_id join users u on u.id=o.buyer_user_id join stores s on s.id=o.store_id";
    private final MarketplaceAccess access;
    private final JdbcTemplate db;

    public AdminPaymentReadService(MarketplaceAccess access, JdbcTemplate db) {
        this.access = access;
        this.db = db;
    }

    public record Summary(long total, long cod, long online, long review) {
    }

    public Summary summary(Long actor) {
        access.admin(actor);
        return new Summary(db.queryForObject("select count(*) from " + SOURCE, Long.class), db.queryForObject("select count(*) from " + SOURCE + " where x.method='COD'", Long.class), db.queryForObject("select count(*) from " + SOURCE + " where x.method='VNPAY'", Long.class), db.queryForObject("select count(*) from " + SOURCE + " where x.status in ('UNKNOWN','REVIEW_REQUIRED') or x.settlement='PENDING_RECONCILIATION'", Long.class));
    }

    public ManagementPage<PaymentRow> list(Long actor, int page, String search, String method) {
        access.admin(actor);
        MarketplaceAccess.page(page);
        String where = " where 1=1";
        var args = new ArrayList<Object>();
        String q = ManagementSupport.like(search);
        if (q != null) {
            where += " and (lower(o.code) like ? or lower(u.name) like ?)";
            args.add(q);
            args.add(q);
        }
        if (method != null && !method.isBlank()) {
            if (!Set.of("COD", "VNPAY").contains(method))
                throw MarketplaceException.invalid("Phương thức không hợp lệ.");
            where += " and x.method=?";
            args.add(method);
        }
        long n = db.queryForObject("select count(*) from " + SOURCE + where, Long.class, args.toArray());
        args.add(20);
        args.add(page * 20);
        var rows = db.query("select x.*,o.code,u.name buyer,s.name store from " + SOURCE + where + " order by x.created_at desc,x.record_id desc limit ? offset ?", (r, i) -> new PaymentRow(r.getString("record_id"), r.getLong("order_id"), r.getString("code"), r.getString("buyer"), r.getString("store"), r.getBigDecimal("amount"), r.getString("method"), r.getString("status"), r.getString("collection"), r.getString("settlement"), ManagementSupport.iso(r.getObject("created_at", LocalDateTime.class))), args.toArray());
        return new ManagementPage<>(rows, page, 20, n, (int) ((n + 19) / 20));
    }
}
