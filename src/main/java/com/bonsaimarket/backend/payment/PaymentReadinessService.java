package com.bonsaimarket.backend.payment;
import com.bonsaimarket.backend.order.*;
import com.bonsaimarket.backend.order.Order;
import com.bonsaimarket.backend.shipping.*;
import com.bonsaimarket.backend.marketplace.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

/** Gọi khi đang giữ khóa đơn để kiểm tra quyền tạo vận đơn. */
@Service
public class PaymentReadinessService {
    private final EntityManager em;private final ShippingSettings settings;
    public PaymentReadinessService(EntityManager em,ShippingSettings settings){this.em=em;this.settings=settings;}
    public OrderPayment view(Order o){return em.find(OrderPayment.class,o.getId());}
    public ShippingPaymentBoundary.Authorization authorize(Order o,ShippingQuote q){
        var p=view(o);
        if(p==null)throw new MarketplaceException(409,"PAYMENT_CONTRACT_REQUIRED","Đơn cũ chưa có hồ sơ thanh toán xác thực.");
        if(!"COD".equals(p.getMethod())||!"AWAITING_COLLECTION".equals(p.getStatus())||!"REQUESTED".equals(p.getCollectionStatus())||!"UNKNOWN".equals(p.getSettlementStatus()))throw new MarketplaceException(409,"PAYMENT_NOT_READY","Thanh toán cần đối chiếu trước khi giao.");
        if(!settings.getCodEnabledStoreIds().contains(o.getStore().getId()))throw new MarketplaceException(409,"COD_ACCOUNT_REQUIRED","Platform chưa xác nhận tài khoản GHN của gian hàng được thu hộ COD staging.");
        if(!p.getQuoteId().equals(q.getId())||p.getFeePayer()!=2||!"VND".equals(p.getCurrency())||p.getCodAmount().compareTo(o.getSubtotal())!=0||p.getShippingFee().compareTo(q.getFee())!=0||p.getAmountDue().compareTo(o.getTotal())!=0||o.getSubtotal().add(q.getFee()).compareTo(o.getTotal())!=0)
            throw new MarketplaceException(409,"PAYMENT_AMOUNT_MISMATCH","COD, phí giao nhận hoặc tổng tiền chưa khớp snapshot đã xác nhận.");
        try{return new ShippingPaymentBoundary.Authorization(2,p.getCodAmount().intValueExact());}catch(ArithmeticException e){throw MarketplaceException.invalid("COD phải là số nguyên VND.");}
    }
}
