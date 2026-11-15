package com.bonsaimarket.backend.notification;
import com.bonsaimarket.backend.marketplace.MarketplaceAccess;
import com.bonsaimarket.backend.order.Order;
import jakarta.persistence.EntityManager;
import java.util.*;

/** Outbox nằm trong giao dịch; không gửi SMTP trong giao dịch nghiệp vụ. */
public final class OrderNotifications {
    private OrderNotifications(){}
    public static void enqueue(EntityManager em,MarketplaceAccess access,Order order,String event){
        String title=switch(event){case "PLACED"->"Đã nhận đơn đặt hàng";case "CONFIRMED"->"Gian hàng đã xác nhận đơn";case "SHIPPING"->"Đơn hàng được cập nhật đang giao";case "ORDER_DELIVERED"->"Đơn hàng được xác nhận đã giao";case "COMPLETED"->"Đơn hàng đã hoàn tất";case "CANCELLED"->"Đơn hàng đã hủy";case "SHIPMENT_CREATED"->"Vận đơn đã được tạo";case "PICKED_UP"->"Hãng vận chuyển đã nhận hàng";case "DELIVERED"->"Hãng vận chuyển báo giao thành công";case "RETURN_REQUESTED"->"Đã nhận yêu cầu trả hàng";case "RETURN_APPROVED"->"Gian hàng đã duyệt yêu cầu trả";case "RETURN_INSPECTED"->"Gian hàng đã kiểm tra hàng trả";default->throw new IllegalArgumentException("Unknown notification event");};
        String key="ORDER:"+order.getId()+":"+event;
        NotificationOutbox.enqueue(em,access,order.getId(),event,order.getBuyer().getEmail(),key,"BonsaiMarket — "+title,
            title+".\nMã đơn: "+order.getCode()+"\nMở BonsaiMarket, mục Đơn mua để xem thông tin mới nhất.\nTrạng thái giao hàng không xác nhận đã thu COD, quyết toán hoặc hoàn tiền.\nBonsaiMarket");
        if(event.equals("CANCELLED")&&!order.getBuyer().getId().equals(order.getStore().getOwner().getId()))
            NotificationOutbox.enqueue(em,access,order.getId(),event,order.getStore().getOwner().getEmail(),key+":SELLER:"+order.getStore().getOwner().getId(),"BonsaiMarket — Đơn hàng đã hủy",
                "Đơn "+order.getCode()+" đã hủy. Xem Kênh người bán > Đơn hàng để đối chiếu lịch sử. Hủy đơn không xác nhận hoàn tiền.");
    }
}
