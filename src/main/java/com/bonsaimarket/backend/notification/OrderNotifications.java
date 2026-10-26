package com.bonsaimarket.backend.notification;
import com.bonsaimarket.backend.marketplace.MarketplaceAccess;
import com.bonsaimarket.backend.order.Order;
import jakarta.persistence.EntityManager;
import java.util.*;

/** Transactional outbox only: never performs SMTP inside the business transaction. */
public final class OrderNotifications {
    private OrderNotifications(){}
    public static void enqueue(EntityManager em,MarketplaceAccess access,Order order,String event){
        if(!em.isJoinedToTransaction())throw new IllegalStateException("Outbox requires the order transaction");
        String title=switch(event){case "PLACED"->"Đã nhận đơn đặt hàng";case "CONFIRMED"->"Gian hàng đã xác nhận đơn";case "CANCELLED"->"Đơn hàng đã hủy";case "SHIPMENT_CREATED"->"Vận đơn đã được tạo";case "PICKED_UP"->"Hãng vận chuyển đã nhận hàng";case "DELIVERED"->"Hãng vận chuyển báo giao thành công";case "RETURN_REQUESTED"->"Đã nhận yêu cầu trả hàng";case "RETURN_APPROVED"->"Gian hàng đã duyệt yêu cầu trả";case "RETURN_INSPECTED"->"Gian hàng đã kiểm tra hàng trả";default->throw new IllegalArgumentException("Unknown notification event");};
        String key="ORDER:"+order.getId()+":"+event;
        if(em.createQuery("select count(n) from OrderNotification n where n.dedupKey=:key",Long.class).setParameter("key",key).getSingleResult()>0)return;
        var n=new OrderNotification();n.setId(UUID.randomUUID().toString());n.setOrderId(order.getId());n.setEventType(event);n.setRecipient(order.getBuyer().getEmail());
        n.setSubject("BonsaiMarket — "+title);n.setBody(title+".\nMã đơn: "+order.getCode()+"\nMở BonsaiMarket, mục Đơn mua để xem thông tin mới nhất.\nTrạng thái giao hàng không xác nhận đã thu COD, quyết toán hoặc hoàn tiền.\nBonsaiMarket");
        n.setDedupKey(key);n.setStatus("PENDING");n.setAttempts(0);n.setCreatedAt(access.now());n.setUpdatedAt(access.now());n.setNextAttemptAt(access.now());em.persist(n);
    }
}
