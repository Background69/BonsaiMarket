package com.bonsaimarket.backend.payment;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/account/payments")
public class PaymentController {
    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping("/init")
    public ResponseEntity<PaymentDtos.PaymentInitResponse> initPayment(
            @AuthenticationPrincipal AuthPrincipal user,
            @Valid @RequestBody PaymentDtos.CreatePaymentRequest request,
            HttpServletRequest servletRequest) {
        return response(service.createPayment(user.id(), request, servletRequest.getRemoteAddr()));
    }

    @PostMapping("/vnpay-callback")
    public ResponseEntity<PaymentDtos.PaymentCallbackResult> handleVnpayCallback(
            @AuthenticationPrincipal AuthPrincipal user,
            @RequestBody Map<String, String> params) {
        return response(service.inspectReturn(user.id(), params));
    }
    @GetMapping("/configuration")
    public ResponseEntity<?> configuration(){return response(Map.of("vnpayAvailable",service.available(),"environment","SANDBOX"));}
    @PostMapping("/vnpay/{code}/reconcile")
    public ResponseEntity<?> reconcile(@AuthenticationPrincipal AuthPrincipal user,@PathVariable String code){return response(service.reconcile(user.id(),code));}

    @PostMapping("/mock-confirm/{txnCode}")
    public ResponseEntity<PaymentDtos.PaymentCallbackResult> mockConfirm(
            @AuthenticationPrincipal AuthPrincipal user,
            @PathVariable String txnCode) {
        return response(service.mockConfirm(user.id(), txnCode));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentDtos.TransactionView>> getOrderTransactions(
            @AuthenticationPrincipal AuthPrincipal user,
            @PathVariable Long orderId) {
        return response(service.getTransactionsForOrder(user.id(), orderId));
    }

    @GetMapping("/banking/{orderId}")
    public ResponseEntity<PaymentDtos.BankTransferInfo> getBankTransferInfo(
            @AuthenticationPrincipal AuthPrincipal user,
            @PathVariable Long orderId) {
        return response(service.getBankTransferInfo(user.id(), orderId));
    }

    private <T> ResponseEntity<T> response(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}

@RestController
class PaymentNotificationController {
    private final PaymentService service;
    PaymentNotificationController(PaymentService service) { this.service=service; }
    @GetMapping("/api/payments/vnpay/ipn")
    public ResponseEntity<Map<String,String>> notify(@RequestParam org.springframework.util.MultiValueMap<String,String> values) {
        String code="00", message="Confirm Success";
        try {
            if(values.values().stream().anyMatch(v->v.size()!=1))throw new com.bonsaimarket.backend.marketplace.MarketplaceException(400,"INVALID_SIGNATURE","Ambiguous parameters");
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.ipn(values.toSingleValueMap()));
        }
        catch (com.bonsaimarket.backend.marketplace.MarketplaceException e) {
            code=switch(e.code){case "INVALID_SIGNATURE"->"97";case "NOT_FOUND"->"01";case "PAYMENT_AMOUNT_MISMATCH"->"04";default->"99";};
            message="Cannot confirm";
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("RspCode",code,"Message",message));
    }
}
