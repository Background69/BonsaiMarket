package com.bonsaimarket.backend.order;

import com.bonsaimarket.backend.marketplace.MarketplaceException;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class AccountOrderService {
    private final EntityManager em;
    private final com.bonsaimarket.backend.management.OrderManagementService management;
    private final com.bonsaimarket.backend.review.ProductReviews reviews;

    public AccountOrderService(EntityManager em,com.bonsaimarket.backend.management.OrderManagementService management,
                               com.bonsaimarket.backend.review.ProductReviews reviews) {
        this.management=management;
        this.em = em;
        this.reviews = reviews;
    }

    @Transactional(readOnly = true)
    public AccountOrderDtos.Overview overview(Long buyerId) {
        var counts = new EnumMap<OrderStatus, Long>(OrderStatus.class);
        em.createQuery("select o.status,count(o) from Order o where o.buyer.id=:buyer group by o.status",Object[].class)
            .setParameter("buyer",buyerId).getResultList().forEach(row -> counts.put((OrderStatus)row[0],(Long)row[1]));
        return new AccountOrderDtos.Overview(counts.getOrDefault(OrderStatus.PENDING,0L),
            counts.getOrDefault(OrderStatus.SHIPPING,0L),counts.getOrDefault(OrderStatus.COMPLETED,0L),
            reviews.countEligiblePurchases(buyerId));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listOrders(Long buyerId, String statusStr, int page, int size) {
        return listOrders(buyerId,statusStr,page,size,null,null,null);
    }
    @Transactional(readOnly = true)
    public Map<String, Object> listOrders(Long buyerId, String statusStr, int page, int size, String search, String from, String to) {
        com.bonsaimarket.backend.marketplace.MarketplaceAccess.page(page);
        if (size <= 0 || size > 50) size = 10;

        StringBuilder where = new StringBuilder(" where o.buyer.id = :buyerId");
        Map<String, Object> params = new HashMap<>();
        params.put("buyerId", buyerId);

        OrderStatus status = null;
        if (statusStr != null && !statusStr.isBlank() && !"ALL".equalsIgnoreCase(statusStr)) {
            try {
                status = OrderStatus.valueOf(statusStr.trim().toUpperCase());
                where.append(" and o.status = :status");
                params.put("status", status);
            } catch (IllegalArgumentException ignored) {
                throw MarketplaceException.invalid("Trạng thái đơn không hợp lệ.");
            }
        }

        if(search!=null&&!search.isBlank()){
            if(search.length()>160)throw MarketplaceException.invalid("Từ khóa quá dài.");
            where.append(" and (lower(o.code) like :search or lower(o.buyer.name) like :search or lower(o.buyer.email) like :search)");params.put("search","%"+search.toLowerCase(Locale.ROOT).replace("%", "").replace("_", "")+"%");
        }
        try {
            var zone=java.time.ZoneId.of("Asia/Ho_Chi_Minh");
            var start=from==null||from.isBlank()?null:java.time.LocalDate.parse(from);
            var end=to==null||to.isBlank()?null:java.time.LocalDate.parse(to);
            if(start!=null&&end!=null&&start.isAfter(end))throw MarketplaceException.invalid("Ngày bắt đầu phải trước ngày kết thúc.");
            if(start!=null){where.append(" and o.createdAt>=:start");params.put("start",LocalDateTime.ofInstant(start.atStartOfDay(zone).toInstant(),java.time.ZoneOffset.UTC));}
            if(end!=null){where.append(" and o.createdAt<:end");params.put("end",LocalDateTime.ofInstant(end.plusDays(1).atStartOfDay(zone).toInstant(),java.time.ZoneOffset.UTC));}
        }catch(java.time.DateTimeException e){throw MarketplaceException.invalid("Ngày không hợp lệ.");}

        var countQuery = em.createQuery("select count(o) from Order o" + where, Long.class);
        var selectQuery = em.createQuery("select o from Order o join fetch o.store s" + where + " order by o.createdAt desc, o.id desc", Order.class);

        params.forEach((k, v) -> {
            countQuery.setParameter(k, v);
            selectQuery.setParameter(k, v);
        });

        long totalElements = countQuery.getSingleResult();
        int totalPages = (int) ((totalElements + size - 1) / size);

        selectQuery.setFirstResult(page * size).setMaxResults(size);
        List<Order> orderList = selectQuery.getResultList();

        List<AccountOrderDtos.OrderSummary> summaries = new ArrayList<>();
        if (!orderList.isEmpty()) {
            List<Long> orderIds = orderList.stream().map(Order::getId).toList();
            List<OrderItem> items = em.createQuery(
                    "select i from OrderItem i left join fetch i.product p where i.order.id in :orderIds", OrderItem.class)
                    .setParameter("orderIds", orderIds)
                    .getResultList();

            Map<Long, List<AccountOrderDtos.ItemSummary>> itemsByOrder = new LinkedHashMap<>();
            for (OrderItem i : items) {
                itemsByOrder.computeIfAbsent(i.getOrder().getId(), ignored -> new ArrayList<>())
                        .add(new AccountOrderDtos.ItemSummary(
                                i.getId(),
                                i.getProduct() != null ? i.getProduct().getId() : null,
                                i.getProductNameSnapshot(),
                                i.getProduct() != null ? i.getProduct().getImageUrl() : null,
                                i.getUnitPrice(),
                                i.getQuantity(),
                                i.getLineTotal()
                        ));
            }

            for (Order o : orderList) {
                List<AccountOrderDtos.ItemSummary> orderItems = itemsByOrder.getOrDefault(o.getId(), Collections.emptyList());
                int totalQuantity = orderItems.stream().mapToInt(AccountOrderDtos.ItemSummary::quantity).sum();
                var available = management.buyerAvailability(buyerId, o.getId());

                summaries.add(new AccountOrderDtos.OrderSummary(
                        o.getId(),
                        o.getCode(),
                        o.getStatus().name(),
                        o.getStore() != null ? o.getStore().getId() : null,
                        o.getStore() != null ? o.getStore().getName() : "",
                        o.getSubtotal(),
                        o.getShippingFee(),
                        o.getTotal(),
                        iso(o.getCreatedAt()),
                        orderItems.size(),
                        totalQuantity,
                        orderItems, available.actions().contains("CANCELLED"), canPay(o), available.blockers()
                ));
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", summaries);
        result.put("page", page);
        result.put("size", size);
        result.put("totalElements", totalElements);
        result.put("totalPages", totalPages);
        return result;
    }

    @Transactional(readOnly = true)
    public AccountOrderDtos.OrderDetailPage getOrderDetail(Long buyerId, Long orderId) {
        Order order = em.find(Order.class, orderId);
        if (order == null || !order.getBuyer().getId().equals(buyerId)) {
            throw MarketplaceException.missing();
        }

        List<OrderItem> items = em.createQuery(
                "select i from OrderItem i left join fetch i.product p where i.order.id = :orderId", OrderItem.class)
                .setParameter("orderId", orderId)
                .getResultList();

        List<AccountOrderDtos.ItemSummary> itemSummaries = items.stream().map(i -> new AccountOrderDtos.ItemSummary(
                i.getId(),
                i.getProduct() != null ? i.getProduct().getId() : null,
                i.getProductNameSnapshot(),
                i.getProduct() != null ? i.getProduct().getImageUrl() : null,
                i.getUnitPrice(),
                i.getQuantity(),
                i.getLineTotal()
        )).toList();

        var shared=management.buyerDetail(buyerId,orderId);
        boolean canCancel = shared.actions().contains("CANCELLED");
        boolean canPay = canPay(order);

        return new AccountOrderDtos.OrderDetailPage(
                order.getId(),
                order.getCode(),
                order.getStatus().name(),
                order.getStore() != null ? order.getStore().getId() : null,
                order.getStore() != null ? order.getStore().getName() : "",
                order.getShippingAddressSnapshot(),
                order.getShippingMethod(),
                order.getShippingFee(),
                order.getSubtotal(),
                order.getTotal(),
                order.getNotes(),
                iso(order.getCreatedAt()),
                iso(order.getUpdatedAt()),
                canCancel,
                canPay,
                itemSummaries, shared.actions(),shared.payment(),shared.shippingAddress(),shared.buyer(),
                shared.actionBlockers(), shared.history()
        );
    }
    private boolean canPay(Order order) {
        var intent=em.find(com.bonsaimarket.backend.payment.OrderPayment.class,order.getId());
        return java.util.Set.of(OrderStatus.PENDING,OrderStatus.CONFIRMED).contains(order.getStatus()) && (intent==null||"VNPAY".equals(intent.getMethod())&&java.util.Set.of("PENDING","FAILED").contains(intent.getStatus()))
            && em.createQuery("select count(t) from Transaction t where t.order.id=:id and t.status in :states", Long.class)
                .setParameter("id", order.getId()).setParameter("states", List.of(com.bonsaimarket.backend.payment.TransactionStatus.PENDING,
                    com.bonsaimarket.backend.payment.TransactionStatus.SUCCESS,com.bonsaimarket.backend.payment.TransactionStatus.UNKNOWN,com.bonsaimarket.backend.payment.TransactionStatus.EXPIRED)).getSingleResult() == 0;
    }

    public AccountOrderDtos.OrderDetailPage cancelOrder(Long buyerId, Long orderId, String reason) {
        management.cancelByBuyer(buyerId,orderId,reason);
        if(reason!=null&&!reason.isBlank()){
            if(reason.length()>1000)throw MarketplaceException.invalid("Lý do hủy tối đa 1000 ký tự.");
            Order order=em.find(Order.class,orderId);String old=order.getNotes()==null?"":order.getNotes();order.setNotes(old+"\nLý do hủy: "+reason.strip());
        }
        return getOrderDetail(buyerId,orderId);
    }
    private static String iso(LocalDateTime value){return value==null?null:value.atOffset(java.time.ZoneOffset.UTC).toString();}
}
