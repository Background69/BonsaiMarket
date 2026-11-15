package com.bonsaimarket.backend.shipping;

import com.bonsaimarket.backend.marketplace.MarketplaceException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import com.bonsaimarket.backend.order.Order;
import com.bonsaimarket.backend.order.OrderStatus;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

@Component
public class ShipmentOrderGuard {
    private final EntityManager em;
    public ShipmentOrderGuard(EntityManager em){this.em=em;}
    /** Bên gọi giữ cùng khóa ghi Order như lúc tạo vận đơn; không gọi hãng hoặc sửa tồn ở đây. */
    @Transactional(propagation=Propagation.MANDATORY)
    public void beforeCancellation(Long orderId){
        var blocked = cancellationRestriction(orderId);
        if (blocked != null) throw blocked;
    }

    public MarketplaceException cancellationRestriction(Long orderId) {
        var states=em.createQuery("select s.state from Shipment s where s.orderId=:id",ShipmentState.class).setParameter("id",orderId).getResultList();
        if(states.stream().anyMatch(s->s!=ShipmentState.CANCELLED)) return new MarketplaceException(409,"SHIPMENT_REQUIRES_RECONCILIATION","Đã có yêu cầu vận chuyển. Cần GHN xác nhận hủy trước lấy hàng; sau bàn giao dùng quy trình hoàn hàng riêng.");
        return null;
    }

    /** Read-only availability and the locked mutation use the same carrier evidence. */
    public MarketplaceException progressRestriction(Order order, OrderStatus target, boolean lock) {
        if (!Set.of(OrderStatus.SHIPPING, OrderStatus.DELIVERED, OrderStatus.COMPLETED).contains(target)) return null;
        var query = em.createQuery("select s from Shipment s where s.orderId=:id", Shipment.class)
            .setParameter("id", order.getId());
        var shipment = query.getResultStream().findFirst().orElse(null);
        boolean carrierBound = shipment != null || "GHN_STAGING".equals(order.getShippingMethod())
            || em.find(com.bonsaimarket.backend.payment.OrderPayment.class, order.getId()) != null;
        // Existing STANDARD orders without a carrier retain their manual lifecycle.
        if (!carrierBound) return null;
        if (shipment == null) return new MarketplaceException(409, "SHIPMENT_REQUIRED", "Đơn GHN chưa có vận đơn. Tạo và đối chiếu vận đơn trước khi cập nhật trạng thái.");
        if (lock) em.refresh(shipment, LockModeType.PESSIMISTIC_WRITE);
        boolean outbound = Set.of(ShipmentState.PICKED_UP, ShipmentState.IN_TRANSIT,
            ShipmentState.OUT_FOR_DELIVERY, ShipmentState.DELIVERY_FAILED, ShipmentState.DELIVERED).contains(shipment.getState());
        boolean verified = shipment.getLastEventAt() != null && shipment.getTrackingCode() != null;
        if (verified && (target == OrderStatus.SHIPPING ? outbound : shipment.getState() == ShipmentState.DELIVERED)) return null;
        return new MarketplaceException(409, "SHIPMENT_PROGRESS_REQUIRED", target == OrderStatus.SHIPPING
            ? "Chưa có xác nhận hãng đã nhận hàng. Đối chiếu vận đơn trước khi chuyển đơn sang đang giao."
            : "Chưa có xác nhận giao thành công từ hãng. Đối chiếu vận đơn trước khi xác nhận đã giao hoặc hoàn tất.");
    }
}
