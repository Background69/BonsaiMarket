package com.bonsaimarket.backend.shipping;

import com.bonsaimarket.backend.marketplace.MarketplaceException;
import tools.jackson.databind.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.Flow;

/** GHN's current developer.ghn.vn contracts, reviewed 2026-10-25.
 * Staging ONLY: production activation requires a separate reviewed change and payment contract. */
@Component
public class GhnShippingProvider implements ShippingProvider {
    static final String BASE="https://dev-online-gateway.ghn.vn/shiip/public-api";
    private final ShippingSettings settings;
    private final ObjectMapper json;
    private final HttpClient http;
    private final Semaphore slots=new Semaphore(4);
    @Autowired public GhnShippingProvider(ShippingSettings settings,ObjectMapper json){
        this(settings,json,HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).followRedirects(HttpClient.Redirect.NEVER).build());
    }
    GhnShippingProvider(ShippingSettings settings,ObjectMapper json,HttpClient http){this.settings=settings;this.json=json;this.http=http;}

    @Override public Pickup pickup(int shopId){
        int offset=0;
        for(int page=0;page<5;page++){
            var data=call(shopId,"/v2/shop/all?offset="+offset+"&limit=100",null);
            for(var s:data.path("shops"))if(s.path("_id").asInt()==shopId&&s.path("status").asInt()==1){
                int district=s.path("district_id").asInt();String ward=text(s,"ward_code",20);
                if(district<1)throw addressError();
                return new Pickup(shopId,district,ward,text(s,"name",200),text(s,"phone",30),text(s,"address",1024));
            }
            int next=data.path("last_offset").asInt();
            if(next<=offset||data.path("shops").isEmpty())break;offset=next;
        }
        throw new MarketplaceException(409,"CARRIER_SHOP_NOT_OWNED","Token platform không có shop GHN đang hoạt động tương ứng. Liên hệ quản trị viên.");
    }
    @Override public Destination validateAddress(int shopId,Destination a){
        if(a==null||blank(a.recipientName())||a.recipientName().length()>150||a.phone()==null||!a.phone().matches("(?:0[2-9][0-9]{8,9}|\\+84[2-9][0-9]{8,9})")
            ||blank(a.addressDetail())||a.addressDetail().length()>500||blank(a.provinceName())||blank(a.wardName()))throw addressError();
        if(a.newAddress()||blank(a.districtName())){
            var province=match(call(shopId,"/v3/master-data/province/all?offset=0&limit=200",null),"name",a.provinceName(),"_id",a.provinceId());
            int id=province.path("_id").asInt();
            var ward=match(call(shopId,"/v3/master-data/ward/all-by-province-id?province_id="+id+"&offset=0&limit=200",null),"name",a.wardName(),"_id",null);
            if(ward.path("status").asInt()!=1||ward.path("parent_id").asInt()!=id)throw addressError();
            return new Destination(a.recipientName(),a.phone(),text(province,"name",150),"",text(ward,"name",150),a.addressDetail(),id,null,null,true);
        }
        var p=match(call(shopId,"/master-data/province",null),"ProvinceName",a.provinceName(),"ProvinceID",a.provinceId());
        int province=p.path("ProvinceID").asInt();
        var d=match(call(shopId,"/master-data/district?province_id="+province,null),"DistrictName",a.districtName(),"DistrictID",a.districtId());
        int district=d.path("DistrictID").asInt();
        var w=match(call(shopId,"/master-data/ward?district_id="+district,null),"WardName",a.wardName(),"WardCode",null);
        String ward=text(w,"WardCode",20);
        if(a.wardCode()!=null&&!a.wardCode().equals(ward))throw addressError();
        return new Destination(a.recipientName(),a.phone(),text(p,"ProvinceName",150),text(d,"DistrictName",150),text(w,"WardName",150),a.addressDetail(),province,district,ward,false);
    }
    @Override public List<Rate> quote(int shopId,Pickup pickup,Destination a,Parcel parcel,int insurance){
        return rates(shopId,pickup,a,parcel,insurance,0);
    }
    @Override public List<Rate> quoteCod(int shopId,Pickup pickup,Destination a,Parcel parcel,int insurance,int codAmount){
        return rates(shopId,pickup,a,parcel,insurance,codAmount);
    }
    private List<Rate> rates(int shopId,Pickup pickup,Destination a,Parcel parcel,int insurance,int codAmount){
        if(a.newAddress()||a.districtId()==null||blank(a.wardCode()))
            throw new MarketplaceException(422,"ADDRESS_COMPATIBILITY_REQUIRED","Địa chỉ hai cấp đã được nhận diện. GHN tính phí còn cần mã quận tương thích; chưa có mapping được xác minh nên chưa thể báo phí. Không đổi địa chỉ cũ tự động.");
        var services=call(shopId,"/v2/shipping-order/available-services",Map.of("shop_id",shopId,"from_district",pickup.districtId(),"to_district",a.districtId()));
        if(!services.isArray())throw malformed();
        List<Rate> rates=new ArrayList<>();
        for(var s:services){
            if(s.path("service_type_id").asInt()!=2||!rates.isEmpty())continue; // Single measured parcel under 20kg only.
            var body=new LinkedHashMap<String,Object>();
            body.put("from_district_id",pickup.districtId());body.put("from_ward_code",pickup.wardCode());
            body.put("to_district_id",a.districtId());body.put("to_ward_code",a.wardCode());body.put("service_type_id",2);
            body.put("weight",parcel.weightGrams());body.put("length",parcel.lengthCm());body.put("width",parcel.widthCm());body.put("height",parcel.heightCm());
            body.put("insurance_value",insurance);body.put("cod_value",codAmount);
            var fee=call(shopId,"/v2/shipping-order/fee",body).path("total");
            if(!fee.isIntegralNumber()||!fee.canConvertToInt()||fee.asInt()<0||fee.asInt()>50000000)throw malformed();
            rates.add(new Rate(2,text(s,"short_name",120),new java.math.BigDecimal(fee.asText())));
        }
        if(rates.isEmpty())throw new MarketplaceException(422,"UNSUPPORTED_ROUTE","GHN chưa có dịch vụ kiện nhẹ phù hợp cho tuyến này.");
        return rates;
    }
    @Override public Remote create(int shopId,Create r){
        var a=r.destination();var p=r.parcel();var b=new LinkedHashMap<String,Object>();
        b.put("client_order_code",r.clientReference());b.put("to_name",a.recipientName());b.put("to_phone",a.phone());b.put("to_address",a.addressDetail());
        b.put("to_province_name",a.provinceName());b.put("to_district_name",a.districtName());b.put("to_ward_name",a.wardName());b.put("is_new_to_address",a.newAddress());
        b.put("from_name",r.pickup().name());b.put("from_phone",r.pickup().phone());b.put("from_address",r.pickup().address());
        // Pickup remains the carrier-registered shop, never public store coordinates or user.address.
        b.put("service_type_id",r.serviceType());b.put("payment_type_id",r.feePayer());b.put("cod_amount",r.codAmount());
        b.put("insurance_value",r.insuranceValue());b.put("required_note","KHONGCHOXEMHANG");
        b.put("weight",p.weightGrams());b.put("length",p.lengthCm());b.put("width",p.widthCm());b.put("height",p.heightCm());
        b.put("items",r.items().stream().map(i->Map.of("name",i.name(),"quantity",i.quantity(),"price",i.price())).toList());
        var data=call(shopId,"/v2/shipping-order/create",b);
        // Create returns a code, not a carrier status/event time. Read detail before adding timeline facts.
        return new Remote(text(data,"order_code",50),r.clientReference(),shopId,null,null);
    }
    @Override public Remote find(int shopId,String trackingCode,String reference){
        String path=trackingCode==null?"/v2/shipping-order/detail-by-client-code?client_order_code="+encode(reference):"/v2/shipping-order/detail?order_code="+encode(trackingCode);
        var d=call(shopId,path,null);
        Instant time;
        try{time=Instant.parse(text(d,"updated_date",40));}catch(RuntimeException e){throw malformed();}
        return new Remote(text(d,"order_code",50),text(d,"client_order_code",50),d.path("shop_id").asInt(),text(d,"status",40),time);
    }
    @Override public boolean cancel(int shopId,String code){
        var d=call(shopId,"/v2/switch-status/cancel",Map.of("order_codes",List.of(code)));
        if(!d.isArray()||d.size()!=1||!code.equals(d.get(0).path("order_code").asText())||!d.get(0).path("result").isBoolean())throw malformed();
        return d.get(0).path("result").asBoolean();
    }
    @Override public boolean returnToSender(int shopId,String code){
        var d=call(shopId,"/v2/switch-status/return",Map.of("order_codes",List.of(code)));
        if(!d.isArray()||d.size()!=1||!code.equals(d.get(0).path("order_code").asText())||!d.get(0).path("result").isBoolean())throw malformed();
        return d.get(0).path("result").asBoolean();
    }
    private JsonNode call(int shopId,String path,Object body){
        if(!settings.isEnabled()||settings.getToken().isBlank())throw new MarketplaceException(503,"SHIPPING_NOT_CONFIGURED","GHN thử nghiệm chưa được cấu hình.");
        if(!slots.tryAcquire())throw new MarketplaceException(429,"CARRIER_BUSY","GHN đang có nhiều yêu cầu. Thử lại sau ít phút.");
        try{
            var request=HttpRequest.newBuilder(URI.create(BASE+path)).timeout(Duration.ofSeconds(8))
                .header("Token",settings.getToken()).header("ShopId",Integer.toString(shopId)).header("Accept","application/json");
            if(body!=null)request.header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));else request.GET();
            var response=http.send(request.build(),bounded());
            if(response.statusCode()==401||response.statusCode()==403)throw new MarketplaceException(503,"CARRIER_AUTH","Cấu hình truy cập GHN chưa hợp lệ. Liên hệ quản trị viên.");
            if(response.statusCode()==429)throw new MarketplaceException(429,"CARRIER_RATE_LIMIT","GHN giới hạn yêu cầu. Thử lại sau ít phút.");
            if(response.statusCode()>=500)throw unavailable();
            if(response.statusCode()<200||response.statusCode()>=300)throw new MarketplaceException(422,"CARRIER_REJECTED","GHN từ chối thông tin hoặc tuyến vận chuyển. Kiểm tra cấu hình shop, địa chỉ và kiện hàng.");
            var root=json.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(response.body());
            if(root==null||!root.isObject()||root.path("code").asInt()!=200||root.path("data").isNull()||root.path("data").isMissingNode())throw malformed();
            return root.path("data");
        }catch(MarketplaceException e){throw e;}
        catch(InterruptedException e){Thread.currentThread().interrupt();throw unavailable();}
        catch(IOException e){throw unavailable();}
        catch(RuntimeException e){throw malformed();}
        finally{slots.release();}
    }
    private static HttpResponse.BodyHandler<String> bounded(){return info->new HttpResponse.BodySubscriber<>(){
        final HttpResponse.BodySubscriber<String> delegate=HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);
        Flow.Subscription subscription;long size;boolean ended;
        public CompletionStage<String> getBody(){return delegate.getBody();}
        public void onSubscribe(Flow.Subscription s){subscription=s;delegate.onSubscribe(s);}
        public void onNext(List<ByteBuffer> chunks){if(ended)return;for(var c:chunks)size+=c.remaining();if(size>262144){ended=true;subscription.cancel();delegate.onError(new IOException("Carrier response too large"));}else delegate.onNext(chunks);}
        public void onError(Throwable e){if(!ended){ended=true;delegate.onError(e);}}
        public void onComplete(){if(!ended){ended=true;delegate.onComplete();}}
    };}
    private static JsonNode match(JsonNode rows,String field,String name,String idField,Integer id){
        if(!rows.isArray())throw addressError();List<JsonNode> found=new ArrayList<>();
        for(var r:rows)if(name!=null&&name.strip().equalsIgnoreCase(r.path(field).asText().strip())&&(id==null||r.path(idField).asInt()==id))found.add(r);
        if(found.size()!=1)throw addressError();return found.getFirst();
    }
    private static boolean blank(String s){return s==null||s.isBlank();}
    private static String text(JsonNode n,String key,int max){String s=n.path(key).asText();if(s.isBlank()||s.length()>max)throw malformed();return s;}
    private static String encode(String s){if(s==null||!s.matches("[A-Za-z0-9_-]{1,50}"))throw malformed();return URLEncoder.encode(s,StandardCharsets.UTF_8);}
    private static MarketplaceException addressError(){return new MarketplaceException(422,"ADDRESS_INVALID","Địa chỉ thiếu thông tin hoặc chưa khớp duy nhất với danh mục GHN. Cần chỉnh sửa/đối chiếu địa chỉ, không tự đổi mã.");}
    private static MarketplaceException malformed(){return new MarketplaceException(502,"CARRIER_RESPONSE_INVALID","GHN trả về dữ liệu chưa hợp lệ. Tải lại để đối chiếu; không gửi tạo vận đơn lặp lại.");}
    private static MarketplaceException unavailable(){return new MarketplaceException(503,"CARRIER_UNAVAILABLE","Chưa nhận được phản hồi hợp lệ từ GHN. Đối chiếu trạng thái trước khi thử lại.");}
}
