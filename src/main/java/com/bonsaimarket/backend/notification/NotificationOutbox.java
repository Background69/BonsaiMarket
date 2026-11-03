package com.bonsaimarket.backend.notification;

import com.bonsaimarket.backend.marketplace.MarketplaceAccess;
import jakarta.persistence.EntityManager;
import java.util.UUID;

/** Shared persistence for order and question mail; the existing dispatcher owns delivery. */
public final class NotificationOutbox {
    private NotificationOutbox() {}

    public static void enqueue(EntityManager em, MarketplaceAccess access, Long orderId, String event,
                               String recipient, String key, String subject, String body) {
        if (!em.isJoinedToTransaction()) throw new IllegalStateException("Outbox requires the business transaction");
        if (orderId == null && !event.equals("PRODUCT_QUESTION_ASKED")) throw new IllegalArgumentException("Notification target required");
        if (em.createQuery("select count(n) from OrderNotification n where n.dedupKey=:key", Long.class)
            .setParameter("key", key).getSingleResult() > 0) return;
        var row = new OrderNotification();
        row.setId(UUID.randomUUID().toString()); row.setOrderId(orderId); row.setEventType(event);
        row.setRecipient(recipient); row.setDedupKey(key); row.setSubject(subject); row.setBody(body);
        row.setStatus("PENDING"); row.setAttempts(0); row.setCreatedAt(access.now());
        row.setUpdatedAt(access.now()); row.setNextAttemptAt(access.now()); em.persist(row);
    }
}
