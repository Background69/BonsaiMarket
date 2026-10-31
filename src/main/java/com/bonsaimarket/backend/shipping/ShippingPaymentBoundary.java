package com.bonsaimarket.backend.shipping;

import com.bonsaimarket.backend.order.Order;
import com.bonsaimarket.backend.marketplace.MarketplaceException;
import org.springframework.stereotype.Component;

/** Chỉ mở khi Payment cấp quyền xác thực và được tuần tự hóa. Cấu hình, trạng thái đơn hay cờ paid từ client không thay thế được. */
@Component
public class ShippingPaymentBoundary {
    private final com.bonsaimarket.backend.payment.PaymentReadinessService readiness;
    public ShippingPaymentBoundary(){this.readiness=null;}
    @org.springframework.beans.factory.annotation.Autowired
    public ShippingPaymentBoundary(com.bonsaimarket.backend.payment.PaymentReadinessService readiness){this.readiness=readiness;}
    public record Authorization(int feePayer,int codAmount) {}
    public Authorization authorize(Order order,ShippingQuote quote) {
        if(readiness!=null)return readiness.authorize(order,quote);
        throw new MarketplaceException(409,"PAYMENT_CONTRACT_REQUIRED","Chưa có hợp đồng xác nhận thanh toán/COD. Đã chặn tạo vận đơn; thông tin chuẩn bị giao vẫn được lưu.");
    }
}
