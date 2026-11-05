package com.bonsaimarket.backend.management;

import com.bonsaimarket.backend.community.ContentAccess;
import com.bonsaimarket.backend.marketplace.*;
import com.bonsaimarket.backend.order.Order;

import java.time.LocalDateTime;
import java.util.*;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/**
 * A single support case per order. Decisions have no financial side effects.
 */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class SupportCaseService {
    public record CaseRow(long id, long orderId, String code, String buyer, String seller, String status, String reason,
                          long revision, String updatedAt) {
    }

    public record Event(long id, String actor, String kind, String body, String status, String createdAt) {
    }

    public record Detail(CaseRow support, List<Event> events, boolean canRespond, List<String> actions) {
    }

    private final JdbcTemplate db;
    private final MarketplaceAccess access;
    private final jakarta.persistence.EntityManager em;
    private static final String SELECT = "select c.*,o.code,b.name buyer,s.name seller from support_cases c join orders o on o.id=c.order_id join users b on b.id=o.buyer_user_id join stores st on st.id=o.store_id join users s on s.id=st.owner_user_id";

    public SupportCaseService(JdbcTemplate db, MarketplaceAccess access, jakarta.persistence.EntityManager em) {
        this.db = db;
        this.access = access;
        this.em = em;
    }

    private CaseRow map(java.sql.ResultSet r, int n) throws java.sql.SQLException {
        return new CaseRow(r.getLong("id"), r.getLong("order_id"), r.getString("code"), r.getString("buyer"), r.getString("seller"), r.getString("status"), r.getString("reason"), r.getLong("revision"), ManagementSupport.iso(r.getObject("updated_at", LocalDateTime.class)));
    }

    private void authorize(Long actor, long orderId, boolean admin) {
        if (admin) {
            access.admin(actor);
            return;
        }
        access.actor(actor);
        var o = em.find(Order.class, orderId);
        if (o == null || !o.getBuyer().getId().equals(actor) && !o.getStore().getOwner().getId().equals(actor))
            throw MarketplaceException.missing();
    }

    private CaseRow row(long id, boolean lock) {
        var rows = db.query(SELECT + " where c.id=?" + (lock ? " for update" : ""), this::map, id);
        if (rows.isEmpty()) throw MarketplaceException.missing();
        return rows.getFirst();
    }

    @Transactional(readOnly = true)
    public ManagementPage<CaseRow> list(Long actor, boolean admin, int page, String status) {
        if (admin) access.admin(actor);
        else access.actor(actor);
        MarketplaceAccess.page(page);
        String where = " where 1=1";
        var args = new ArrayList<Object>();
        if (!admin) {
            where += " and (o.buyer_user_id=? or st.owner_user_id=?)";
            args.add(actor);
            args.add(actor);
        }
        if (status != null && !status.isBlank()) {
            if (!Set.of("OPEN", "IN_REVIEW", "RESOLVED", "CLOSED").contains(status))
                throw MarketplaceException.invalid("Trạng thái hồ sơ không hợp lệ.");
            where += " and c.status=?";
            args.add(status);
        }
        long total = db.queryForObject("select count(*) from (" + SELECT + where + ") counted", Long.class, args.toArray());
        args.add(20);
        args.add(page * 20);
        return new ManagementPage<>(db.query(SELECT + where + " order by c.updated_at desc,c.id desc limit ? offset ?", this::map, args.toArray()), page, 20, total, (int) ((total + 19) / 20));
    }

    @Transactional(readOnly = true)
    public Detail detail(Long actor, boolean admin, long id) {
        var c = row(id, false);
        authorize(actor, c.orderId(), admin);
        var events = db.query("select e.*,u.name actor_name from support_case_events e join users u on u.id=e.actor_id where e.case_id=? order by e.revision", (r, n) -> new Event(r.getLong("id"), r.getString("actor_name"), r.getString("event_type"), r.getString("body"), r.getString("status"), ManagementSupport.iso(r.getObject("created_at", LocalDateTime.class))), id);
        var actions = admin ? c.status().equals("OPEN") ? List.of("IN_REVIEW") : c.status().equals("IN_REVIEW") ? List.of("RESOLVED", "CLOSED") : List.<String>of() : List.<String>of();
        return new Detail(c, events, Set.of("OPEN", "IN_REVIEW").contains(c.status()), actions);
    }

    public Detail open(Long actor, long orderId, String input) {
        MarketplaceTransactions.requireReadCommitted(em);
        authorize(actor, orderId, false);
        access.lock(Order.class, orderId);
        String reason = ContentAccess.text(input, 1000);
        var existing = db.query("select id,opened_by,reason from support_cases where order_id=?", (r, n) -> new Object[]{r.getLong(1), r.getLong(2), r.getString(3)}, orderId);
        if (!existing.isEmpty()) {
            var r = existing.getFirst();
            if (actor.equals(r[1]) && reason.equals(r[2])) return detail(actor, false, (Long) r[0]);
            throw new MarketplaceException(409, "SUPPORT_EXISTS", "Đơn đã có hồ sơ. Mở hồ sơ hiện có để phản hồi.");
        }
        db.update("insert into support_cases(order_id,opened_by,reason,status,revision,created_at,updated_at) values(?,?,?,'OPEN',0,?,?)", orderId, actor, reason, access.now(), access.now());
        long id = db.queryForObject("select id from support_cases where order_id=?", Long.class, orderId);
        event(actor, id, "OPENED", reason, "open", "OPEN", 0);
        return detail(actor, false, id);
    }

    public Detail respond(Long actor, boolean admin, long id, long revision, String input, String key) {
        MarketplaceTransactions.requireReadCommitted(em);
        var initial = row(id, false);
        authorize(actor, initial.orderId(), admin);
        var c = row(id, true);
        String body = ContentAccess.text(input, 4000);
        if (key == null || !key.matches("[A-Za-z0-9_-]{16,80}"))
            throw MarketplaceException.invalid("Thiếu mã gửi phản hồi hợp lệ.");
        var prior = db.queryForList("select body from support_case_events where case_id=? and actor_id=? and request_key=?", id, actor, key);
        if (!prior.isEmpty()) {
            if (!body.equals(prior.getFirst().get("body")))
                throw new MarketplaceException(409, "REQUEST_CONFLICT", "Mã gửi đã dùng cho nội dung khác.");
            return detail(actor, admin, id);
        }
        MarketplaceAccess.revision(c.revision(), revision);
        if (!Set.of("OPEN", "IN_REVIEW").contains(c.status()))
            throw new MarketplaceException(409, "SUPPORT_CLOSED", "Hồ sơ đã đóng, không thể thêm phản hồi.");
        if (db.queryForObject("select count(*) from support_case_events where actor_id=? and created_at>=?", Long.class, actor, access.now().minusHours(1)) >= 30)
            throw new MarketplaceException(429, "SUPPORT_RATE_LIMIT", "Bạn đã gửi nhiều phản hồi. Vui lòng thử lại sau.");
        advance(id, c.revision());
        event(actor, id, "RESPONSE", body, key, c.status(), c.revision() + 1);
        return detail(actor, admin, id);
    }

    public Detail decide(Long actor, long id, long revision, String status, String input) {
        MarketplaceTransactions.requireReadCommitted(em);
        access.admin(actor);
        var c = row(id, true);
        MarketplaceAccess.revision(c.revision(), revision);
        String reason = ContentAccess.text(input, 1000);
        if (!(c.status().equals("OPEN") && "IN_REVIEW".equals(status) || c.status().equals("IN_REVIEW") && Set.of("RESOLVED", "CLOSED").contains(status)))
            throw new MarketplaceException(409, "SUPPORT_STATE_INVALID", "Quyết định không phù hợp trạng thái hồ sơ.");
        db.update("update support_cases set status=?,revision=revision+1,updated_at=? where id=?", status, access.now(), id);
        event(actor, id, "DECISION", reason, "decision-" + (revision + 1), status, revision + 1);
        return detail(actor, true, id);
    }

    private void advance(long id, long revision) {
        db.update("update support_cases set revision=revision+1,updated_at=? where id=? and revision=?", access.now(), id, revision);
    }

    private void event(Long actor, long id, String kind, String body, String key, String status, long revision) {
        db.update("insert into support_case_events(case_id,actor_id,event_type,body,request_key,status,revision,created_at) values(?,?,?,?,?,?,?,?)", id, actor, kind, body, key, status, revision, access.now());
        MarketplaceAudit.record(em, access, actor, "SUPPORT_" + (kind.equals("OPENED") ? "OPENED" : kind.equals("RESPONSE") ? "RESPONSE" : status), "SUPPORT_CASE", id, kind.equals("RESPONSE") ? "Đã gửi phản hồi trong hồ sơ hỗ trợ." : body);
        em.flush();
    }
}
