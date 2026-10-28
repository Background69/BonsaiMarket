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

    public AccountOrderService(EntityManager em) {
        this.em = em;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listOrders(Long buyerId, String statusStr, int page, int size) {
        if (page < 0) page = 0;
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
            }
        }

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

                summaries.add(new AccountOrderDtos.OrderSummary(
                        o.getId(),
                        o.getCode(),
                        o.getStatus().name(),
                        o.getStore() != null ? o.getStore().getId() : null,
                        o.getStore() != null ? o.getStore().getName() : "",
                        o.getSubtotal(),
                        o.getShippingFee(),
                        o.getTotal(),
                        o.getCreatedAt() != null ? o.getCreatedAt().toString() : null,
                        orderItems.size(),
                        totalQuantity,
                        orderItems
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

        boolean canCancel = order.getStatus() == OrderStatus.PENDING;
        boolean canPay = order.getStatus() == OrderStatus.PENDING;

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
                order.getCreatedAt() != null ? order.getCreatedAt().toString() : null,
                order.getUpdatedAt() != null ? order.getUpdatedAt().toString() : null,
                canCancel,
                canPay,
                itemSummaries
        );
    }

    public AccountOrderDtos.OrderDetailPage cancelOrder(Long buyerId, Long orderId, String reason) {
        Order order = em.find(Order.class, orderId);
        if (order == null || !order.getBuyer().getId().equals(buyerId)) {
            throw MarketplaceException.missing();
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw MarketplaceException.invalid("Chỉ có thể hủy đơn hàng đang chờ xác nhận.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        if (reason != null && !reason.isBlank()) {
            String existingNotes = order.getNotes() != null ? order.getNotes() + " | " : "";
            order.setNotes(existingNotes + "Lý do hủy: " + reason.trim());
        }
        em.flush();

        return getOrderDetail(buyerId, orderId);
    }
}
