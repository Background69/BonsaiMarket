package com.bonsaimarket.backend.management;

import com.bonsaimarket.backend.management.ManagementSupport.Range;
import com.bonsaimarket.backend.marketplace.MarketplaceAccess;
import com.bonsaimarket.backend.order.OrderStatus;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** Read-only revenue statistics: bar chart by day and pie chart by category (functions 10, 11, 13b). */
@Service
@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
public class StatisticsService {
    private static final int MAX_PIE_SLICES = 8;
    private final EntityManager em;
    private final MarketplaceAccess access;
    private final Clock clock;

    public StatisticsService(EntityManager em, MarketplaceAccess access, Clock clock) {
        this.em = em;
        this.access = access;
        this.clock = clock;
    }

    public record DayPoint(String date, BigDecimal revenue, long orders) { }

    public record CategoryShare(Long categoryId, String name, BigDecimal revenue, long quantity) { }

    public record Overview(String from, String to, BigDecimal totalRevenue, long revenueOrders, long pendingOrders,
                           List<DayPoint> days, List<CategoryShare> categories, Map<String, Long> statusCounts) { }

    public Overview admin(Long actorId, String from, String to) {
        access.admin(actorId);
        return build(null, ManagementSupport.range(from, to, clock));
    }

    public Overview seller(Long actorId, String from, String to) {
        access.seller(actorId);
        return build(actorId, ManagementSupport.range(from, to, clock));
    }

    /** ownerId == null means the whole marketplace; otherwise only orders of the store owned by that user. */
    private Overview build(Long ownerId, Range range) {
        String ownerFilter = ownerId == null ? "" : " and o.store.owner.id=:owner";

        var revenueQuery = em.createQuery("select o.createdAt, o.subtotal from Order o where o.status in :statuses"
                + " and o.createdAt>=:start and o.createdAt<:end" + ownerFilter, Object[].class)
            .setParameter("statuses", ManagementSupport.REVENUE_STATUSES)
            .setParameter("start", range.start())
            .setParameter("end", range.end());
        if (ownerId != null) revenueQuery.setParameter("owner", ownerId);

        Map<LocalDate, BigDecimal> revenueByDay = new TreeMap<>();
        Map<LocalDate, Long> ordersByDay = new TreeMap<>();
        BigDecimal total = BigDecimal.ZERO;
        long orderCount = 0;
        for (Object[] row : revenueQuery.getResultList()) {
            LocalDate day = ManagementSupport.businessDay((LocalDateTime) row[0]);
            BigDecimal amount = row[1] == null ? BigDecimal.ZERO : new BigDecimal(row[1].toString());
            revenueByDay.merge(day, amount, BigDecimal::add);
            ordersByDay.merge(day, 1L, Long::sum);
            total = total.add(amount);
            orderCount++;
        }
        List<DayPoint> days = new ArrayList<>();
        for (LocalDate day = range.from(); !day.isAfter(range.to()); day = day.plusDays(1)) {
            days.add(new DayPoint(day.toString(), revenueByDay.getOrDefault(day, BigDecimal.ZERO), ordersByDay.getOrDefault(day, 0L)));
        }

        var categoryQuery = em.createQuery("select c.id, c.name, sum(i.lineTotal), sum(i.quantity)"
                + " from OrderItem i join i.order o join i.product p join p.category c"
                + " where o.status in :statuses and o.createdAt>=:start and o.createdAt<:end" + ownerFilter
                + " group by c.id, c.name order by sum(i.lineTotal) desc, c.id", Object[].class)
            .setParameter("statuses", ManagementSupport.REVENUE_STATUSES)
            .setParameter("start", range.start())
            .setParameter("end", range.end());
        if (ownerId != null) categoryQuery.setParameter("owner", ownerId);
        List<CategoryShare> categories = new ArrayList<>();
        BigDecimal otherRevenue = BigDecimal.ZERO;
        long otherQuantity = 0;
        for (Object[] row : categoryQuery.getResultList()) {
            BigDecimal revenue = row[2] == null ? BigDecimal.ZERO : new BigDecimal(row[2].toString());
            long quantity = row[3] == null ? 0 : ((Number) row[3]).longValue();
            if (categories.size() < MAX_PIE_SLICES) {
                categories.add(new CategoryShare(((Number) row[0]).longValue(), String.valueOf(row[1]), revenue, quantity));
            } else {
                otherRevenue = otherRevenue.add(revenue);
                otherQuantity += quantity;
            }
        }
        if (otherQuantity > 0 || otherRevenue.signum() > 0) categories.add(new CategoryShare(null, "Khác", otherRevenue, otherQuantity));

        var statusQuery = em.createQuery("select o.status, count(o) from Order o where o.createdAt>=:start and o.createdAt<:end"
                + ownerFilter + " group by o.status", Object[].class)
            .setParameter("start", range.start())
            .setParameter("end", range.end());
        if (ownerId != null) statusQuery.setParameter("owner", ownerId);
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) statusCounts.put(status.name(), 0L);
        for (Object[] row : statusQuery.getResultList()) statusCounts.put(((OrderStatus) row[0]).name(), ((Number) row[1]).longValue());

        var pendingQuery = em.createQuery("select count(o) from Order o where o.status=:pending" + ownerFilter, Long.class)
            .setParameter("pending", OrderStatus.PENDING);
        if (ownerId != null) pendingQuery.setParameter("owner", ownerId);
        long pending = pendingQuery.getSingleResult();

        return new Overview(range.from().toString(), range.to().toString(), total, orderCount, pending, days, categories, statusCounts);
    }
}
