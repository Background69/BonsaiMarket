package com.bonsaimarket.backend.payment;

import com.bonsaimarket.backend.marketplace.MarketplaceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.math.BigDecimal;

/** Gateway 2.1.0. Only the sandbox host is reachable; POSTs are never automatically retried. */
@Component
public class VnpayGateway {
    static final String API="https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";
    static final String QUERY_REQUEST="vnp_RequestId,vnp_Version,vnp_Command,vnp_TmnCode,vnp_TxnRef,vnp_TransactionDate,vnp_CreateDate,vnp_IpAddr,vnp_OrderInfo";
    static final String REFUND_REQUEST="vnp_RequestId,vnp_Version,vnp_Command,vnp_TmnCode,vnp_TransactionType,vnp_TxnRef,vnp_Amount,vnp_TransactionNo,vnp_TransactionDate,vnp_CreateBy,vnp_CreateDate,vnp_IpAddr,vnp_OrderInfo";
    static final String RESPONSE="vnp_ResponseId,vnp_Command,vnp_ResponseCode,vnp_Message,vnp_TmnCode,vnp_TxnRef,vnp_Amount,vnp_BankCode,vnp_PayDate,vnp_TransactionNo,vnp_TransactionType,vnp_TransactionStatus,vnp_OrderInfo";
    private final boolean enabled,refundEnabled;
    private final String merchant,secret,serverIp;
    private final ObjectMapper json;
    private final HttpClient http;
    @Autowired public VnpayGateway(ObjectMapper json,@Value("${vnpay.enabled:false}") boolean enabled,
        @Value("${vnpay.refund-enabled:false}") boolean refundEnabled,@Value("${vnpay.tmn-code:}") String merchant,
        @Value("${vnpay.hash-secret:}") String secret,@Value("${vnpay.server-ip:127.0.0.1}") String serverIp){
        this(json,enabled,refundEnabled,merchant,secret,serverIp,HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).followRedirects(HttpClient.Redirect.NEVER).build());
    }
    VnpayGateway(ObjectMapper json,boolean enabled,boolean refundEnabled,String merchant,String secret,String serverIp,HttpClient http){
        this.json=json;this.enabled=enabled;this.refundEnabled=refundEnabled;this.merchant=merchant;this.secret=secret;this.serverIp=serverIp;this.http=http;
    }
    public boolean available(){return enabled&&!merchant.isBlank()&&!secret.isBlank();}
    public boolean refundsAvailable(){return available()&&refundEnabled;}
    public static String date(LocalDateTime utc){return utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh")).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));}
    public static String amount(BigDecimal vnd){
        try{long n=vnd.longValueExact();if(n<1||n>9_999_999_999L)throw new ArithmeticException();return Long.toString(Math.multiplyExact(n,100));}
        catch(ArithmeticException|NullPointerException e){throw new MarketplaceException(422,"PAYMENT_AMOUNT_INVALID","Số tiền phải nguyên VND, dương và nằm trong giới hạn gateway.");}
    }
    public Map<String,String> query(String requestId,String reference,String originalDate){
        var b=base(requestId,"querydr",reference,originalDate,"Query "+reference);sign(b,QUERY_REQUEST);return call(b,RESPONSE+",vnp_PromotionCode,vnp_PromotionAmount");
    }
    public Map<String,String> queryRefund(String requestId,String reference,String originalDate,String refundTransactionNo){
        var b=base(requestId,"querydr",reference,originalDate,"Query "+reference);b.put("vnp_TransactionNo",refundTransactionNo);sign(b,QUERY_REQUEST);return call(b,RESPONSE+",vnp_PromotionCode,vnp_PromotionAmount");
    }
    public Map<String,String> refund(String requestId,String reference,String originalDate,String transactionNo,BigDecimal amount,String type,long admin){
        if(!refundsAvailable())throw new MarketplaceException(503,"REFUND_PROVIDER_NOT_CONFIGURED","Merchant sandbox chưa bật quyền hoàn tiền.");
        var b=base(requestId,"refund",reference,originalDate,"Refund "+requestId);
        b.put("vnp_TransactionType",type);b.put("vnp_TransactionNo",transactionNo);b.put("vnp_Amount",amount(amount));b.put("vnp_CreateBy","Admin "+admin);sign(b,REFUND_REQUEST);return call(b,RESPONSE);
    }
    private Map<String,String> base(String id,String command,String reference,String date,String info){
        if(!available())throw new MarketplaceException(503,"PAYMENT_NOT_CONFIGURED","VNPAY sandbox chưa được cấu hình.");
        if(date==null||!date.matches("[0-9]{14}"))throw new MarketplaceException(409,"PAYMENT_RECONCILIATION_REQUIRED","Thiếu thời điểm khởi tạo gốc; không tự suy diễn giao dịch lịch sử.");
        var b=new LinkedHashMap<String,String>();b.put("vnp_RequestId",id);b.put("vnp_Version","2.1.0");b.put("vnp_Command",command);b.put("vnp_TmnCode",merchant);b.put("vnp_TxnRef",reference);b.put("vnp_TransactionDate",date);b.put("vnp_CreateDate",date(LocalDateTime.now(ZoneOffset.UTC)));b.put("vnp_IpAddr",serverIp);b.put("vnp_OrderInfo",info);return b;
    }
    static String canonical(Map<String,String> b,String fields){return Arrays.stream(fields.split(",")).map(k->b.getOrDefault(k,"")).collect(java.util.stream.Collectors.joining("|"));}
    private void sign(Map<String,String> b,String fields){b.put("vnp_SecureHash",VNPayUtil.hmacSHA512(secret,canonical(b,fields)));}
    private Map<String,String> call(Map<String,String> b,String fields){
        try{
            var req=HttpRequest.newBuilder(URI.create(API)).timeout(Duration.ofSeconds(8)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(b))).build();
            // Read a bounded body even if the provider omits Content-Length.
            var response=http.send(req,boundedResponse());
            if(response.statusCode()!=200)throw unavailable();
            var node=json.reader().with(tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(response.body());
            if(node==null||!node.isObject())throw unavailable();
            var result=new LinkedHashMap<String,String>();node.properties().forEach(e->{if(e.getValue().isValueNode())result.put(e.getKey(),e.getValue().asText());});
            if(!VNPayUtil.equalHash(VNPayUtil.hmacSHA512(secret,canonical(result,fields)),result.get("vnp_SecureHash"))||!merchant.equals(result.get("vnp_TmnCode"))||!b.get("vnp_TxnRef").equals(result.get("vnp_TxnRef"))||!b.get("vnp_Command").equals(result.get("vnp_Command"))||result.getOrDefault("vnp_ResponseId","").isBlank()||!b.get("vnp_OrderInfo").equals(result.get("vnp_OrderInfo")))
                throw new MarketplaceException(502,"PROVIDER_RESPONSE_INVALID","Phản hồi VNPAY chưa xác minh được; cần đối soát, không gửi lại lệnh thu/hoàn.");
            if("refund".equals(b.get("vnp_Command"))&&(!b.get("vnp_Amount").equals(result.get("vnp_Amount"))||!b.get("vnp_TransactionType").equals(result.get("vnp_TransactionType"))))throw unavailable();
            return Collections.unmodifiableMap(result);
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw unavailable();}
        catch(MarketplaceException e){throw e;}
        catch(Exception e){throw unavailable();}
    }
    /** Complete getBody only after EOF, so HttpRequest's deadline also covers body delivery. */
    static HttpResponse.BodyHandler<String> boundedResponse(){return info->new HttpResponse.BodySubscriber<>(){
        final HttpResponse.BodySubscriber<String> delegate=HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);
        java.util.concurrent.Flow.Subscription subscription;long size;boolean ended;
        public java.util.concurrent.CompletionStage<String> getBody(){return delegate.getBody();}
        public void onSubscribe(java.util.concurrent.Flow.Subscription s){subscription=s;delegate.onSubscribe(s);}
        public void onNext(List<ByteBuffer> chunks){if(ended)return;for(var c:chunks)size+=c.remaining();if(size>65536){ended=true;subscription.cancel();delegate.onError(new java.io.IOException("Provider response too large"));}else delegate.onNext(chunks);}
        public void onError(Throwable e){if(!ended){ended=true;delegate.onError(e);}}
        public void onComplete(){if(!ended){ended=true;delegate.onComplete();}}
    };}
    private static MarketplaceException unavailable(){return new MarketplaceException(503,"PROVIDER_RECONCILIATION_REQUIRED","Chưa có kết quả VNPAY chắc chắn. Giữ hồ sơ chờ đối soát; không tự gửi lại.");}
}
