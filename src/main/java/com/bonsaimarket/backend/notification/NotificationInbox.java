package com.bonsaimarket.backend.notification;

import com.bonsaimarket.backend.marketplace.*;
import jakarta.persistence.*;

import java.time.*;
import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class NotificationInbox {
    public record Item(String id, String title, String body, String link, String createdAt, String readAt) {
    }

    private final EntityManager em;
    private final MarketplaceAccess access;

    public NotificationInbox(EntityManager em, MarketplaceAccess access) {
        this.em = em;
        this.access = access;
    }

    private void actor(Long id, boolean admin) {
        if (admin) access.admin(id);
        else access.actor(id);
    }

    private static String time(LocalDateTime time) {
        return time == null ? null : time.toInstant(ZoneOffset.UTC).toString();
    }

    private Item item(OrderNotification n) {
        return new Item(n.getId(), n.getSubject(), n.getBody(), n.getTargetLink(), time(n.getCreatedAt()), time(n.getReadAt()));
    }

    @Transactional(readOnly = true)
    public Map<String, Long> count(Long actor, boolean admin) {
        actor(actor, admin);
        return Map.of("unread", em.createQuery("select count(n) from OrderNotification n where n.channel='IN_APP' and n.recipientUserId=:id and n.readAt is null", Long.class).setParameter("id", actor).getSingleResult());
    }

    @Transactional(readOnly = true)
    public ManagementPage<Item> list(Long actor, boolean admin, int page, String unread) {
        actor(actor, admin);
        MarketplaceAccess.page(page);
        if (!Set.of("", "true", "false").contains(unread))
            throw MarketplaceException.invalid("Bộ lọc thông báo không hợp lệ.");
        String where = " where n.channel='IN_APP' and n.recipientUserId=:id" + (unread.isBlank() ? "" : unread.equals("true") ? " and n.readAt is null" : " and n.readAt is not null");
        long total = em.createQuery("select count(n) from OrderNotification n" + where, Long.class).setParameter("id", actor).getSingleResult();
        var rows = em.createQuery("select n from OrderNotification n" + where + " order by n.createdAt desc,n.id desc", OrderNotification.class).setParameter("id", actor).setFirstResult(page * 20).setMaxResults(20).getResultStream().map(this::item).toList();
        return new ManagementPage<>(rows, page, 20, total, (int) ((total + 19) / 20));
    }

    public Item read(Long actor, boolean admin, String id) {
        MarketplaceTransactions.requireReadCommitted(em);
        actor(actor, admin);
        OrderNotification n = em.find(OrderNotification.class, id);
        if (n == null || !"IN_APP".equals(n.getChannel()) || !actor.equals(n.getRecipientUserId()))
            throw MarketplaceException.missing();
        em.refresh(n, LockModeType.PESSIMISTIC_WRITE);
        if (n.getReadAt() == null) {
            n.setReadAt(access.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
            em.flush();
            em.refresh(n);
        }
        return item(n);
    }
}
