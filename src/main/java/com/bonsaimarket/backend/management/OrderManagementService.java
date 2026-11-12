package com.bonsaimarket.backend.management;

import com.bonsaimarket.backend.inventory.InventoryService;
import com.bonsaimarket.backend.marketplace.ManagementPage;
import com.bonsaimarket.backend.marketplace.MarketplaceAccess;
import com.bonsaimarket.backend.marketplace.MarketplaceException;
import com.bonsaimarket.backend.marketplace.MarketplaceTransactions;
import com.bonsaimarket.backend.order.Order;
import com.bonsaimarket.backend.order.OrderItem;
import com.bonsaimarket.backend.order.OrderStatus;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** Admin (12c) và seller (13b) dùng chung quản lý đơn. Seller chỉ xem đơn của gian hàng mình; quyền sở hữu lấy từ session. */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class OrderManagementService {
    private final EntityManager em;
    private final MarketplaceAccess access;
    private final InventoryService inventory;
    private final com.bonsaimarket.backend.shipping.ShipmentOrderGuard shipments;
    private final OrderStatusHistory history;
    private final com.bonsaimarket.backend.wallet.VndWallet wallet;

    public OrderManagementService(EntityManager em, MarketplaceAccess access, InventoryService inventory, com.bonsaimarket.backend.shipping.ShipmentOrderGuard shipments, OrderStatusHistory history,com.bonsaimarket.backend.wallet.VndWallet wallet) {
        this.em = em;
        this.access = access;
        this.inventory = inventory;
        this.shipments = shipments;
        this.history = history;
        this.wallet = wallet;
    }

    public record OrderRow(Long id, String code, String status, Long buyerId, String buyerName, String buyerEmail, Long storeId, String storeName,
                           BigDecimal subtotal, BigDecimal shippingFee, BigDecimal total, long itemCount, long quantity, String createdAt,PaymentSummary payment) { }
    public record PaymentSummary(String method,String status,boolean latestAttempt) {}

    public record Party(Long id, String name, String email, String phone) { }

    public record Line(Long id, Long productId, String name, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) { }

    public record OrderDetail(Long id, String code, String status, String createdAt, String updatedAt, String notes, String shippingMethod,
                              String shippingAddress, BigDecimal subtotal, BigDecimal shippingFee, BigDecimal total, Party buyer,
                              Long storeId, String storeName, List<Line> items, List<String> actions,
                              com.bonsaimarket.backend.payment.OrderPayment payment,
                              List<ActionBlocker> actionBlockers, List<OrderStatusHistory.Entry> history) { }

    public record ActionBlocker(String status, String code, String message) {}
    public record Availability(List<String> actions, List<ActionBlocker> blockers) {}

    @Transactional(readOnly = true)
    public ManagementPage<OrderRow> list(Long actorId, boolean admin, int page, String search, String status, String from, String to) {
        actor(actorId, admin);
        return listFor(actorId, admin, false, page, search, status, from, to);
    }

    @Transactional(readOnly = true)
    public ManagementPage<OrderRow> buyerList(Long actorId, int page, String search, String status, String from, String to) {
        access.actor(actorId);
        return listFor(actorId, false, true, page, search, status, from, to);
    }

    private ManagementPage<OrderRow> listFor(Long actorId, boolean admin, boolean buyer, int page, String search, String status, String from, String to) {
        MarketplaceAccess.page(page);
        StringBuilder where = new StringBuilder(" where o.id>0");
        Map<String, Object> params = new LinkedHashMap<>();
        if (buyer) {
            where.append(" and b.id=:owner");
            params.put("owner", actorId);
        } else if (!admin) {
            where.append(" and s.owner.id=:owner");
            params.put("owner", actorId);
        }
        OrderStatus wanted = ManagementSupport.status(status);
        if (wanted != null) {
            where.append(" and o.status=:status");
            params.put("status", wanted);
        }
        String like = ManagementSupport.like(search);
        if (like != null) {
            where.append(" and (lower(o.code) like :q or lower(b.name) like :q or lower(b.email) like :q)");
            params.put("q", like);
        }
        LocalDate fromDay = ManagementSupport.date(from);
        LocalDate toDay = ManagementSupport.date(to);
        if (fromDay != null && toDay != null && fromDay.isAfter(toDay)) throw MarketplaceException.invalid("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
        if (fromDay != null) {
            where.append(" and o.createdAt>=:start");
            params.put("start", ManagementSupport.startOf(fromDay));
        }
        if (toDay != null) {
            where.append(" and o.createdAt<:end");
            params.put("end", ManagementSupport.endOf(toDay));
        }
        String source = " from Order o join o.buyer b join o.store s" + where;
        var count = em.createQuery("select count(o)" + source, Long.class);
        var rows = em.createQuery("select o.id, o.code, o.status, b.id, b.name, b.email, s.id, s.name, o.subtotal, o.shippingFee, o.total, o.createdAt"
            + source + " order by o.createdAt desc, o.id desc", Object[].class);
        params.forEach((key, value) -> {
            count.setParameter(key, value);
            rows.setParameter(key, value);
        });
        long total = count.getSingleResult();
        rows.setFirstResult(page * ManagementSupport.PAGE_SIZE).setMaxResults(ManagementSupport.PAGE_SIZE);
        List<Object[]> found = rows.getResultList();

        Map<Long, long[]> totals = new HashMap<>();
        Map<Long,PaymentSummary> paymentSummaries=new HashMap<>();
        if (!found.isEmpty()) {
            List<Long> ids = new ArrayList<>();
            for (Object[] r : found) ids.add(((Number) r[0]).longValue());
            if(admin){
                for(var p:em.createQuery("select p from OrderPayment p where p.orderId in :ids",com.bonsaimarket.backend.payment.OrderPayment.class).setParameter("ids",ids).getResultList())paymentSummaries.put(p.getOrderId(),new PaymentSummary(p.getMethod(),p.getStatus(),false));
                for(Object[] t:em.createQuery("select t.order.id,t.paymentMethod,t.status from Transaction t where t.order.id in :ids order by t.id desc",Object[].class).setParameter("ids",ids).getResultList())paymentSummaries.putIfAbsent(((Number)t[0]).longValue(),new PaymentSummary(String.valueOf(t[1]),String.valueOf(t[2]),true));
            }
            for (Object[] r : em.createQuery("select i.order.id, count(i), sum(i.quantity) from OrderItem i where i.order.id in :ids group by i.order.id", Object[].class)
                .setParameter("ids", ids).getResultList()) {
                totals.put(((Number) r[0]).longValue(), new long[]{((Number) r[1]).longValue(), r[2] == null ? 0 : ((Number) r[2]).longValue()});
            }
        }
        List<OrderRow> content = new ArrayList<>();
        for (Object[] r : found) {
            Long id = ((Number) r[0]).longValue();
            long[] sums = totals.getOrDefault(id, new long[]{0, 0});
            content.add(new OrderRow(id, String.valueOf(r[1]), ((OrderStatus) r[2]).name(), ((Number) r[3]).longValue(), String.valueOf(r[4]),
                admin ? String.valueOf(r[5]) : null, ((Number) r[6]).longValue(), String.valueOf(r[7]), money(r[8]), money(r[9]), money(r[10]),
                sums[0], sums[1], ManagementSupport.iso((java.time.LocalDateTime) r[11]),paymentSummaries.get(id)));
        }
        int pages = (int) ((total + ManagementSupport.PAGE_SIZE - 1) / ManagementSupport.PAGE_SIZE);
        return new ManagementPage<>(content, page, ManagementSupport.PAGE_SIZE, total, pages);
    }

    @Transactional(readOnly = true)
    public OrderDetail detail(Long actorId, boolean admin, Long id) {
        actor(actorId, admin);
        Order order = em.find(Order.class, id);
        if (order == null || !visible(order, actorId, admin)) throw MarketplaceException.missing();
        return detailOf(order, admin);
    }

    public OrderDetail transition(Long actorId, boolean admin, Long id, String target) {
        return transition(actorId,admin,id,target,null);
    }
    public OrderDetail transition(Long actorId, boolean admin, Long id, String target, String reason) {
        MarketplaceTransactions.requireReadCommitted(em);
        actor(actorId, admin);
        OrderStatus next = ManagementSupport.status(target);
        if (next == null) throw MarketplaceException.invalid("Chọn trạng thái mới cho đơn hàng.");
        Order order = access.lock(Order.class, id);
        if (!visible(order, actorId, admin)) throw MarketplaceException.missing();
        if(next==OrderStatus.CANCELLED) history.cancellationReason(actorId,order,reason);
        return applyTransition(actorId, order, next, allowed(order.getStatus(), admin), admin);
    }

    @Transactional(readOnly = true)
    public OrderDetail buyerDetail(Long actorId, Long id) {
        access.actor(actorId);
        Order order=em.find(Order.class,id);
        if(order==null || !order.getBuyer().getId().equals(actorId)) throw MarketplaceException.missing();
        return buyerDetailOf(order);
    }

    public OrderDetail cancelByBuyer(Long actorId, Long id) {
        return cancelByBuyer(actorId,id,null);
    }
    public OrderDetail cancelByBuyer(Long actorId, Long id, String reason) {
        MarketplaceTransactions.requireReadCommitted(em);
        access.actor(actorId);
        Order order=access.lock(Order.class,id);
        if(!order.getBuyer().getId().equals(actorId)) throw MarketplaceException.missing();
        history.cancellationReason(actorId,order,reason);
        applyTransition(actorId, order, OrderStatus.CANCELLED, buyerAllowed(order), false);
        return buyerDetailOf(order);
    }

    private List<OrderStatus> buyerAllowed(Order order) {
        return order.getStatus()==OrderStatus.PENDING || order.getStatus()==OrderStatus.CONFIRMED ? List.of(OrderStatus.CANCELLED) : List.of();
    }

    private OrderDetail buyerDetailOf(Order order) {
        return detailOf(order,false,buyerAllowed(order));
    }

    @Transactional(readOnly = true)
    public Availability buyerAvailability(Long actorId, Long id) {
        access.actor(actorId);
        Order order = em.find(Order.class, id);
        if (order == null || !order.getBuyer().getId().equals(actorId)) throw MarketplaceException.missing();
        return availability(order, buyerAllowed(order));
    }

    private MarketplaceException restriction(Order order, OrderStatus next, boolean lock) {
        long foreignLines = em.createQuery("select count(i) from OrderItem i where i.order.id=:id and i.product.store.id<>:store", Long.class)
            .setParameter("id", order.getId()).setParameter("store", order.getStore().getId()).getSingleResult();
        if (foreignLines > 0) return new MarketplaceException(409, "ORDER_STORE_CONFLICT", "Đơn có sản phẩm thuộc gian hàng khác. Cần đối chiếu trước khi xử lý.");
        if (next == OrderStatus.CANCELLED) {
            long paid = em.createQuery("select count(t) from Transaction t where t.order.id=:id and t.status=:status", Long.class)
                .setParameter("id", order.getId()).setParameter("status", com.bonsaimarket.backend.payment.TransactionStatus.SUCCESS).getSingleResult();
            if (paid > 0) return new MarketplaceException(409, "REFUND_REVIEW_REQUIRED", "Đơn có giao dịch thành công; cần đối chiếu hoàn tiền trước khi hủy.");
            var cod=em.find(com.bonsaimarket.backend.payment.OrderPayment.class,order.getId());
            boolean refundableSandbox=cod!=null&&"WALLET_SANDBOX".equals(cod.getMethod())&&"SANDBOX_PAID".equals(cod.getStatus())&&wallet.refundable(order);
            if(cod!=null&&!refundableSandbox&&(!java.util.Set.of("AWAITING_COLLECTION","CANCELLED").contains(cod.getStatus())
                ||!java.util.Set.of("REQUESTED","CANCELLED").contains(cod.getCollectionStatus())
                ||cod.getCollectedAmount()!=null||cod.getSettledAmount()!=null
                ||!"UNKNOWN".equals(cod.getSettlementStatus())))
                return new MarketplaceException(409,"REFUND_REVIEW_REQUIRED","COD có bằng chứng thu tiền hoặc cần đối chiếu; không hủy yêu cầu thu tự động.");
            var shipmentBlocker = shipments.cancellationRestriction(order.getId());
            if (shipmentBlocker != null) return shipmentBlocker;
        }
        return shipments.progressRestriction(order, next, lock);
    }

    private Availability availability(Order order, List<OrderStatus> permitted) {
        List<String> actions = new ArrayList<>();
        List<ActionBlocker> blockers = new ArrayList<>();
        for (OrderStatus target : permitted) {
            var blocked = restriction(order, target, false);
            if (blocked == null) actions.add(target.name());
            else blockers.add(new ActionBlocker(target.name(), blocked.code, blocked.getMessage()));
        }
        return new Availability(actions, blockers);
    }

    private OrderDetail applyTransition(Long actorId, Order order, OrderStatus next, List<OrderStatus> permitted, boolean admin) {
        Long id=order.getId();
        if (!permitted.contains(next)) {
            throw new MarketplaceException(409, "ORDER_STATE_INVALID", "Không thể chuyển đơn hàng sang trạng thái này. Tải lại để xem trạng thái mới nhất.");
        }
        var blocked = restriction(order, next, true);
        if (blocked != null) throw blocked;
        OrderStatus previous = order.getStatus();
        order.setStatus(next);
        order.setUpdatedAt(access.now());
        // Tồn kho cập nhật cùng giao dịch đơn: xác nhận trừ tồn, hủy trả tồn; cả hai đều chống lặp.
        if (next == OrderStatus.CONFIRMED) inventory.debitConfirmedOrder(id);
        if (next == OrderStatus.CANCELLED) inventory.restoreCancelledOrder(id);
        if (next == OrderStatus.CANCELLED) {
            var intent=em.find(com.bonsaimarket.backend.payment.OrderPayment.class,id);
            if(intent!=null){if("WALLET_SANDBOX".equals(intent.getMethod())){wallet.refundCancelled(order);intent.setStatus("SANDBOX_REFUNDED");intent.setSettlementStatus("SANDBOX_REFUNDED");}else{intent.setStatus("CANCELLED");intent.setCollectionStatus("CANCELLED");}}
        }
        history.record(actorId, order, previous);
        com.bonsaimarket.backend.notification.OrderNotifications.enqueue(em,access,order,
            next == OrderStatus.DELIVERED ? "ORDER_DELIVERED" : next.name());
        em.flush();
        return detailOf(order, admin);
    }

    static List<OrderStatus> allowed(OrderStatus current, boolean admin) {
        if (current == null) return List.of();
        return switch (current) {
            case PENDING -> List.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED);
            case CONFIRMED -> List.of(OrderStatus.SHIPPING, OrderStatus.CANCELLED);
            case SHIPPING -> List.of(OrderStatus.DELIVERED);
            case DELIVERED -> admin ? List.of(OrderStatus.COMPLETED) : List.of();
            default -> List.of();
        };
    }

    private void actor(Long actorId, boolean admin) {
        if (admin) access.admin(actorId);
        else access.seller(actorId);
    }

    private boolean visible(Order order, Long actorId, boolean admin) {
        if (admin) return true;
        var owner = order.getStore() == null ? null : order.getStore().getOwner();
        return owner != null && actorId.equals(owner.getId());
    }

    private OrderDetail detailOf(Order order, boolean admin) {
        return detailOf(order,admin,allowed(order.getStatus(),admin));
    }

    private OrderDetail detailOf(Order order, boolean admin, List<OrderStatus> permitted) {
        List<Line> lines = new ArrayList<>();
        for (OrderItem item : em.createQuery("select i from OrderItem i where i.order.id=:id order by i.id", OrderItem.class)
            .setParameter("id", order.getId()).getResultList()) {
            lines.add(new Line(item.getId(), item.getProduct() == null ? null : item.getProduct().getId(), item.getProductNameSnapshot(),
                item.getUnitPrice(), item.getQuantity() == null ? 0 : item.getQuantity(), item.getLineTotal()));
        }
        var buyer = order.getBuyer();
        // Seller chỉ thấy tên người mua; thông tin liên hệ giao hàng nằm trong snapshot địa chỉ.
        Party party = buyer == null ? null : new Party(buyer.getId(), buyer.getName(), admin ? buyer.getEmail() : null, admin ? buyer.getPhone() : null);
        var available = availability(order, permitted);
        return new OrderDetail(order.getId(), order.getCode(), order.getStatus().name(), ManagementSupport.iso(order.getCreatedAt()),
            ManagementSupport.iso(order.getUpdatedAt()), order.getNotes(), order.getShippingMethod(), order.getShippingAddressSnapshot(),
            order.getSubtotal(), order.getShippingFee(), order.getTotal(), party, order.getStore().getId(),
            em.find(com.bonsaimarket.backend.payment.OrderPayment.class,order.getId())==null?order.getStore().getName():em.find(com.bonsaimarket.backend.payment.OrderPayment.class,order.getId()).getStoreNameSnapshot(),
            lines, available.actions(),em.find(com.bonsaimarket.backend.payment.OrderPayment.class,order.getId()),
            available.blockers(), history.list(order.getId(), admin));
    }

    private static BigDecimal money(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }
}
