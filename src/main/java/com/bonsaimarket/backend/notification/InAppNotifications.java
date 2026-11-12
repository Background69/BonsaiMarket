package com.bonsaimarket.backend.notification;

import com.bonsaimarket.backend.marketplace.*;
import com.bonsaimarket.backend.user.User;
import jakarta.persistence.EntityManager;

import java.util.*;

/**
 * Transactional in-app delivery through the existing outbox; no SMTP or provider calls.
 */
public final class InAppNotifications {
    private InAppNotifications() {
    }

    private static void persist(EntityManager em, MarketplaceAccess access, Long recipient, String key, String title, String body, String link) {
        if (!em.isJoinedToTransaction())
            throw new IllegalStateException("Notification requires the business transaction");
        if (link == null || !link.startsWith("/") || link.startsWith("//"))
            throw new IllegalArgumentException("Local target required");
        String dedup = "INAPP:" + key + ":" + recipient;
        if (em.createQuery("select count(n) from OrderNotification n where n.dedupKey=:key", Long.class).setParameter("key", dedup).getSingleResult() > 0)
            return;
        var user = em.find(User.class, recipient);
        if (user == null) return;
        var row = new OrderNotification();
        row.setId(UUID.randomUUID().toString());
        row.setChannel("IN_APP");
        row.setRecipientUserId(recipient);
        row.setRecipient(user.getEmail());
        row.setEventType("ADMIN_ACTIVITY");
        row.setSubject(title);
        row.setBody(body);
        row.setTargetLink(link);
        row.setDedupKey(dedup);
        row.setStatus("PENDING");
        row.setAttempts(0);
        row.setCreatedAt(access.now());
        row.setUpdatedAt(access.now());
        row.setNextAttemptAt(access.now());
        em.persist(row);
    }

    public static void admins(EntityManager em, MarketplaceAccess access, Long actor, String key, String title, String link) {
        for (var user : em.createQuery("select u from User u where u.role=com.bonsaimarket.backend.user.UserRole.ADMIN and u.isActive=true order by u.id", User.class).getResultList())
            if (!user.getId().equals(actor))
                persist(em, access, user.getId(), key, title, "Có công việc mới cần xem xét. Mở hồ sơ liên quan để kiểm tra thông tin hiện tại.", link);
    }

    public static void report(EntityManager em, MarketplaceAccess access, Long actor, long id) {
        admins(em, access, actor, "REPORT:" + id, "Báo cáo vi phạm mới", "/admin/reports?record=" + id);
    }
    public static void reportSubmitted(EntityManager em, MarketplaceAccess access, Long reporter, long id) {
        persist(em,access,reporter,"REPORT_SUBMITTED:"+id,"Đã tiếp nhận báo cáo #"+id,"Báo cáo đã được gửi, đang chờ quản trị viên xem xét. Chưa có kết luận về người bị báo cáo.","/notifications");
    }

    public static void wallet(EntityManager em,MarketplaceAccess access,long recipient,String transaction,String state){
        String title=switch(state){case "PENDING"->"Giao dịch ví thử nghiệm đang xử lý";case "COMPLETED"->"Đã hoàn tất giao dịch ví thử nghiệm";case "FAILED"->"Giao dịch ví thử nghiệm thất bại";default->"Cập nhật ví thử nghiệm";};
        persist(em,access,recipient,"VND_WALLET:"+transaction+":"+state,title,"Giao dịch tiền mô phỏng có cập nhật. Mở ví để xem trạng thái; không có chuyển tiền thật.","/wallet?transaction="+transaction);
    }

