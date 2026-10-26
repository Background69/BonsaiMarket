package com.bonsaimarket.backend.shipping;

import com.bonsaimarket.backend.marketplace.MarketplaceException;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

@Component
public class ShipmentOrderGuard {
    private final EntityManager em;
    public ShipmentOrderGuard(EntityManager em){this.em=em;}
    /** Caller holds the same Order write lock as shipment reservation. No carrier I/O or stock changes here. */
    @Transactional(propagation=Propagation.MANDATORY)
    public void beforeCancellation(Long orderId){
        var states=em.createQuery("select s.state from Shipment s where s.orderId=:id",ShipmentState.class).setParameter("id",orderId).getResultList();
        if(states.stream().anyMatch(s->s!=ShipmentState.CANCELLED))throw new MarketplaceException(409,"SHIPMENT_REQUIRES_RECONCILIATION","Đã có yêu cầu vận chuyển. Cần GHN xác nhận hủy trước lấy hàng; sau bàn giao dùng quy trình hoàn hàng riêng.");
    }
}
