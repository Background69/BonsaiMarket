package com.bonsaimarket.backend.management;

import com.bonsaimarket.backend.marketplace.*;

import java.util.*;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Safe business projection. Raw audit payloads remain in the database.
 */
@Service
@Transactional(readOnly = true)
public class AdminHistory {
    public record Entry(long id, long actorId, String actor, String action, String entity, long entityId, String reason,
                        String createdAt, String link, String before, String after) {
    }

    private final JdbcTemplate db;
    private final MarketplaceAccess access;
    private final AdminAuditPresentation presentation;

    public AdminHistory(JdbcTemplate db, MarketplaceAccess access, AdminAuditPresentation presentation) {
        this.db = db;
        this.access = access;
        this.presentation = presentation;
    }

    static final Map<String, String> LABELS = Map.ofEntries(Map.entry("USER_ACTIVATE", "Đã mở khóa tài khoản"), Map.entry("USER_DEACTIVATE", "Đã khóa tài khoản"), Map.entry("APPLICATION_SUBMITTED", "Đã gửi hồ sơ gian hàng"), Map.entry("APPLICATION_APPROVED", "Đã duyệt gian hàng"), Map.entry("APPLICATION_REJECTED", "Đã từ chối hồ sơ"), Map.entry("PRODUCT_HIDE", "Đã ẩn sản phẩm"), Map.entry("PRODUCT_RESTORE", "Đã khôi phục sản phẩm"), Map.entry("PRODUCT_REMOVE", "Đã gỡ sản phẩm"), Map.entry("STORE_SUSPEND", "Đã đình chỉ gian hàng"), Map.entry("STORE_RESTORE", "Đã khôi phục gian hàng"), Map.entry("SUPPORT_OPENED", "Đã mở hồ sơ tranh chấp"), Map.entry("SUPPORT_RESPONSE", "Đã phản hồi hồ sơ"), Map.entry("SUPPORT_IN_REVIEW", "Đã tiếp nhận hồ sơ"), Map.entry("SUPPORT_RESOLVED", "Đã ghi nhận kết luận hồ sơ"), Map.entry("SUPPORT_CLOSED", "Đã đóng hồ sơ"));

    static String label(String action) {
        return LABELS.getOrDefault(action, AuditPresentation.action(action));
    }

    static String link(String type, long id) {
        return switch (type) {
            case "USER" -> "/admin/users/" + id;
            case "STORE" -> "/admin/stores/" + id;
            case "PRODUCT" -> "/admin/products/" + id;
            case "ORDER" -> "/admin/orders/" + id;
            case "APPLICATION" -> "/admin/store-applications?record=" + id;
            case "REPORT" -> "/admin/reports?record=" + id;
            case "SUPPORT_CASE" -> "/admin/disputes/" + id;
            case "COMMUNITY_POST", "COMMUNITY_COMMENT" -> "/admin/community";
            default -> "/admin/audit";
        };
    }

    private static String entity(String type) {
        return switch (type) {
            case "USER" -> "Tài khoản";
            case "STORE" -> "Gian hàng";
            case "PRODUCT" -> "Sản phẩm";
            case "ORDER", "ORDER_CANCELLATION" -> "Đơn hàng";
            case "APPLICATION" -> "Hồ sơ gian hàng";
            case "REPORT" -> "Báo cáo";
            case "SUPPORT_CASE" -> "Hồ sơ tranh chấp";
            case "REFUND" -> "Hồ sơ hoàn tiền";
            case "CATEGORY" -> "Danh mục";
            default -> "Nội dung quản trị";
        };
    }

    private List<Entry> query(String where, List<Object> args, int page) {
        var all = new ArrayList<>(args);
        all.add(20);
        all.add(page * 20);
        return db.query("select a.*,u.name actor_name,ast.before_state,ast.after_state from marketplace_audit a join users u on u.id=a.actor_id left join marketplace_audit_states ast on ast.audit_id=a.id" + where + " order by a.created_at desc,a.id desc limit ? offset ?", (r, n) -> {
            String a = r.getString("action"), t = r.getString("target_type");
            long id = r.getLong("target_id");
            var d = presentation.detail(a, r.getString("reason"), r.getString("before_state"), r.getString("after_state"));
            String targetLink = link(t, id);
            if (t.equals("REFUND")) {
                var order = db.query("select order_id from order_refunds where id=?", (rs, i) -> rs.getLong(1), id).stream().findFirst().orElse(null);
                if (order != null) targetLink = "/admin/refunds/" + order;
            }
            return new Entry(r.getLong("id"), r.getLong("actor_id"), r.getString("actor_name"), presentation.action(a), entity(t), id, d.reason(), ManagementSupport.iso(r.getObject("created_at", java.time.LocalDateTime.class)), targetLink, d.before(), d.after());
        }, all.toArray());
    }

    public List<Entry> target(Long actor, String type, long id) {
        access.admin(actor);
        return query(" where a.target_type=? and a.target_id=?", List.of(type, id), 0);
    }

    public ManagementPage<Entry> list(Long actor, int page, String search, String type) {
        return list(actor, page, search, type, "", "", "");
    }

    public ManagementPage<Entry> list(Long actor, int page, String search, String type, String action, String from, String to) {
        access.admin(actor);
        MarketplaceAccess.page(page);
        String where = " where 1=1";
        var args = new ArrayList<Object>();
        String q = ManagementSupport.like(search);
        if (q != null) {
            where += " and lower(u.name) like ?";
            args.add(q);
        }
        if (type != null && !type.isBlank()) {
            if (!Set.of("USER", "STORE", "PRODUCT", "ORDER", "ORDER_CANCELLATION", "APPLICATION", "REPORT", "SUPPORT_CASE", "COMMUNITY_POST", "COMMUNITY_COMMENT", "CATEGORY", "REFUND").contains(type))
                throw MarketplaceException.invalid("Nhóm nhật ký không hợp lệ.");
            where += " and a.target_type=?";
            args.add(type);
        }
        if (action != null && !action.isBlank()) {
            if (!action.matches("[A-Z_]{1,40}")) throw MarketplaceException.invalid("Hoạt động không hợp lệ.");
            where += " and a.action=?";
            args.add(action);
        }
        var start = ManagementSupport.date(from);
        var end = ManagementSupport.date(to);
        if (start != null && end != null && start.isAfter(end))
            throw MarketplaceException.invalid("Khoảng ngày không hợp lệ.");
        if (start != null) {
            where += " and a.created_at>=?";
            args.add(ManagementSupport.startOf(start));
        }
        if (end != null) {
            where += " and a.created_at<?";
            args.add(ManagementSupport.endOf(end));
        }
        long n = db.queryForObject("select count(*) from marketplace_audit a join users u on u.id=a.actor_id" + where, Long.class, args.toArray());
        return new ManagementPage<>(query(where, args, page), page, 20, n, (int) ((n + 19) / 20));
    }
}