    public static void audit(EntityManager em, MarketplaceAccess access, MarketplaceAudit audit) {
        String action = audit.getAction(), type = audit.getTargetType();
        long id = audit.getTargetId();
        Long actor = audit.getActorId();
        String key = "AUDIT:" + audit.getId();
        if (action.equals("APPLICATION_SUBMITTED"))
            admins(em, access, actor, key, "Yêu cầu mở gian hàng mới", "/admin/store-applications?record=" + id);
        if (action.equals("COMMUNITY_REPORTED"))
            admins(em, access, actor, key, "Báo cáo cộng đồng mới", "/admin/reports?record=" + id);
        if (action.equals("REFUND_REQUESTED")) {
            var orders = em.createNativeQuery("select order_id from order_refunds where id=?", Long.class).setParameter(1, id).getResultList();
            if (!orders.isEmpty())
                admins(em, access, actor, key, "Hồ sơ hoàn tiền cần xem xét", "/admin/refunds/" + orders.getFirst());
        }
        if (action.equals("SUPPORT_OPENED") || action.equals("SUPPORT_RESPONSE"))
            admins(em, access, actor, key, "Hồ sơ tranh chấp có cập nhật", "/admin/disputes/" + id);
        var recipients = new LinkedHashSet<Long>();
        String title = null, link = null;
        if (action.equals("APPLICATION_APPROVED") || action.equals("APPLICATION_REJECTED")) {
            recipients.addAll(em.createQuery("select a.applicant.id from SellerApplication a where a.id=:id", Long.class).setParameter("id", id).getResultList());
            title = action.endsWith("APPROVED") ? "Yêu cầu mở gian hàng đã được duyệt" : "Yêu cầu mở gian hàng đã được phản hồi";
            link = "/store-application";
        } else if (Set.of("PRODUCT_HIDE", "PRODUCT_RESTORE", "PRODUCT_REMOVE").contains(action)) {
            recipients.addAll(em.createQuery("select p.store.owner.id from Product p where p.id=:id", Long.class).setParameter("id", id).getResultList());
            title = "Trạng thái kiểm duyệt sản phẩm đã thay đổi";
            link = "/seller/products/" + id + "/content";
        } else if (Set.of("STORE_SUSPEND", "STORE_RESTORE").contains(action)) {
            recipients.addAll(em.createQuery("select s.owner.id from Store s where s.id=:id", Long.class).setParameter("id", id).getResultList());
            title = action.equals("STORE_SUSPEND") ? "Gian hàng đã bị đình chỉ" : "Gian hàng đã được khôi phục";
            link = "/seller";
        } else if (type.equals("SUPPORT_CASE")) {
            var rows = em.createNativeQuery("select o.buyer_user_id,s.owner_user_id from support_cases c join orders o on o.id=c.order_id join stores s on s.id=o.store_id where c.id=?", Object[].class).setParameter(1, id).getResultList();
            for (var value : rows) {
                var row = (Object[]) value;
                recipients.add(((Number) row[0]).longValue());
                recipients.add(((Number) row[1]).longValue());
            }
            title = "Hồ sơ hỗ trợ đơn hàng có cập nhật";
            link = "/support-cases/" + id;
        } else if (action.startsWith("REPORT_") && !action.equals("REPORT_CREATED")) {
            recipients.addAll(em.createQuery("select r.reporter.id from MarketplaceReport r where r.id=:id", Long.class).setParameter("id", id).getResultList());
            title = switch(action){case "REPORT_IN_REVIEW" -> "Báo cáo đang được xem xét";case "REPORT_DISMISSED" -> "Báo cáo được đóng: không đủ cơ sở";default -> "Báo cáo đã được xử lý";};
            link = "/notifications";
        }
        if (title != null) for (long recipient : recipients)
            if (!actor.equals(recipient))
                persist(em, access, recipient, key, title,
                    type.equals("REPORT") ? "Báo cáo #"+id+" có cập nhật: "+title+". Nội dung chi tiết của hồ sơ được giữ riêng cho quản trị viên."
                        : "Có quyết định hoặc phản hồi mới. Xem hồ sơ để đối chiếu; thông báo này không xác nhận kết quả thanh toán.", link);
    }
}
