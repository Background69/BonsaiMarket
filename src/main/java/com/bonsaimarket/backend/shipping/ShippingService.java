package com.bonsaimarket.backend.shipping;

import com.bonsaimarket.backend.marketplace.*;
import com.bonsaimarket.backend.order.*;
import com.bonsaimarket.backend.order.Order;
import com.bonsaimarket.backend.store.StoreLocations;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import static com.bonsaimarket.backend.shipping.ShippingProvider.*;

/** Short transactions reserve work under the existing order lock. Carrier I/O is always outside them. */
@Service
public class ShippingService {
    public enum Scope {BUYER,SELLER,ADMIN}
    public record ParcelWrite(@Min(1) @Max(19999) int weightGrams,@Min(1) @Max(200) int lengthCm,
        @Min(1) @Max(200) int widthCm,@Min(1) @Max(200) int heightCm,boolean carrierAcceptedGoods,@Min(0) long revision) {}
    public record QuoteView(String id,String provider,String environment,int serviceType,String serviceName,BigDecimal fee,String currency,
        LocalDateTime expiresAt,LocalDateTime selectedAt) {}
    public record EventView(ShipmentState state,LocalDateTime eventAt,LocalDateTime receivedAt,String source) {}
    public record Tracking(String provider,String environment,String trackingCode,ShipmentState state,LocalDateTime lastSyncAt,
        List<EventView> events) {}
    public record Overview(boolean sandbox,boolean canCreate,String createBlocker,ShippingParcel parcel,QuoteView selection,Tracking shipment,
        com.bonsaimarket.backend.payment.OrderPayment payment,boolean checkoutBound) {}
    private record Context(Long actor,Long orderId,int shopId,long revision,Parcel parcel,Destination destination,String fingerprint,int insurance) {}
    private record Reserved(String shipmentId,boolean created,Create request,int shopId,String pickupSnapshot) {}
    private record Sync(String id,int shopId,String tracking,String reference) {}
    private record Preflight(int shop,String pickup,String destination,String parcel,int service,int insurance,int cod,BigDecimal fee) {}
    private final EntityManager em;
    private final MarketplaceAccess access;
    private final StoreLocations stores;
    private final ShippingSettings settings;
    private final ShippingProvider provider;
    private final ShippingPaymentBoundary payment;
    private final ObjectMapper json;
    private final TransactionTemplate tx;
    public ShippingService(EntityManager em,MarketplaceAccess access,StoreLocations stores,ShippingSettings settings,
        ShippingProvider provider,ShippingPaymentBoundary payment,ObjectMapper json,PlatformTransactionManager manager){
        this.em=em;this.access=access;this.stores=stores;this.settings=settings;this.provider=provider;this.payment=payment;this.json=json;
        tx=new TransactionTemplate(manager);tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }
    private <T>T db(Supplier<T> work){return tx.execute(status->work.get());}
    private Shipment lockShipment(String id){var row=em.find(Shipment.class,id);if(row==null)throw MarketplaceException.missing();em.refresh(row,LockModeType.PESSIMISTIC_WRITE);return row;}
    private Order order(Long actor,Long id,Scope scope,boolean lock){
        if(scope==Scope.ADMIN)access.admin(actor);else if(scope==Scope.SELLER)access.seller(actor);else access.actor(actor);
        var o=lock?access.lock(Order.class,id):em.find(Order.class,id);
        if(o==null||scope==Scope.BUYER&&!o.getBuyer().getId().equals(actor)||scope==Scope.SELLER&&!o.getStore().getOwner().getId().equals(actor))throw MarketplaceException.missing();
        return o;
    }
    private void ready(Order o){
        if(o.getStatus()!=OrderStatus.PENDING&&o.getStatus()!=OrderStatus.CONFIRMED)throw conflict("ORDER_NOT_READY","Chỉ chuẩn bị vận chuyển cho đơn chờ xử lý hoặc đã xác nhận.");
        if(!stores.eligible(o.getStore()))throw conflict("SELLER_APPROVAL_REQUIRED","Gian hàng cần hồ sơ người bán đã được duyệt và đang hoạt động.");
    }
    public Overview overview(Long actor,Long id,Scope scope){return db(()->{
        var o=order(actor,id,scope,false);var shipment=shipment(id);var selected=selection(id);
        var intent=em.find(com.bonsaimarket.backend.payment.OrderPayment.class,id);
        String blocker="PAYMENT_CONTRACT_REQUIRED";
        try{ready(o);if(o.getStatus()!=OrderStatus.CONFIRMED)throw conflict("ORDER_NOT_READY","Chưa xác nhận đơn.");
            if(shipment!=null)throw conflict("SHIPMENT_EXISTS","Đã có vận đơn.");
            if(selected==null)throw conflict("SHIPPING_SELECTION_REQUIRED","Chưa chọn giao nhận.");
            validateQuote(o,selected);payment.authorize(o,selected);blocker=null;
        }catch(MarketplaceException e){blocker=e.code;}
        return new Overview(true,scope==Scope.SELLER&&blocker==null,blocker,em.find(ShippingParcel.class,id),selected==null?null:view(selected),tracking(shipment),intent,intent!=null);
    });}
    /** Joins the checkout transaction; carrier I/O already happened before order placement. */
    @org.springframework.transaction.annotation.Transactional(propagation=org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void attachCheckout(Order o,String quoteId,int shop,int service,String name,BigDecimal fee,String destination,String pickup,String parcel,LocalDateTime expires){
        var box=json.readValue(parcel,Parcel.class);var p=new ShippingParcel();p.setOrderId(o.getId());p.setWeightGrams(box.weightGrams());p.setLengthCm(box.lengthCm());p.setWidthCm(box.widthCm());p.setHeightCm(box.heightCm());p.setCarrierAcceptedGoods(true);p.setRevision(1);p.setUpdatedAt(access.now());em.persist(p);
        var q=new ShippingQuote();q.setId(quoteId);q.setOrderId(o.getId());q.setBuyerId(o.getBuyer().getId());q.setShopId(shop);q.setParcelRevision(1);q.setServiceType(service);q.setServiceName(name);q.setFee(fee);q.setDestinationSnapshot(destination);q.setPickupSnapshot(pickup);q.setParcelSnapshot(parcel);q.setOrderFingerprint(fingerprint(o));q.setExpiresAt(expires);q.setCreatedAt(access.now());q.setSelectedAt(access.now());em.persist(q);em.flush();
    }
    private boolean checkoutBound(Long id){return em.find(com.bonsaimarket.backend.payment.OrderPayment.class,id)!=null;}
    private void mutableSelection(Long id){if(checkoutBound(id))throw conflict("CHECKOUT_SELECTION_LOCKED","Phí, địa chỉ và kiện đã được xác nhận tại Checkout. Cần hủy và đặt lại nếu thay đổi.");}
    public ShippingParcel saveParcel(Long actor,Long id,ParcelWrite body){return db(()->{
        var o=order(actor,id,Scope.SELLER,true);ready(o);
        mutableSelection(id);
        if(shipment(id)!=null)throw conflict("SHIPMENT_EXISTS","Kiện hàng đã gắn với vận đơn. Không thể thay đổi snapshot.");
        var p=em.find(ShippingParcel.class,id);
        if(p==null){MarketplaceAccess.revision(0,body.revision());p=new ShippingParcel();p.setOrderId(id);}else MarketplaceAccess.revision(p.getRevision(),body.revision());
        p.setWeightGrams(body.weightGrams());p.setLengthCm(body.lengthCm());p.setWidthCm(body.widthCm());p.setHeightCm(body.heightCm());
        p.setCarrierAcceptedGoods(body.carrierAcceptedGoods());p.setRevision(p.getRevision()+1);p.setUpdatedAt(access.now());em.persist(p);
        for(var q:em.createQuery("select q from ShippingQuote q where q.orderId=:id and q.selectedAt is not null",ShippingQuote.class).setParameter("id",id).getResultList())q.setSelectedAt(null);
        em.flush();return p;
    });}
    public List<QuoteView> quotes(Long actor,Long id){
        var cached=db(()->{order(actor,id,Scope.BUYER,false);mutableSelection(id);return validQuotes(id);});
        if(!cached.isEmpty())return cached;
        var c=db(()->{
            var o=order(actor,id,Scope.BUYER,false);ready(o);var p=em.find(ShippingParcel.class,id);
            if(p==null)throw conflict("MISSING_PARCEL","Gian hàng chưa nhập khối lượng và kích thước kiện hàng đã đóng gói.");
            if(!p.isCarrierAcceptedGoods())throw conflict("GOODS_ACCEPTANCE_REQUIRED","Gian hàng cần đối chiếu chính sách nhận cây sống, hàng dễ vỡ và xác nhận loại hàng được hãng chấp nhận.");
            Destination destination;
            try{destination=json.readValue(o.getShippingAddressSnapshot(),Destination.class);}catch(RuntimeException e){throw conflict("ADDRESS_INVALID","Địa chỉ đơn cũ chưa có snapshot có cấu trúc. Cần xác minh với người mua trước khi báo phí.");}
            return new Context(actor,id,settings.shop(o.getStore().getId()),p.getRevision(),p.snapshot(),destination,fingerprint(o),insurance(o));
        });
        var pickup=provider.pickup(c.shopId());var destination=provider.validateAddress(c.shopId(),c.destination());
        var rates=provider.quote(c.shopId(),pickup,destination,c.parcel(),c.insurance());
        if(rates==null||rates.isEmpty()||rates.size()>5)throw conflict("UNSUPPORTED_ROUTE","Chưa có dịch vụ vận chuyển phù hợp.");
        return db(()->{
            var o=order(actor,id,Scope.BUYER,true);ready(o);var p=em.find(ShippingParcel.class,id);
            if(p==null||p.getRevision()!=c.revision()||!fingerprint(o).equals(c.fingerprint())||settings.shop(o.getStore().getId())!=c.shopId())throw conflict("QUOTE_CHANGED","Đơn hàng hoặc kiện hàng vừa thay đổi. Tính phí lại.");
            var existing=validQuotes(id);if(!existing.isEmpty())return existing;
            List<QuoteView> result=new ArrayList<>();
            for(var rate:rates){
                if(rate.serviceType()!=2||rate.fee()==null||rate.fee().signum()<0||rate.fee().scale()>2||rate.fee().compareTo(new BigDecimal("50000000"))>0||rate.serviceName()==null||rate.serviceName().length()>120)throw conflict("CARRIER_RESPONSE_INVALID","Báo phí GHN chưa hợp lệ.");
                var q=new ShippingQuote();q.setId(UUID.randomUUID().toString());q.setOrderId(id);q.setBuyerId(actor);q.setShopId(c.shopId());q.setParcelRevision(c.revision());
                q.setServiceType(rate.serviceType());q.setServiceName(rate.serviceName());q.setFee(rate.fee());q.setOrderFingerprint(c.fingerprint());
                q.setDestinationSnapshot(json.writeValueAsString(destination));q.setPickupSnapshot(json.writeValueAsString(pickup));q.setParcelSnapshot(json.writeValueAsString(c.parcel()));
                q.setCreatedAt(access.now());q.setExpiresAt(access.now().plusMinutes(10));em.persist(q);result.add(view(q));
            }
            em.flush();return result;
        });
    }
    private List<QuoteView> validQuotes(Long id){
        var o=em.find(Order.class,id);var p=em.find(ShippingParcel.class,id);
        if(o==null||p==null||!p.isCarrierAcceptedGoods()||o.getStatus()!=OrderStatus.PENDING&&o.getStatus()!=OrderStatus.CONFIRMED||!stores.eligible(o.getStore()))return List.of();
        int shop=settings.shop(o.getStore().getId());String fingerprint=fingerprint(o);
        return em.createQuery("select q from ShippingQuote q where q.orderId=:id and q.expiresAt>:now and q.parcelRevision=:revision and q.orderFingerprint=:fingerprint and q.shopId=:shop order by q.createdAt desc",ShippingQuote.class)
            .setParameter("id",id).setParameter("now",access.now()).setParameter("revision",p.getRevision()).setParameter("fingerprint",fingerprint).setParameter("shop",shop)
            .setMaxResults(5).getResultStream().map(this::view).toList();
    }
    public QuoteView select(Long actor,Long id,String quoteId){return db(()->{
        var o=order(actor,id,Scope.BUYER,true);ready(o);if(shipment(id)!=null)throw conflict("SHIPMENT_EXISTS","Đã có vận đơn cho đơn hàng này.");
        mutableSelection(id);
        var q=em.find(ShippingQuote.class,quoteId);validateQuote(o,q);
        for(var previous:em.createQuery("select q from ShippingQuote q where q.orderId=:id and q.selectedAt is not null",ShippingQuote.class).setParameter("id",id).getResultList())previous.setSelectedAt(null);
        q.setSelectedAt(access.now());em.flush();return view(q);
        // Staging selection only. Never silently replace existing order totals, fees, payment method or address.
    });}
    private void validateQuote(Order o,ShippingQuote q){
        if(q==null||!q.getOrderId().equals(o.getId())||!q.getBuyerId().equals(o.getBuyer().getId()))throw MarketplaceException.missing();
        var p=em.find(ShippingParcel.class,o.getId());
        if(!checkoutBound(o.getId())&&!q.getExpiresAt().isAfter(access.now()))throw conflict("QUOTE_EXPIRED","Báo phí đã hết hạn. Tính phí lại trước khi chọn dịch vụ.");
        if(p==null||!p.isCarrierAcceptedGoods()||q.getParcelRevision()!=p.getRevision()||!q.getOrderFingerprint().equals(fingerprint(o))||q.getShopId()!=settings.shop(o.getStore().getId()))throw conflict("QUOTE_CHANGED","Địa chỉ, kiện hàng hoặc cấu hình shop đã thay đổi. Tính phí lại.");
    }
    public Overview create(Long actor,Long id){
        var check=db(()->{
            var o=order(actor,id,Scope.SELLER,true);
            if(!checkoutBound(id)||shipment(id)!=null)return null;
            ready(o);if(o.getStatus()!=OrderStatus.CONFIRMED)throw conflict("ORDER_NOT_READY","Cần xác nhận đơn trước.");
            var q=selection(id);if(q==null)throw conflict("SHIPPING_SELECTION_REQUIRED","Thiếu dịch vụ đã xác nhận.");validateQuote(o,q);var a=payment.authorize(o,q);
            return new Preflight(q.getShopId(),q.getPickupSnapshot(),q.getDestinationSnapshot(),q.getParcelSnapshot(),q.getServiceType(),insurance(o),a.codAmount(),q.getFee());
        });
        if(check!=null){
            var pickup=provider.pickup(check.shop());
            if(!check.pickup().equals(json.writeValueAsString(pickup)))throw conflict("PICKUP_CHANGED","Kho GHN thay đổi. Cần đối chiếu hoặc hủy và đặt lại.");
            var rates=provider.quoteCod(check.shop(),pickup,json.readValue(check.destination(),Destination.class),json.readValue(check.parcel(),Parcel.class),check.insurance(),check.cod());
            if(rates==null||rates.stream().noneMatch(r->r.serviceType()==check.service()&&r.fee()!=null&&r.fee().compareTo(check.fee())==0))
                throw conflict("SHIPPING_PRICE_CHANGED","Phí GHN đã thay đổi. Không tạo vận đơn với tổng mới; cần hủy và Buyer xác nhận đơn mới.");
        }
        var reservation=db(()->{
            var o=order(actor,id,Scope.SELLER,true);ready(o);
            var existing=shipment(id);if(existing!=null)return new Reserved(existing.getId(),false,null,existing.getShopId(),null);
            if(o.getStatus()!=OrderStatus.CONFIRMED)throw conflict("ORDER_NOT_READY","Cần xác nhận đơn qua quy trình đơn hàng trước khi tạo vận đơn.");
            var q=selection(id);if(q==null)throw conflict("SHIPPING_SELECTION_REQUIRED","Người mua chưa xác nhận báo phí thử nghiệm.");validateQuote(o,q);
            var auth=payment.authorize(o,q); // Always closed until the Payment owner supplies the contract.
            if(auth==null||!(auth.feePayer()==1&&auth.codAmount()==0||auth.feePayer()==2&&auth.codAmount()>0&&auth.codAmount()<=50000000))throw conflict("PAYMENT_CONTRACT_REQUIRED","Hợp đồng thu hộ hoặc bên trả cước không hợp lệ.");
            if(q.getFee().compareTo(o.getShippingFee())!=0||o.getSubtotal().add(q.getFee()).compareTo(o.getTotal())!=0)throw conflict("ORDER_TOTAL_MISMATCH","Phí vận chuyển chưa khớp tổng tiền đã xác nhận. Không tự thay đổi giá của đơn hàng.");
            var s=new Shipment();s.setId(UUID.randomUUID().toString());s.setOrderId(id);s.setQuoteId(q.getId());s.setShopId(q.getShopId());
            s.setClientReference("BM-"+s.getId());s.setState(ShipmentState.CREATING);s.setCreatedAt(access.now());s.setUpdatedAt(access.now());em.persist(s);em.flush();
            var items=em.createQuery("select i from OrderItem i where i.order.id=:id order by i.id",OrderItem.class).setParameter("id",id).getResultStream()
                .map(i->new Item(i.getProductNameSnapshot(),i.getQuantity(),i.getUnitPrice().intValueExact())).toList();
            if(items.isEmpty()||items.size()>100)throw conflict("ORDER_ITEMS_INVALID","Đơn chưa có danh sách hàng hợp lệ.");
            return new Reserved(s.getId(),true,new Create(s.getClientReference(),json.readValue(q.getDestinationSnapshot(),Destination.class),json.readValue(q.getPickupSnapshot(),Pickup.class),json.readValue(q.getParcelSnapshot(),Parcel.class),q.getServiceType(),insurance(o),auth.feePayer(),auth.codAmount(),items),s.getShopId(),q.getPickupSnapshot());
        });
        if(!reservation.created()){
            var state=db(()->em.find(Shipment.class,reservation.shipmentId()).getState());
            if(state==ShipmentState.UNKNOWN)synchronize(actor,id,Scope.SELLER);
            return overview(actor,id,Scope.SELLER); // Never repeat create, including after process crash or timeout.
        }
        try{
            if(!reservation.pickupSnapshot().equals(json.writeValueAsString(provider.pickup(reservation.shopId()))))throw conflict("PICKUP_CHANGED","Kho GHN đã thay đổi sau báo phí. Cần đối chiếu trước khi giao.");
            var remote=provider.create(reservation.shopId(),reservation.request());
            db(()->{var s=lockShipment(reservation.shipmentId());validateRemote(s,remote);s.setTrackingCode(remote.trackingCode());if(s.getState()==ShipmentState.CREATING)s.setState(ShipmentState.UNKNOWN);s.setUpdatedAt(access.now());return null;});
        }catch(RuntimeException e){db(()->{var s=lockShipment(reservation.shipmentId());if(s.getState()==ShipmentState.CREATING)s.setState(ShipmentState.UNKNOWN);s.setUpdatedAt(access.now());return null;});throw e;}
        synchronize(actor,id,Scope.SELLER);return overview(actor,id,Scope.SELLER);
    }
    public void synchronize(Long actor,Long id,Scope scope){
        var sync=db(()->{
            order(actor,id,scope,true);var s=shipment(id);if(s==null)throw MarketplaceException.missing();
            if(s.getSyncAfter()!=null&&s.getSyncAfter().isAfter(access.now()))throw new MarketplaceException(429,"SYNC_THROTTLED","Đã yêu cầu đối chiếu gần đây. Thử lại sau 5 phút.");
            settings.shop(em.find(Order.class,id).getStore().getId());
            s.setSyncAfter(access.now().plusMinutes(5));return new Sync(s.getId(),s.getShopId(),s.getTrackingCode(),s.getClientReference());
        });
        try{
            var remote=provider.find(sync.shopId(),sync.tracking(),sync.reference());
            db(()->{var s=lockShipment(sync.id());validateRemote(s,remote);
                if(s.getTrackingCode()==null)s.setTrackingCode(remote.trackingCode());
                apply(s,remote.status(),remote.eventAt(),"POLL","switch_status");s.setLastSyncAt(access.now());return null;});
        }catch(RuntimeException e){db(()->{var s=lockShipment(sync.id());s.setSyncAfter(access.now().plusMinutes(15));return null;});throw e;}
    }
    public void cancel(Long actor,Long id){
        var sync=db(()->{order(actor,id,Scope.SELLER,true);var s=shipment(id);
            if(s==null)throw MarketplaceException.missing();
            if(s.getState()!=ShipmentState.WAITING_PICKUP||s.getTrackingCode()==null)throw conflict("SHIPMENT_CANCEL_UNSAFE","Chỉ hủy vận đơn đã xác minh chưa lấy hàng. Sau bàn giao cần quy trình hoàn hàng riêng.");
            settings.shop(em.find(Order.class,id).getStore().getId());s.setState(ShipmentState.UNKNOWN);s.setSyncAfter(null);return new Sync(s.getId(),s.getShopId(),s.getTrackingCode(),s.getClientReference());});
        if(!provider.cancel(sync.shopId(),sync.tracking()))throw conflict("CARRIER_CANCEL_REJECTED","GHN chưa xác nhận hủy. Cần đối chiếu vận đơn trước khi hủy đơn hàng.");
        synchronize(actor,id,Scope.SELLER); // Only an actual carrier status may release order cancellation.
    }
    public void webhook(int shopId,String code,String reference,String type,String status,Instant time){
        if(!Set.of("create","switch_status").contains(type))return; // COD/fee events cannot mutate shipping, orders or payment.
        db(()->{
            var found=em.createQuery("select s from Shipment s where s.trackingCode=:code or s.clientReference=:reference",Shipment.class).setParameter("code",code).setParameter("reference",reference).getResultList();
            if(found.size()!=1)throw MarketplaceException.missing();var s=lockShipment(found.getFirst().getId());
            var o=em.find(Order.class,s.getOrderId());
            if(s.getShopId()!=shopId||settings.shop(o.getStore().getId())!=shopId||!s.getClientReference().equals(reference)||s.getTrackingCode()!=null&&!s.getTrackingCode().equals(code))throw new MarketplaceException(403,"CARRIER_REFERENCE_MISMATCH","Thông tin vận đơn không khớp.");
            if(s.getTrackingCode()==null)s.setTrackingCode(code);apply(s,status,time,"WEBHOOK",type);s.setLastSyncAt(access.now());return null;
        });
    }
    private void apply(Shipment s,String raw,Instant instant,String source,String type){
        if(raw==null||raw.length()>40||instant==null)throw MarketplaceException.invalid("Thiếu trạng thái hoặc thời gian sự kiện.");
        var next=ShipmentState.fromGhn(raw);var time=LocalDateTime.ofInstant(instant,ZoneOffset.UTC);
        if(time.isAfter(access.now().plusMinutes(5))||time.isBefore(s.getCreatedAt().minusMinutes(5)))throw MarketplaceException.invalid("Thời gian sự kiện không phù hợp vận đơn.");
        String key=hash(s.getClientReference()+"|"+type+"|"+raw+"|"+instant);
        if(em.createQuery("select count(e) from ShipmentEvent e where e.shipmentId=:id and e.dedupKey=:key",Long.class).setParameter("id",s.getId()).setParameter("key",key).getSingleResult()>0)return;
        var e=new ShipmentEvent();e.setShipmentId(s.getId());e.setDedupKey(key);e.setCarrierStatus(raw);e.setState(next);e.setEventAt(time);e.setReceivedAt(access.now());e.setSource(source);em.persist(e);
        if((s.getLastEventAt()==null||time.isAfter(s.getLastEventAt()))&&s.getState().accepts(next)){
            boolean changed=s.getState()!=next;s.setState(next);s.setLastEventAt(time);s.setUpdatedAt(access.now());
            if(changed&&Set.of(ShipmentState.WAITING_PICKUP,ShipmentState.PICKED_UP,ShipmentState.DELIVERED).contains(next)){
                String event=next==ShipmentState.WAITING_PICKUP?"SHIPMENT_CREATED":next.name();
                com.bonsaimarket.backend.notification.OrderNotifications.enqueue(em,access,em.find(Order.class,s.getOrderId()),event);
            }
        }
        // Late events stay in history. They never drive inventory, refunds or the Order state machine.
    }
    private void validateRemote(Shipment s,Remote r){if(r==null||r.shopId()!=s.getShopId()||!s.getClientReference().equals(r.clientReference())||r.trackingCode()==null||!r.trackingCode().matches("[A-Za-z0-9_-]{1,50}")||s.getTrackingCode()!=null&&!s.getTrackingCode().equals(r.trackingCode()))throw conflict("CARRIER_REFERENCE_MISMATCH","GHN trả về mã không khớp vận đơn. Cần đối chiếu thủ công.");}
    public void codReport(int shop,String code,String reference,Instant time,Integer amount,Instant transfer,BigDecimal fee,Integer feePayer){db(()->{
        var found=em.createQuery("select s from Shipment s where s.clientReference=:ref",Shipment.class).setParameter("ref",reference).getResultStream().findFirst().orElseThrow(MarketplaceException::missing);
        var o=access.lock(Order.class,found.getOrderId());var s=lockShipment(found.getId());
        if(s.getShopId()!=shop||settings.shop(o.getStore().getId())!=shop||s.getTrackingCode()==null||!s.getTrackingCode().equals(code))throw new MarketplaceException(403,"CARRIER_REFERENCE_MISMATCH","Callback không khớp vận đơn đã xác minh.");
        if(time==null||transfer==null||amount==null||amount<0||amount>50000000||feePayer==null||feePayer<1||feePayer>2||fee!=null&&(fee.signum()<0||fee.compareTo(new BigDecimal("50000000"))>0||fee.stripTrailingZeros().scale()>0))throw MarketplaceException.invalid("Thiếu bằng chứng COD hoặc thời điểm chuyển do GHN báo.");
        var at=LocalDateTime.ofInstant(time,ZoneOffset.UTC);var transferred=LocalDateTime.ofInstant(transfer,ZoneOffset.UTC);
        if(at.isAfter(access.now().plusMinutes(5))||at.isBefore(s.getCreatedAt().minusMinutes(5))||transferred.isAfter(at.plusMinutes(5))||transferred.isBefore(s.getCreatedAt().minusMinutes(5)))throw MarketplaceException.invalid("Thời gian báo cáo COD chưa phù hợp vận đơn.");
        String key=hash(reference+"|cod|"+time+"|"+amount+"|"+transfer+"|"+fee+"|"+feePayer);
        if(em.createQuery("select count(r) from CodProviderReport r where r.dedupKey=:key",Long.class).setParameter("key",key).getSingleResult()>0)return null;
        var p=em.find(com.bonsaimarket.backend.payment.OrderPayment.class,o.getId());
        boolean discrepancy=p==null||!p.getMethod().equals("COD")||p.getStatus().equals("CANCELLED")||p.getCodAmount().compareTo(BigDecimal.valueOf(amount))!=0||p.getFeePayer()!=feePayer||fee!=null&&fee.signum()>0&&p.getShippingFee().compareTo(fee)!=0;
        var r=new com.bonsaimarket.backend.payment.CodProviderReport();r.setId(UUID.randomUUID().toString());r.setOrderId(o.getId());r.setShipmentId(s.getId());r.setDedupKey(key);r.setReportedAmount(BigDecimal.valueOf(amount));r.setReportedFee(fee);r.setTransferAt(transferred);r.setReportedAt(at);r.setReceivedAt(access.now());r.setComparison(discrepancy?"DISCREPANCY":fee==null||fee.signum()==0?"FEES_UNVERIFIED":"AMOUNT_MATCH");em.persist(r);
        if(p!=null){
            // A late matching callback cannot erase an earlier discrepancy. Bank settlement is never inferred.
            if(discrepancy||"DISCREPANCY".equals(p.getSettlementStatus())){p.setSettlementStatus("DISCREPANCY");p.setStatus("REVIEW_REQUIRED");}
            else p.setSettlementStatus("PENDING_RECONCILIATION");
        }
        return null;
    });}
    private Shipment shipment(Long id){return em.createQuery("select s from Shipment s where s.orderId=:id",Shipment.class).setParameter("id",id).getResultStream().findFirst().orElse(null);}
    private ShippingQuote selection(Long id){return em.createQuery("select q from ShippingQuote q where q.orderId=:id and q.selectedAt is not null",ShippingQuote.class).setParameter("id",id).getResultStream().findFirst().orElse(null);}
    private Tracking tracking(Shipment s){if(s==null)return null;return new Tracking("GHN","STAGING",s.getTrackingCode(),s.getState(),s.getLastSyncAt(),em.createQuery("select e from ShipmentEvent e where e.shipmentId=:id order by e.eventAt desc,e.id desc",ShipmentEvent.class).setParameter("id",s.getId()).setMaxResults(100).getResultStream().map(e->new EventView(e.getState(),e.getEventAt(),e.getReceivedAt(),e.getSource())).toList());}
    private QuoteView view(ShippingQuote q){return new QuoteView(q.getId(),"GHN","STAGING",q.getServiceType(),q.getServiceName(),q.getFee(),"VND",q.getExpiresAt(),q.getSelectedAt());}
    private int insurance(Order o){try{int amount=o.getSubtotal().intValueExact();if(amount<0||amount>5000000)throw new ArithmeticException();return amount;}catch(ArithmeticException e){throw conflict("INSURANCE_REVIEW_REQUIRED","Giá trị hàng vượt giới hạn khai giá của tích hợp hiện tại. Cần đối chiếu hợp đồng GHN.");}}
    private String fingerprint(Order o){var lines=em.createQuery("select i.id,i.product.id,i.quantity,i.unitPrice,i.lineTotal,i.productNameSnapshot from OrderItem i where i.order.id=:id order by i.id",Object[].class).setParameter("id",o.getId()).getResultList();return hash(json.writeValueAsString(Arrays.asList(o.getId(),o.getStore().getId(),o.getBuyer().getId(),o.getShippingAddressSnapshot(),o.getSubtotal(),o.getShippingFee(),o.getTotal(),lines)));}
    static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private static MarketplaceException conflict(String code,String message){return new MarketplaceException(409,code,message);}
}
