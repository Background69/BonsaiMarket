package com.bonsaimarket.backend.shipping;

import com.bonsaimarket.backend.marketplace.MarketplaceException;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

@Component
public class ShipmentOrderGuard {
    private final EntityManager em;
    public ShipmentOrderGuard(EntityManager em){this.em=em;}
    /** Bên gọi giữ cùng khóa ghi Order như lúc tạo vận đơn; không gọi hãng hoặc sửa tồn ở đây. */
    @Transactional(propagation=Propagation.MANDATORY)
    public void beforeCancellation(Long orderId){
        var states=em.createQuery("select s.state from Shipment s where s.orderId=:id",ShipmentState.class).setParameter("id",orderId).getResultList();
        if(states.stream().anyMatch(s->s!=ShipmentState.CANCELLED))throw new MarketplaceException(409,"SHIPMENT_REQUIRES_RECONCILIATION","Đã có yêu cầu vận chuyển. Cần GHN xác nhận hủy trước lấy hàng; sau bàn giao dùng quy trình hoàn hàng riêng.");
    }
}
