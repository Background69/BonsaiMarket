package com.bonsaimarket.backend.returns;
import com.bonsaimarket.backend.marketplace.*;
import com.bonsaimarket.backend.order.*;
import com.bonsaimarket.backend.order.Order;
import com.bonsaimarket.backend.shipping.*;
import com.bonsaimarket.backend.inventory.InventoryService;
import com.bonsaimarket.backend.notification.OrderNotifications;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;

/** Yêu cầu trả toàn bộ đơn; lấy hàng trả và hoàn tiền vẫn xử lý bên ngoài khi chưa có tích hợp. */
@Service
public class OrderReturnService {
    public record Request(@NotBlank @Size(min=10,max=1000) String reason) {}
    public record Inspection(@AssertTrue(message="Xác nhận đã thực nhận toàn bộ hàng.") boolean received,
        @NotBlank @Size(min=10,max=1000) String condition,boolean restock) {}
    public record Overview(OrderReturn request,String refundStatus,String reverseShippingStatus,String shipmentState) {}
    private record CarrierWork(Long orderId,int shop,String tracking) {}
    private final EntityManager em;private final MarketplaceAccess access;private final InventoryService inventory;private final ShippingSettings settings;private final ShippingProvider provider;private final TransactionTemplate tx;
    public OrderReturnService(EntityManager em,MarketplaceAccess access,InventoryService inventory,ShippingSettings settings,ShippingProvider provider,PlatformTransactionManager manager){this.em=em;this.access=access;this.inventory=inventory;this.settings=settings;this.provider=provider;tx=new TransactionTemplate(manager);tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);}
    private <T>T db(Supplier<T> action){return tx.execute(s->action.get());}
    private Order order(Long actor,Long id,String scope){
        if(scope.equals("admin"))access.admin(actor);else if(scope.equals("seller"))access.seller(actor);else access.actor(actor);
        var o=access.lock(Order.class,id);if(scope.equals("account")&&!o.getBuyer().getId().equals(actor)||scope.equals("seller")&&!o.getStore().getOwner().getId().equals(actor))throw MarketplaceException.missing();return o;
    }
    private Shipment shipment(Long id){return em.createQuery("select s from Shipment s where s.orderId=:id",Shipment.class).setParameter("id",id).getResultStream().findFirst().orElse(null);}
    public Overview overview(Long actor,Long id,String scope){return db(()->{order(actor,id,scope);return view(id);});}
    private Overview view(Long id){var s=shipment(id);return new Overview(em.find(OrderReturn.class,id),"EXTERNAL_INTEGRATION_REQUIRED","BUYER_REVERSE_WAYBILL_NOT_INTEGRATED",s==null?null:s.getState().name());}
    private OrderReturn fresh(Order o,String kind,String reason){var r=new OrderReturn();r.setOrderId(o.getId());r.setKind(kind);r.setStatus("REQUESTED");r.setReason(reason.strip());r.setInspection("");r.setCarrierRequestStatus("NOT_REQUESTED");r.setRequestedAt(access.now());r.setUpdatedAt(access.now());return r;}
    public Overview request(Long actor,Long id,Request b){return db(()->{
        var o=order(actor,id,"account");if(o.getStatus()!=OrderStatus.DELIVERED&&o.getStatus()!=OrderStatus.COMPLETED)throw conflict("RETURN_NOT_ELIGIBLE","Chỉ yêu cầu trả sau khi đơn đã giao; hủy trước giao dùng quy trình hủy đơn.");
        var previous=em.find(OrderReturn.class,id);if(previous!=null){if(!previous.getReason().equals(b.reason().strip())||!previous.getKind().equals("BUYER_RETURN"))throw conflict("RETURN_EXISTS","Đã có yêu cầu trả khác cho đơn này.");return view(id);}
        var r=fresh(o,"BUYER_RETURN",b.reason());em.persist(r);OrderNotifications.enqueue(em,access,o,"RETURN_REQUESTED");em.flush();return view(id);
    });}
    public Overview approve(Long actor,Long id){return db(()->{
        var o=order(actor,id,"seller");var r=em.find(OrderReturn.class,id);if(r==null)throw MarketplaceException.missing();if(!r.getKind().equals("BUYER_RETURN"))throw conflict("RETURN_KIND_INVALID","Đây là hoàn hàng của hãng vận chuyển.");
        if(r.getStatus().equals("REQUESTED")){r.setStatus("APPROVED");r.setApprovedAt(access.now());r.setUpdatedAt(access.now());OrderNotifications.enqueue(em,access,o,"RETURN_APPROVED");}return view(id);
    });}
    public Overview inspect(Long actor,Long id,Inspection b){return db(()->{
        var o=order(actor,id,"seller");if(!b.received())throw conflict("RETURN_RECEIPT_REQUIRED","Cần xác nhận thực nhận toàn bộ hàng trước khi kiểm tra.");
        var s=shipment(id);var r=em.find(OrderReturn.class,id);
        if(r==null&&s!=null&&s.getState()==ShipmentState.RETURNED){r=fresh(o,"CARRIER_RETURN","GHN đã báo hoàn về người gửi");r.setStatus("APPROVED");r.setApprovedAt(access.now());r.setCarrierRequestStatus("PROVIDER_REPORTED");em.persist(r);}
        if(r==null)throw MarketplaceException.missing();
        if(r.getStatus().equals("INSPECTED")){if(!r.getInspection().equals(b.condition().strip())||r.isRestock()!=b.restock())throw conflict("RETURN_ALREADY_INSPECTED","Kết quả kiểm tra đã được ghi nhận; không ghi đè tồn kho.");return view(id);}
        if(!r.getStatus().equals("APPROVED")||r.getKind().equals("CARRIER_RETURN")&&(s==null||s.getState()!=ShipmentState.RETURNED))throw conflict("RETURN_NOT_RECEIVED","Cần yêu cầu được duyệt; hoàn về từ GHN phải có trạng thái đã hoàn trước khi xác nhận nhận và kiểm tra.");
        r.setStatus("INSPECTED");r.setReceivedAt(access.now());r.setInspectedAt(access.now());r.setInspection(b.condition().strip());r.setRestock(b.restock());r.setUpdatedAt(access.now());
        if(b.restock())inventory.restoreInspectedReturn(id);OrderNotifications.enqueue(em,access,o,"RETURN_INSPECTED");em.flush();return view(id);
    });}
    public Overview carrierReturn(Long actor,Long id){
        var work=db(()->{
            var o=order(actor,id,"seller");var s=shipment(id);if(s==null||s.getTrackingCode()==null||!Set.of(ShipmentState.DELIVERY_FAILED,ShipmentState.RETURN_REQUESTED).contains(s.getState()))throw conflict("CARRIER_RETURN_UNSAFE","Chỉ yêu cầu hoàn cho vận đơn GHN báo giao thất bại hoặc chờ hoàn; không tạo reverse waybill sau giao thành công.");
            var r=em.find(OrderReturn.class,id);if(r!=null){if(!r.getKind().equals("CARRIER_RETURN"))throw conflict("RETURN_EXISTS","Đơn đã có yêu cầu trả của Buyer.");if(!r.getCarrierRequestStatus().equals("REJECTED"))throw conflict("CARRIER_RETURN_RECONCILE","Yêu cầu hoàn đã gửi hoặc chưa rõ phản hồi. Đối chiếu tracking, không gửi lặp.");}
            else{r=fresh(o,"CARRIER_RETURN","Yêu cầu GHN hoàn đơn giao thất bại");r.setStatus("APPROVED");r.setApprovedAt(access.now());em.persist(r);}
            r.setCarrierRequestStatus("SENDING");r.setUpdatedAt(access.now());em.flush();return new CarrierWork(id,settings.shop(o.getStore().getId()),s.getTrackingCode());
        });
        try{boolean accepted=provider.returnToSender(work.shop(),work.tracking());db(()->{order(actor,id,"seller");var r=em.find(OrderReturn.class,id);r.setCarrierRequestStatus(accepted?"ACKNOWLEDGED":"REJECTED");r.setUpdatedAt(access.now());return null;});if(!accepted)throw conflict("CARRIER_RETURN_REJECTED","GHN chưa chấp nhận yêu cầu hoàn. Không thay trạng thái hàng hoặc tồn kho.");}
        catch(RuntimeException e){db(()->{var r=em.find(OrderReturn.class,id);em.refresh(r,LockModeType.PESSIMISTIC_WRITE);if(r.getCarrierRequestStatus().equals("SENDING"))r.setCarrierRequestStatus("UNKNOWN");r.setUpdatedAt(access.now());return null;});throw e;}
        return overview(actor,id,"seller");
    }
    private static MarketplaceException conflict(String code,String message){return new MarketplaceException(409,code,message);}
}
