package com.bonsaimarket.backend.payment;

import com.bonsaimarket.backend.marketplace.MarketplaceException;
import org.junit.jupiter.api.*;
import tools.jackson.databind.ObjectMapper;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/** Captured transport only. No VNPAY network, merchant access or real money. */
class VnpayGatewayTest {
    ObjectMapper json=new ObjectMapper();HttpClient http=mock(HttpClient.class);
    VnpayGateway gateway;String command="querydr",type="01",amount="13000000";boolean tampered;List<Map<String,String>> requests=new ArrayList<>();
    @BeforeEach void setup()throws Exception{
        gateway=new VnpayGateway(json,true,true,"TESTONLY","test-signing-secret","127.0.0.1",http);
        when(http.send(any(),any(HttpResponse.BodyHandler.class))).thenAnswer(a->{
            HttpRequest req=a.getArgument(0);assertEquals(VnpayGateway.API,req.uri().toString());assertEquals("POST",req.method());
            var b=json.readTree(body(req));Map<String,String> fields=new LinkedHashMap<>();b.properties().forEach(e->fields.put(e.getKey(),e.getValue().asText()));requests.add(fields);
            assertEquals(VNPayUtil.hmacSHA512("test-signing-secret",VnpayGateway.canonical(fields,"refund".equals(command)?VnpayGateway.REFUND_REQUEST:VnpayGateway.QUERY_REQUEST)),fields.get("vnp_SecureHash"));
            Map<String,String> r=new LinkedHashMap<>(Map.of("vnp_ResponseId","response-1","vnp_Command",command,"vnp_ResponseCode","00","vnp_Message","Success","vnp_TmnCode","TESTONLY","vnp_TxnRef",fields.get("vnp_TxnRef"),"vnp_Amount",amount,"vnp_TransactionNo","123456","vnp_TransactionType",type,"vnp_TransactionStatus","00"));
            r.put("vnp_OrderInfo",fields.get("vnp_OrderInfo"));r.put("vnp_BankCode","NCB");r.put("vnp_PayDate","20261113100000");
            r.put("vnp_SecureHash",VNPayUtil.hmacSHA512("test-signing-secret",VnpayGateway.canonical(r,VnpayGateway.RESPONSE+(command.equals("querydr")?",vnp_PromotionCode,vnp_PromotionAmount":""))));
            if(tampered)r.put("vnp_Amount","1");
            HttpResponse<String> response=mock(HttpResponse.class);when(response.statusCode()).thenReturn(200);when(response.body()).thenReturn(json.writeValueAsString(r));return response;
        });
    }
    static String body(HttpRequest req)throws Exception{var bytes=new ByteArrayOutputStream();var done=new CompletableFuture<String>();req.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>(){
        public void onSubscribe(Flow.Subscription s){s.request(Long.MAX_VALUE);}public void onNext(ByteBuffer b){byte[] data=new byte[b.remaining()];b.get(data);bytes.writeBytes(data);}public void onError(Throwable t){done.completeExceptionally(t);}public void onComplete(){done.complete(bytes.toString(StandardCharsets.UTF_8));}
    });return done.get(2,TimeUnit.SECONDS);}
    @Test void querySignsOriginalCreationDateAndVerifiesResponse(){var r=gateway.query("unique-request","original-reference","20261113094500");assertEquals("00",r.get("vnp_TransactionStatus"));assertEquals("20261113094500",requests.getFirst().get("vnp_TransactionDate"));assertEquals("2.1.0",requests.getFirst().get("vnp_Version"));}
    @Test void refundSignsPartialAndStableRequestWithOriginalLink(){command="refund";type="03";amount="1000000";gateway.refund("refund-id","original-reference","20261113094500","999",new java.math.BigDecimal("10000"),type,7);var r=requests.getFirst();assertEquals("1000000",r.get("vnp_Amount"));assertEquals("999",r.get("vnp_TransactionNo"));assertEquals("Refund refund-id",r.get("vnp_OrderInfo"));}
    @Test void forgedResponseNeverSucceeds(){tampered=true;assertEquals("PROVIDER_RESPONSE_INVALID",assertThrows(MarketplaceException.class,()->gateway.query("req","ref","20261113094500")).code);}
    @Test void networkTimeoutIsNotRetried()throws Exception{when(http.send(any(),any(HttpResponse.BodyHandler.class))).thenThrow(new HttpTimeoutException("test transport"));assertThrows(MarketplaceException.class,()->gateway.query("req","ref","20261113094500"));verify(http,times(1)).send(any(),any(HttpResponse.BodyHandler.class));}
    @Test void exactVndBoundsAndVietnamTime(){for(String invalid:List.of("0","-1","0.5","10000000000","9223372036854775807"))assertThrows(MarketplaceException.class,()->VnpayGateway.amount(new java.math.BigDecimal(invalid)));assertEquals("999999999900",VnpayGateway.amount(new java.math.BigDecimal("9999999999")));assertEquals("20261113070000",VnpayGateway.date(java.time.LocalDateTime.parse("2026-11-13T00:00:00")));}
    @Test void missingOriginalDateAndDisabledRefundAreGated(){assertThrows(MarketplaceException.class,()->gateway.query("req","ref",null));var disabled=new VnpayGateway(json,true,false,"TESTONLY","secret","127.0.0.1",http);assertThrows(MarketplaceException.class,()->disabled.refund("req","ref","20261113094500","999",java.math.BigDecimal.ONE,"02",7));verifyNoInteractions(http);}
    @Test void responseBodyIsBoundedAndCompletionWaitsForEof(){
        var subscriber=VnpayGateway.boundedResponse().apply(mock(HttpResponse.ResponseInfo.class));var subscription=mock(Flow.Subscription.class);subscriber.onSubscribe(subscription);
        subscriber.onNext(List.of(ByteBuffer.wrap("{}".getBytes(StandardCharsets.UTF_8))));assertFalse(subscriber.getBody().toCompletableFuture().isDone());subscriber.onComplete();assertEquals("{}",subscriber.getBody().toCompletableFuture().join());
        var oversized=VnpayGateway.boundedResponse().apply(mock(HttpResponse.ResponseInfo.class));var other=mock(Flow.Subscription.class);oversized.onSubscribe(other);oversized.onNext(List.of(ByteBuffer.allocate(65537)));verify(other).cancel();assertTrue(oversized.getBody().toCompletableFuture().isCompletedExceptionally());
    }
}
