package com.bonsaimarket.backend.checkout;

import com.bonsaimarket.backend.marketplace.MarketplaceException;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(assignableTypes = CheckoutController.class)
public class CheckoutErrors {
    @ExceptionHandler(MarketplaceException.class)
    ResponseEntity<?> business(MarketplaceException e) {
        return ResponseEntity.status(e.status).cacheControl(CacheControl.noStore()).body(Map.of("code", e.code, "message", e.getMessage()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        var fields = new LinkedHashMap<String, String>();
        e.getBindingResult().getFieldErrors().forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(Map.of("code", "VALIDATION_FAILED",
                "message", "Vui lòng kiểm tra sản phẩm, địa chỉ và số lượng.", "fields", fields));
    }
    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    ResponseEntity<?> malformed() { return business(MarketplaceException.invalid("Dữ liệu đặt hàng không hợp lệ.")); }
    @ExceptionHandler({org.springframework.dao.ConcurrencyFailureException.class, org.springframework.dao.DataIntegrityViolationException.class,
            jakarta.persistence.PessimisticLockException.class, jakarta.persistence.LockTimeoutException.class})
    ResponseEntity<?> conflict() { return business(new MarketplaceException(409, "CHECKOUT_CONFLICT", "Dữ liệu vừa thay đổi. Vui lòng thử lại.")); }
}
