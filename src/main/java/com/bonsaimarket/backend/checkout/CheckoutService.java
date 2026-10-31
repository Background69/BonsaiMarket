package com.bonsaimarket.backend.checkout;

import com.bonsaimarket.backend.address.*;
import com.bonsaimarket.backend.cart.*;
import com.bonsaimarket.backend.inventory.InventoryService;
import com.bonsaimarket.backend.marketplace.*;
import com.bonsaimarket.backend.order.*;
import com.bonsaimarket.backend.order.Order;
import com.bonsaimarket.backend.payment.OrderPayment;
import com.bonsaimarket.backend.product.Product;
import com.bonsaimarket.backend.shipping.*;
import com.bonsaimarket.backend.store.*;
import com.bonsaimarket.backend.user.User;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import static com.bonsaimarket.backend.shipping.ShippingProvider.*;

/** Mỗi lần đặt chọn rõ một gian hàng; sản phẩm của gian hàng khác vẫn trong giỏ. */
@Service
public class CheckoutService {
    public record Content(@NotNull @Positive Long productId,@Min(1) @Max(1000000) int quantity) {}
    public record PackageWrite(@NotEmpty @Size(max=100) List<@Valid Content> items,
        @NotNull @Valid ShippingService.ParcelWrite parcel,boolean enabled,@Min(0) long revision) {}
    public record QuoteInput(@NotNull @Positive Long storeId,@NotBlank @Size(min=64,max=64) String fingerprint,
        @Positive Long addressId,@Valid AddressDtos.Write delivery, @Positive Long productId,@Min(1) @Max(1000000) Integer quantity) {
        public QuoteInput(Long storeId,String fingerprint,Long addressId,AddressDtos.Write delivery){this(storeId,fingerprint,addressId,delivery,null,null);}
        @AssertTrue(message="Chọn địa chỉ đã lưu hoặc nhập địa chỉ dùng riêng cho đơn này.")
        public boolean isAddressChoiceValid(){return (addressId==null)!=(delivery==null);}
        @AssertTrue(message="Mua ngay cần cả sản phẩm và số lượng.")
        public boolean isProductChoiceValid(){return (productId==null)==(quantity==null);}
    }
    public record Place(@NotBlank @Pattern(regexp="[a-f0-9-]{36}") String quoteId,
        @NotBlank @Pattern(regexp="[A-Za-z0-9_-]{16,80}") String idempotencyKey,
        @NotBlank @Pattern(regexp="COD") String paymentMethod,@Size(max=1000) String note) {}
    public record Preview(CartDtos.View cart,String fingerprint,List<String> paymentMethods,String mode) {}
    public record QuoteView(String id,String provider,String environment,String serviceName,BigDecimal subtotal,BigDecimal shippingFee,
        BigDecimal total,String currency,int feePayer,BigDecimal codAmount,Destination destination,LocalDateTime expiresAt) {}
    public record Receipt(Long orderId,String code,String status,String paymentStatus,BigDecimal total,String currency,boolean replayed) {}
    public record AddressInput(Long addressId,Destination input,Long productId,Integer quantity) { public AddressInput(Long addressId,Destination input){this(addressId,input,null,null);} }
    private record Context(Long storeId,String storeName,String fingerprint,String contents,List<CartItem> lines,BigDecimal subtotal) {}
    private record Prepared(String fingerprint,String packageId,long revision,int shopId,Parcel parcel,Destination destination,String addressInput,int amount) {}
    private final EntityManager em;private final MarketplaceAccess access;private final CartService carts;private final AddressService addresses;
    private final StoreLocations stores;private final ShippingSettings settings;private final ShippingProvider provider;
    private final ShippingService shipping;private final InventoryService inventory;private final ObjectMapper json;private final TransactionTemplate tx;
    public CheckoutService(EntityManager em,MarketplaceAccess access,CartService carts,AddressService addresses,StoreLocations stores,
        ShippingSettings settings,ShippingProvider provider,ShippingService shipping,InventoryService inventory,ObjectMapper json,PlatformTransactionManager manager){
        this.em=em;this.access=access;this.carts=carts;this.addresses=addresses;this.stores=stores;this.settings=settings;this.provider=provider;this.shipping=shipping;this.inventory=inventory;this.json=json;
        tx=new TransactionTemplate(manager);tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }
    private <T>T db(Supplier<T> action){return tx.execute(s->action.get());}
    public Preview preview(Long actor){return db(()->{var cart=carts.locked(actor);return new Preview(carts.view(actor),fingerprint(carts.items(cart)),List.of("COD"),"SINGLE_STORE_SELECTION");});}
    private String fingerprint(List<CartItem> rows){return hash(json.writeValueAsString(rows.stream().map(i->{var p=i.getProduct();return Arrays.asList(i.getId(),p.getId(),i.getQuantity(),p.getPrice(),p.getName(),p.getStatus(),p.getStore().getId(),p.getStore().getStatus());}).toList()));}
    private Context context(Long actor,Long storeId,boolean lock){return context(actor,storeId,lock,null,null);}
    private Context context(Long actor,Long storeId,boolean lock,Long productId,Integer quantity){
        var cart=carts.locked(actor);var store=lock?access.lock(Store.class,storeId):em.find(Store.class,storeId);
        if(store==null)throw MarketplaceException.missing();
        if(!stores.eligible(store))throw conflict("SELLER_UNAVAILABLE","Gian hàng chưa đủ điều kiện bán và vận chuyển.");
        if(store.getOwner().getId().equals(actor))throw conflict("SELF_PURCHASE","Không thể mua sản phẩm của chính gian hàng mình.");
        var all=carts.items(cart);
        if(productId!=null){
            if(quantity==null||quantity<1||quantity>1000000)throw MarketplaceException.invalid("Số lượng không hợp lệ.");
            var product=em.find(Product.class,productId);if(product==null||!product.getStore().getId().equals(storeId))throw MarketplaceException.missing();
            var line=new CartItem();line.setProduct(product);line.setQuantity(quantity);all=List.of(line);
        }
        if(all.isEmpty())throw conflict("EMPTY_CART","Giỏ hàng đang trống.");
        if(all.size()>100)throw MarketplaceException.invalid("Giỏ hàng vượt giới hạn 100 sản phẩm.");
        var lines=all.stream().filter(i->i.getProduct().getStore().getId().equals(storeId)).toList();
        if(lines.isEmpty())throw conflict("CART_CHANGED","Không còn sản phẩm của gian hàng đã chọn trong giỏ.");
        BigDecimal subtotal=BigDecimal.ZERO;
        for(var line:lines){var p=line.getProduct();if(lock){em.refresh(p,LockModeType.PESSIMISTIC_WRITE);}carts.validate(p,line.getQuantity());
            if(p.getPrice()==null||p.getPrice().signum()<=0)throw conflict("PRICE_INVALID","Giá sản phẩm chưa hợp lệ.");
            subtotal=subtotal.add(p.getPrice().multiply(BigDecimal.valueOf(line.getQuantity())));
        }
        try{int value=subtotal.intValueExact();if(value<1||value>5000000)throw new ArithmeticException();}catch(ArithmeticException e){throw conflict("VALUE_UNSUPPORTED","Tích hợp hiện nhận đơn có tiền hàng nguyên VND, tối đa 5.000.000 ₫ theo giới hạn khai giá.");}
        return new Context(storeId,store.getName(),fingerprint(all),contents(lines.stream().map(i->new Content(i.getProduct().getId(),i.getQuantity())).toList()),lines,subtotal);
    }
    private String contents(List<Content> items){
        if(items.stream().map(Content::productId).distinct().count()!=items.size())throw MarketplaceException.invalid("Không lặp lại sản phẩm trong mẫu kiện.");
        return json.writeValueAsString(items.stream().sorted(Comparator.comparing(Content::productId)).toList());
    }
    private Store owned(Long actor){access.seller(actor);return em.createQuery("select s from Store s where s.owner.id=:id",Store.class).setParameter("id",actor).getResultStream().findFirst().orElseThrow(MarketplaceException::missing);}
    public List<CheckoutPackage> packages(Long actor){return db(()->{var s=owned(actor);return em.createQuery("select p from CheckoutPackage p where p.storeId=:id order by p.updatedAt desc",CheckoutPackage.class).setParameter("id",s.getId()).setMaxResults(50).getResultList();});}
    public CheckoutPackage savePackage(Long actor,String id,PackageWrite b){return db(()->{
        var store=access.lock(Store.class,owned(actor).getId());if(!stores.eligible(store))throw conflict("SELLER_APPROVAL_REQUIRED","Cần gian hàng đang hoạt động và người bán được duyệt.");
        var p=em.find(CheckoutPackage.class,id);
        if(p!=null&&!p.getStoreId().equals(store.getId()))throw MarketplaceException.missing();
        MarketplaceAccess.revision(p==null?0:p.getRevision(),b.revision());
        if(b.enabled()&&!b.parcel().carrierAcceptedGoods())throw conflict("GOODS_ACCEPTANCE_REQUIRED","Cần xác nhận hãng chấp nhận hàng và cách đóng gói của mẫu kiện này.");
        for(var item:b.items()){var product=em.find(Product.class,item.productId());if(product==null||!product.getStore().getId().equals(store.getId()))throw MarketplaceException.missing();}
        if(p==null){if(packages(actor).size()>=50)throw MarketplaceException.invalid("Mỗi gian hàng tối đa 50 mẫu kiện.");p=new CheckoutPackage();p.setId(id);p.setStoreId(store.getId());}
        p.setContents(contents(b.items()));var box=b.parcel();p.setParcel(json.writeValueAsString(new Parcel(box.weightGrams(),box.lengthCm(),box.widthCm(),box.heightCm())));
        p.setEnabled(b.enabled());p.setRevision(p.getRevision()+1);p.setUpdatedAt(access.now());em.persist(p);em.flush();return p;
    });}
    private CheckoutPackage matching(Context c){return em.createQuery("select p from CheckoutPackage p where p.storeId=:id and p.enabled=true order by p.updatedAt desc",CheckoutPackage.class)
        .setParameter("id",c.storeId()).setMaxResults(50).getResultStream().filter(p->p.getContents().equals(c.contents())).findFirst()
        .orElseThrow(()->conflict("CHECKOUT_PACKAGE_REQUIRED","Gian hàng chưa khai báo kiện đã đo cho đúng bộ sản phẩm và số lượng này. Liên hệ gian hàng hoặc điều chỉnh giỏ; chưa thể báo phí."));}
    private Destination address(Long actor,Long id,AddressDtos.Write input){
        if(id!=null){var a=addresses.shippingSnapshot(actor,id);return new Destination(a.recipientName(),a.phone(),a.provinceName(),a.districtName(),a.wardName(),a.addressDetail(),null,null,null,a.districtName()==null||a.districtName().isBlank());}
        return new Destination(input.recipientName(),input.phone(),input.provinceName(),input.districtName(),input.wardName(),input.addressDetail(),null,null,null,input.districtName().isBlank());
    }
    public List<QuoteView> quote(Long actor,QuoteInput b){
        var prepared=db(()->{
            var c=context(actor,b.storeId(),false,b.productId(),b.quantity());if(!c.fingerprint().equals(b.fingerprint()))throw conflict("CART_CHANGED","Giá hoặc giỏ hàng đã thay đổi. Xem lại trước khi tính phí.");
            int shop=settings.shop(b.storeId());if(!settings.getCodEnabledStoreIds().contains(b.storeId()))throw conflict("COD_ACCOUNT_REQUIRED","Gian hàng chưa được platform xác nhận quyền thu hộ COD staging.");
            var pack=matching(c);var destination=address(actor,b.addressId(),b.delivery());
            return new Prepared(c.fingerprint(),pack.getId(),pack.getRevision(),shop,json.readValue(pack.getParcel(),Parcel.class),destination,json.writeValueAsString(new AddressInput(b.addressId(),destination,b.productId(),b.quantity())),c.subtotal().intValueExact());
        });
        // Gọi hãng vận chuyển ngoài khóa DB; dữ liệu giả lập chỉ có trong tests.
        var pickup=provider.pickup(prepared.shopId());var destination=provider.validateAddress(prepared.shopId(),prepared.destination());
        var rates=provider.quoteCod(prepared.shopId(),pickup,destination,prepared.parcel(),prepared.amount(),prepared.amount());
        if(rates==null||rates.isEmpty()||rates.size()>5)throw conflict("UNSUPPORTED_ROUTE","Không có dịch vụ phù hợp địa chỉ này.");
        return db(()->{
            var c=context(actor,b.storeId(),true,b.productId(),b.quantity());checkContext(c,prepared.fingerprint(),prepared.packageId(),prepared.revision(),prepared.shopId());
            if(!json.writeValueAsString(new AddressInput(b.addressId(),address(actor,b.addressId(),b.delivery()),b.productId(),b.quantity())).equals(prepared.addressInput()))throw conflict("ADDRESS_CHANGED","Địa chỉ vừa thay đổi. Tính phí lại.");
            var result=new ArrayList<QuoteView>();
            for(var rate:rates){
                if(rate.serviceType()!=2||rate.fee()==null||rate.fee().signum()<0||rate.fee().compareTo(new BigDecimal("50000000"))>0||rate.serviceName()==null||rate.serviceName().length()>120)throw conflict("CARRIER_RESPONSE_INVALID","Báo phí không hợp lệ.");
                try{rate.fee().intValueExact();}catch(ArithmeticException e){throw conflict("CARRIER_RESPONSE_INVALID","Phí phải nguyên VND.");}
                var q=new CheckoutQuote();q.setId(UUID.randomUUID().toString());q.setBuyerId(actor);q.setStoreId(b.storeId());q.setPackageId(prepared.packageId());q.setPackageRevision(prepared.revision());
                q.setCartFingerprint(c.fingerprint());q.setAddressInput(prepared.addressInput());q.setDestinationSnapshot(json.writeValueAsString(destination));q.setPickupSnapshot(json.writeValueAsString(pickup));q.setParcelSnapshot(json.writeValueAsString(prepared.parcel()));
                q.setShopId(prepared.shopId());q.setServiceType(rate.serviceType());q.setServiceName(rate.serviceName());q.setFee(rate.fee());q.setSubtotal(c.subtotal());q.setCreatedAt(access.now());q.setExpiresAt(access.now().plusMinutes(10));em.persist(q);result.add(view(q));
            }em.flush();return result;
        });
    }
    private void checkContext(Context c,String expected,String packageId,long revision,int shop){
        var p=em.find(CheckoutPackage.class,packageId);
        if(!c.fingerprint().equals(expected)||p==null||!p.isEnabled()||p.getRevision()!=revision||!p.getStoreId().equals(c.storeId())||!p.getContents().equals(c.contents())||settings.shop(c.storeId())!=shop||!settings.getCodEnabledStoreIds().contains(c.storeId()))
            throw conflict("QUOTE_CHANGED","Giá, giỏ hàng, kiện hoặc cấu hình giao nhận đã thay đổi. Xem lại và tính phí mới.");
    }
    public Receipt place(Long actor,Place b){return db(()->{
        carts.buyer(actor);access.lock(User.class,actor);
        String requestHash=hash(json.writeValueAsString(Arrays.asList(b.quoteId(),b.paymentMethod(),b.note()==null?"":b.note().strip())));
        var previous=em.createQuery("select r from CheckoutRequest r where r.buyerId=:buyer and r.requestKey=:key",CheckoutRequest.class).setParameter("buyer",actor).setParameter("key",b.idempotencyKey()).getResultStream().findFirst().orElse(null);
        if(previous!=null){if(!previous.getRequestHash().equals(requestHash))throw conflict("IDEMPOTENCY_CONFLICT","Mã xác nhận đã dùng cho nội dung khác.");return receipt(previous.getOrderId(),true);}
        if(!"COD".equals(b.paymentMethod()))throw conflict("PAYMENT_METHOD_UNSUPPORTED","Hiện chỉ hỗ trợ COD khi tài khoản GHN staging đủ điều kiện.");
        var q=em.find(CheckoutQuote.class,b.quoteId());if(q==null||!q.getBuyerId().equals(actor))throw MarketplaceException.missing();
        if(!q.getExpiresAt().isAfter(access.now()))throw conflict("QUOTE_EXPIRED","Báo phí đã hết hạn. Xem lại và tính phí mới.");
        if(em.createQuery("select count(r) from CheckoutRequest r where r.quoteId=:id",Long.class).setParameter("id",q.getId()).getSingleResult()>0)throw conflict("QUOTE_USED","Báo phí này đã được dùng để đặt hàng. Xem lịch sử đơn.");
        var input=json.readValue(q.getAddressInput(),AddressInput.class);
        var c=context(actor,q.getStoreId(),true,input.productId(),input.quantity());checkContext(c,q.getCartFingerprint(),q.getPackageId(),q.getPackageRevision(),q.getShopId());
        if(input.addressId()!=null&&!json.writeValueAsString(address(actor,input.addressId(),null)).equals(json.writeValueAsString(input.input())))throw conflict("ADDRESS_CHANGED","Địa chỉ đã lưu vừa thay đổi. Tính phí và xác nhận lại.");
        var o=new Order();o.setCode("BM-"+UUID.randomUUID().toString());o.setBuyer(em.getReference(User.class,actor));o.setStore(em.getReference(Store.class,c.storeId()));
        o.setShippingAddressSnapshot(q.getDestinationSnapshot());o.setShippingMethod("GHN_STAGING");o.setSubtotal(c.subtotal());o.setShippingFee(q.getFee());o.setTotal(c.subtotal().add(q.getFee()));
        o.setStatus(OrderStatus.PENDING);o.setNotes(b.note()==null?"":b.note().strip());o.setCreatedAt(access.now());o.setUpdatedAt(access.now());em.persist(o);em.flush();
        for(var cartLine:c.lines()){var line=new OrderItem();line.setOrder(o);line.setProduct(cartLine.getProduct());line.setProductNameSnapshot(cartLine.getProduct().getName());line.setUnitPrice(cartLine.getProduct().getPrice());line.setQuantity(cartLine.getQuantity());line.setLineTotal(line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity())));em.persist(line);}
        em.flush();inventory.debitPlacedOrder(o.getId());
        shipping.attachCheckout(o,q.getId(),q.getShopId(),q.getServiceType(),q.getServiceName(),q.getFee(),q.getDestinationSnapshot(),q.getPickupSnapshot(),q.getParcelSnapshot(),q.getExpiresAt());
        var payment=new OrderPayment();payment.setOrderId(o.getId());payment.setQuoteId(q.getId());payment.setMethod("COD");payment.setStatus("AWAITING_COLLECTION");payment.setCurrency("VND");payment.setAmountDue(o.getTotal());payment.setCodAmount(o.getSubtotal());payment.setShippingFee(o.getShippingFee());payment.setFeePayer(2);payment.setCollectionStatus("REQUESTED");payment.setSettlementStatus("UNKNOWN");payment.setStoreNameSnapshot(c.storeName());payment.setCreatedAt(access.now());em.persist(payment);
        var request=new CheckoutRequest();request.setId(UUID.randomUUID().toString());request.setBuyerId(actor);request.setRequestKey(b.idempotencyKey());request.setRequestHash(requestHash);request.setQuoteId(q.getId());request.setOrderId(o.getId());request.setCreatedAt(access.now());em.persist(request);
        com.bonsaimarket.backend.notification.OrderNotifications.enqueue(em,access,o,"PLACED");
        if(input.productId()==null)c.lines().forEach(em::remove);em.flush();return receipt(o.getId(),false);
    });}
    public Receipt recover(Long actor,String key){return db(()->{carts.buyer(actor);var r=em.createQuery("select r from CheckoutRequest r where r.buyerId=:id and r.requestKey=:key",CheckoutRequest.class).setParameter("id",actor).setParameter("key",key).getResultStream().findFirst().orElseThrow(MarketplaceException::missing);return receipt(r.getOrderId(),true);});}
    private Receipt receipt(Long id,boolean replayed){var o=em.find(Order.class,id);var p=em.find(OrderPayment.class,id);return new Receipt(id,o.getCode(),o.getStatus().name(),p.getStatus(),o.getTotal(),"VND",replayed);}
    private QuoteView view(CheckoutQuote q){return new QuoteView(q.getId(),"GHN","STAGING",q.getServiceName(),q.getSubtotal(),q.getFee(),q.getSubtotal().add(q.getFee()),"VND",2,q.getSubtotal(),json.readValue(q.getDestinationSnapshot(),Destination.class),q.getExpiresAt());}
    public CheckoutDtos.CartView cart(Long actor){return db(()->{
        var cart=carts.locked(actor);var rows=carts.items(cart);
        return new CheckoutDtos.CartView(rows.stream().map(this::legacyItem).toList(),addresses.list(actor),null,carts.view(actor),fingerprint(rows),List.of("COD"),"SINGLE_STORE_SELECTION");
    });}
    private CheckoutDtos.Item legacyItem(CartItem row){var p=row.getProduct();return new CheckoutDtos.Item(row.getId(),p.getId(),p.getName(),p.getImageUrl(),p.getStore().getId(),p.getStore().getName(),p.getPrice(),row.getQuantity(),p.getPrice().multiply(BigDecimal.valueOf(row.getQuantity())));}
    public record Reviewed(CheckoutDtos.Request request,List<QuoteView> shippingQuotes) {}
    public CheckoutDtos.Quote quote(Long actor,CheckoutDtos.Request request){
        if(request==null||request.source()==null||request.addressId()==null||request.addressId()<1||(request.notes()!=null&&request.notes().length()>1000))throw MarketplaceException.invalid("Thông tin đặt hàng không hợp lệ.");
        if(request.source()==CheckoutDtos.Source.CART&&(request.productId()!=null||request.quantity()!=null))throw MarketplaceException.invalid("Đặt từ giỏ hàng không nhận sản phẩm hoặc số lượng riêng.");
        var input=db(()->{
            carts.buyer(actor);access.lock(User.class,actor);
            long recent=em.createQuery("select count(a) from CheckoutAttempt a where a.buyerUserId=:buyer and a.createdAt>:since",Long.class).setParameter("buyer",actor).setParameter("since",access.now().minusMinutes(10)).getSingleResult();
            if(recent>=60)throw new MarketplaceException(429,"CHECKOUT_LIMIT","Bạn đã xem lại đơn nhiều lần. Vui lòng thử lại sau vài phút.");
            Long storeId=request.storeId();
            if(request.source()==CheckoutDtos.Source.BUY_NOW){
                if(request.productId()==null||request.quantity()==null||request.quantity()<1||request.quantity()>1000000)throw MarketplaceException.invalid("Chọn sản phẩm và số lượng hợp lệ.");
                var p=em.find(Product.class,request.productId());if(p==null)throw MarketplaceException.missing();storeId=p.getStore().getId();
            }else if(storeId==null){
                var ids=carts.items(carts.locked(actor)).stream().map(i->i.getProduct().getStore().getId()).distinct().toList();
                if(ids.isEmpty())throw conflict("EMPTY_CART","Giỏ hàng đang trống.");
                if(ids.size()!=1)throw conflict("STORE_SELECTION_REQUIRED","Chọn một gian hàng để tính phí. Sản phẩm của gian hàng khác vẫn ở trong giỏ.");storeId=ids.getFirst();
            }
            var c=context(actor,storeId,false,request.productId(),request.quantity());
            return new QuoteInput(storeId,c.fingerprint(),request.addressId(),null,request.productId(),request.quantity());
        });
        var rates=quote(actor,input);
        return db(()->{
            var c=context(actor,input.storeId(),true,input.productId(),input.quantity());
            if(!c.fingerprint().equals(input.fingerprint()))throw conflict("CART_CHANGED","Giỏ hàng vừa thay đổi. Vui lòng tính phí lại.");
            var rate=rates.getFirst();var items=c.lines().stream().map(this::legacyItem).toList();
            var review=new CheckoutDtos.Review(addresses.shippingSnapshot(actor,request.addressId()),items,List.of(new CheckoutDtos.StoreTotal(c.storeId(),c.storeName(),c.subtotal(),rate.shippingFee(),rate.total())),c.subtotal(),rate.shippingFee(),rate.total(),request.notes());
            var a=new CheckoutAttempt();a.setId(UUID.randomUUID().toString());a.setBuyerUserId(actor);a.setRequestJson(json.writeValueAsString(new Reviewed(request,rates)));a.setReviewJson(json.writeValueAsString(review));a.setCreatedAt(access.now());a.setExpiresAt(rate.expiresAt());em.persist(a);
            return new CheckoutDtos.Quote(a.getId(),a.getExpiresAt().toString()+"Z",review,rates);
        });
    }
    private CheckoutAttempt attempt(Long actor,String id){carts.buyer(actor);var a=em.find(CheckoutAttempt.class,id);if(a==null||!a.getBuyerUserId().equals(actor))throw MarketplaceException.missing();return a;}
    public CheckoutDtos.Receipt submit(Long actor,String id){return submit(actor,id,null);}
    public CheckoutDtos.Receipt submit(Long actor,String id,String shippingQuoteId){return db(()->{
        carts.buyer(actor);access.lock(User.class,actor);var a=attempt(actor,id);em.refresh(a,LockModeType.PESSIMISTIC_WRITE);
        var reviewed=json.readValue(a.getRequestJson(),Reviewed.class);
        var selected=shippingQuoteId==null?reviewed.shippingQuotes().getFirst():reviewed.shippingQuotes().stream().filter(q->q.id().equals(shippingQuoteId)).findFirst().orElseThrow(MarketplaceException::missing);
        if(a.getReceiptJson()!=null){
            var r=em.createQuery("select r from CheckoutRequest r where r.buyerId=:actor and r.requestKey=:key",CheckoutRequest.class).setParameter("actor",actor).setParameter("key",id).getSingleResult();
            if(!r.getQuoteId().equals(selected.id()))throw conflict("IDEMPOTENCY_CONFLICT","Yêu cầu này đã xác nhận dịch vụ khác.");
            return json.readValue(a.getReceiptJson(),CheckoutDtos.Receipt.class);
        }
        if(!a.getExpiresAt().isAfter(access.now()))throw conflict("QUOTE_EXPIRED","Bản xem lại đã hết hạn. Vui lòng tính phí lại.");
        var result=place(actor,new Place(selected.id(),id,"COD",reviewed.request().notes()));
        var o=em.find(Order.class,result.orderId());var created=new CheckoutDtos.CreatedOrder(o.getId(),o.getCode(),o.getStore().getId(),o.getStore().getName(),o.getStatus().name(),o.getSubtotal(),o.getShippingFee(),o.getTotal());
        var receipt=new CheckoutDtos.Receipt(id,List.of(created),o.getTotal(),access.now().toString()+"Z");a.setReceiptJson(json.writeValueAsString(receipt));em.flush();return receipt;
    });}
    public CheckoutDtos.Quote savedQuote(Long actor,String id){return db(()->{var a=attempt(actor,id);return new CheckoutDtos.Quote(a.getId(),a.getExpiresAt().toString()+"Z",json.readValue(a.getReviewJson(),CheckoutDtos.Review.class),json.readValue(a.getRequestJson(),Reviewed.class).shippingQuotes());});}
    public CheckoutDtos.Receipt receipt(Long actor,String id){return db(()->{var a=attempt(actor,id);if(a.getReceiptJson()==null)throw MarketplaceException.missing();return json.readValue(a.getReceiptJson(),CheckoutDtos.Receipt.class);});}
    private static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private static MarketplaceException conflict(String code,String message){return new MarketplaceException(409,code,message);}
}
