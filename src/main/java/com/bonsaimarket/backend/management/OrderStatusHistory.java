package com.bonsaimarket.backend.management;

import com.bonsaimarket.backend.marketplace.MarketplaceAccess;
import com.bonsaimarket.backend.marketplace.MarketplaceAudit;
import com.bonsaimarket.backend.order.Order;
import com.bonsaimarket.backend.order.OrderStatus;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Adds transition evidence to the existing audit without fabricating legacy history. */
@Component
public class OrderStatusHistory {
    private final EntityManager em;
    private final MarketplaceAccess access;
    private final ObjectMapper json;

    public OrderStatusHistory(EntityManager em, MarketplaceAccess access, ObjectMapper json) {
        this.em = em; this.access = access; this.json = json;
    }

    public record Entry(Long id, String fromStatus, String toStatus, String actorLabel,
                        Long actorId, String createdAt) {}

    public void record(Long actorId, Order order, OrderStatus previous) {
        String role = actorId.equals(order.getBuyer().getId()) ? "BUYER"
            : actorId.equals(order.getStore().getOwner().getId()) ? "SELLER" : "ADMIN";
        String detail = json.writeValueAsString(java.util.Map.of("version", 1, "from", previous.name(),
            "to", order.getStatus().name(), "actorRole", role));
        MarketplaceAudit.record(em, access, actorId, "ORDER_" + order.getStatus().name(), "ORDER", order.getId(), detail);
    }

    public List<Entry> list(Long orderId, boolean admin) {
        return em.createQuery("select a from MarketplaceAudit a where a.targetType='ORDER' and a.targetId=:id order by a.createdAt,a.id", MarketplaceAudit.class)
            .setParameter("id", orderId).getResultStream().filter(a -> status(a.getAction()) != null)
            .map(a -> entry(a, admin)).toList();
    }

    private Entry entry(MarketplaceAudit audit, boolean admin) {
        String target = status(audit.getAction()), previous = null, actor = "Tài khoản thực hiện";
        try {
            var detail = json.readTree(audit.getReason());
            if (detail != null && detail.path("version").asInt() == 1 && target.equals(detail.path("to").asText())) {
                previous = OrderStatus.valueOf(detail.path("from").asText()).name();
                actor = switch (detail.path("actorRole").asText()) {
                    case "BUYER" -> "Người mua";
                    case "SELLER" -> "Gian hàng";
                    case "ADMIN" -> "Quản trị viên";
                    default -> "Tài khoản thực hiện";
                };
            }
        } catch (RuntimeException ignored) { /* Older audit rows did not store the previous state or actor role. */ }
        return new Entry(audit.getId(), previous, target, actor, admin ? audit.getActorId() : null,
            ManagementSupport.iso(audit.getCreatedAt()));
    }

    private static String status(String action) {
        if (action == null || !action.startsWith("ORDER_")) return null;
        try { return OrderStatus.valueOf(action.substring(6)).name(); }
        catch (IllegalArgumentException ignored) { return null; }
    }
}
