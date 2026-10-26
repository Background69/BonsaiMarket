package com.bonsaimarket.backend.shipping;

import com.bonsaimarket.backend.marketplace.MarketplaceException;
import java.net.*;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.Flow;
import javax.net.ssl.SSLSession;
import org.junit.jupiter.api.*;
import tools.jackson.databind.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static com.bonsaimarket.backend.shipping.ShippingProvider.*;

/** Captures actual HTTP request construction against documented GHN schemas; mocked transport, no external I/O. */
class GhnShippingProviderTest {
    final ShippingSettings settings=new ShippingSettings();final ObjectMapper json=new ObjectMapper();
    final HttpClient http=mock(HttpClient.class);GhnShippingProvider provider;
    final List<HttpRequest> requests=new ArrayList<>();int status=200;String body="";
    @BeforeEach void setup()throws Exception{
        settings.setEnabled(true);settings.setToken("test-token");provider=new GhnShippingProvider(settings,json,http);
        when(http.send(any(),any(HttpResponse.BodyHandler.class))).thenAnswer(a->{HttpRequest r=a.getArgument(0);requests.add(r);return new Response(r,status,body);});
    }
    record Response(HttpRequest request,int statusCode,String body)implements HttpResponse<String>{
        public Optional<HttpResponse<String>> previousResponse(){return Optional.empty();}public HttpHeaders headers(){return HttpHeaders.of(Map.of(),(a,b)->true);}public Optional<SSLSession> sslSession(){return Optional.empty();}public URI uri(){return request.uri();}public HttpClient.Version version(){return HttpClient.Version.HTTP_1_1;}
    }
    Destination address(boolean newer){return new Destination("Test recipient","0901234567","Hồ Chí Minh",newer?"":"Quận 1","Phường Bến Nghé","Test street",null,null,null,newer);}
    Pickup pickup(){return new Pickup(123,1452,"21007","Test pickup","0900000000","Test warehouse");}
    Parcel parcel(){return new Parcel(800,25,20,15);}
    JsonNode requestBody(HttpRequest request)throws Exception{
        var done=new CompletableFuture<String>();var bytes=new java.io.ByteArrayOutputStream();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>(){
            public void onSubscribe(Flow.Subscription s){s.request(Long.MAX_VALUE);}public void onNext(ByteBuffer b){byte[] data=new byte[b.remaining()];b.get(data);bytes.writeBytes(data);}public void onError(Throwable t){done.completeExceptionally(t);}public void onComplete(){done.complete(bytes.toString(StandardCharsets.UTF_8));}
        });return json.readTree(done.get(2,TimeUnit.SECONDS));
    }
    @Test void fixedStagingHostHeadersAndFeeSchemaUseServiceTypeNotServiceId()throws Exception{
        when(http.send(any(),any(HttpResponse.BodyHandler.class))).thenAnswer(a->{HttpRequest r=a.getArgument(0);requests.add(r);return new Response(r,200,r.uri().getPath().endsWith("available-services")?"{\"code\":200,\"data\":[{\"service_id\":53320,\"service_type_id\":2,\"short_name\":\"Hàng nhẹ\"}]}":"{\"code\":200,\"data\":{\"total\":20900}}");});
        var a=new Destination("Test","0901234567","Hồ Chí Minh","Quận 1","Phường Bến Nghé","Test",202,1444,"20308",false);
        assertEquals("20900",provider.quote(123,pickup(),a,parcel(),100000).getFirst().fee().toPlainString());
        for(var r:requests){assertEquals("dev-online-gateway.ghn.vn",r.uri().getHost());assertEquals("https",r.uri().getScheme());assertEquals("123",r.headers().firstValue("ShopId").orElseThrow());assertEquals("test-token",r.headers().firstValue("Token").orElseThrow());assertEquals("POST",r.method());}
        var payload=requestBody(requests.getLast());assertEquals(2,payload.path("service_type_id").asInt());assertFalse(payload.has("service_id"));assertEquals(800,payload.path("weight").asInt());assertEquals(0,payload.path("cod_value").asInt());
    }
    @Test void legacyAddressMappingUsesUniqueExactProviderNamesAndRejectsForgedCodes()throws Exception{
        when(http.send(any(),any(HttpResponse.BodyHandler.class))).thenAnswer(a->{HttpRequest r=a.getArgument(0);requests.add(r);String p=r.uri().getPath();String data=p.endsWith("province")?"[{\"ProvinceID\":202,\"ProvinceName\":\"Hồ Chí Minh\"}]":p.endsWith("district")?"[{\"DistrictID\":1444,\"DistrictName\":\"Quận 1\"}]":"[{\"WardCode\":\"20308\",\"WardName\":\"Phường Bến Nghé\"}]";return new Response(r,200,"{\"code\":200,\"data\":"+data+"}");});
        var resolved=provider.validateAddress(123,address(false));assertEquals(1444,resolved.districtId());assertEquals("20308",resolved.wardCode());assertTrue(requests.stream().allMatch(r->r.method().equals("GET")));
        assertThrows(MarketplaceException.class,()->provider.validateAddress(123,new Destination("Test","0901234567","Hồ Chí Minh","Quận 1","Phường Bến Nghé","Test",202,99999,"FORGED",false)));
        assertThrows(MarketplaceException.class,()->provider.validateAddress(123,new Destination("Test","0901234567","TP tự đoán","Quận 1","Phường Bến Nghé","Test",null,null,null,false)));
    }
    @Test void twoLevelMasterDataIsRecognizedButFeeDoesNotInventLegacyDistrict()throws Exception{
        when(http.send(any(),any(HttpResponse.BodyHandler.class))).thenAnswer(a->{HttpRequest r=a.getArgument(0);requests.add(r);return new Response(r,200,r.uri().getPath().contains("province/all")?"{\"code\":200,\"data\":[{\"_id\":1000001,\"name\":\"Hồ Chí Minh\"}]}":"{\"code\":200,\"data\":[{\"_id\":1000010,\"name\":\"Phường Bến Nghé\",\"parent_id\":1000001,\"status\":1}]}");});
        var resolved=provider.validateAddress(123,address(true));assertTrue(resolved.newAddress());assertNull(resolved.districtId());assertEquals(2,requests.size());
        assertEquals("ADDRESS_COMPATIBILITY_REQUIRED",assertThrows(MarketplaceException.class,()->provider.quote(123,pickup(),resolved,parcel(),100000)).code);assertEquals(2,requests.size());
    }
    @Test void nullOrMalformedProviderFeeNeverBecomesZero(){
        for(String value:List.of("", "{}", "{\"code\":200,\"data\":null}","{\"code\":200,\"data\":{}} trailing")){body=value;assertThrows(MarketplaceException.class,()->provider.find(123,"TEST","BM-ref"));}
    }
    @Test void createRequestCarriesStableReferenceAndActualParcel()throws Exception{
        body="{\"code\":200,\"data\":{\"order_code\":\"STAGE123\"}}";
        var remote=provider.create(123,new Create("BM-stable",address(false),pickup(),parcel(),2,100000,1,0,List.of(new Item("Test goods",1,100000))));assertEquals("STAGE123",remote.trackingCode());assertNull(remote.status());assertNull(remote.eventAt());
        var payload=requestBody(requests.getFirst());assertEquals("BM-stable",payload.path("client_order_code").asText());assertEquals(1,payload.path("payment_type_id").asInt());assertEquals(0,payload.path("cod_amount").asInt());assertEquals(15,payload.path("height").asInt());assertEquals("Test goods",payload.path("items").get(0).path("name").asText());
    }
    @Test void trackingReconciliationUsesDocumentedGetByClientCode(){
        body="{\"code\":200,\"data\":{\"order_code\":\"STAGE123\",\"client_order_code\":\"BM-stable\",\"shop_id\":123,\"status\":\"picked\",\"updated_date\":\"2026-10-25T10:00:00Z\"}}";
        var found=provider.find(123,null,"BM-stable");assertEquals("picked",found.status());assertEquals("GET",requests.getFirst().method());assertEquals("client_order_code=BM-stable",requests.getFirst().uri().getQuery());assertTrue(requests.getFirst().uri().getPath().endsWith("detail-by-client-code"));
    }
    @Test void cancelChecksPerOrderResultInsteadOfOnlyHttpStatus()throws Exception{
        body="{\"code\":200,\"data\":[{\"order_code\":\"STAGE123\",\"result\":false}]}";assertFalse(provider.cancel(123,"STAGE123"));assertEquals("STAGE123",requestBody(requests.getFirst()).path("order_codes").get(0).asText());
        body="{\"code\":200,\"data\":[{\"order_code\":\"OTHER\",\"result\":true}]}";assertThrows(MarketplaceException.class,()->provider.cancel(123,"STAGE123"));
    }
    @Test void providerAuthenticationOutageAndDisabledConfigAreExplicit(){
        for(int code:List.of(401,403,429,500)){status=code;assertThrows(MarketplaceException.class,()->provider.find(123,"TEST","BM-ref"));}
        settings.setEnabled(false);assertEquals("SHIPPING_NOT_CONFIGURED",assertThrows(MarketplaceException.class,()->provider.find(123,"TEST","BM-ref")).code);
        assertFalse(settings.toString().contains("test-token"));
    }
    @Test void unknownCarrierStatusNeverMapsToCancelledOrDelivered(){assertThrows(MarketplaceException.class,()->ShipmentState.fromGhn("unknown"));assertEquals(ShipmentState.RETURN_FAILED,ShipmentState.fromGhn("return_fail"));assertFalse(ShipmentState.DELIVERED.accepts(ShipmentState.IN_TRANSIT));assertFalse(ShipmentState.PICKED_UP.accepts(ShipmentState.CANCELLED));}
    @Test void returnToSenderChecksPerOrderResultAndUsesStagingEndpoint()throws Exception{
        body="{\"code\":200,\"data\":[{\"order_code\":\"STAGE123\",\"result\":false}]}";assertFalse(provider.returnToSender(123,"STAGE123"));assertEquals("dev-online-gateway.ghn.vn",requests.getFirst().uri().getHost());assertTrue(requests.getFirst().uri().getPath().endsWith("switch-status/return"));assertEquals("STAGE123",requestBody(requests.getFirst()).path("order_codes").get(0).asText());
        body="{\"code\":200,\"data\":[{\"order_code\":\"OTHER\",\"result\":true}]}";assertThrows(MarketplaceException.class,()->provider.returnToSender(123,"STAGE123"));
    }
}
