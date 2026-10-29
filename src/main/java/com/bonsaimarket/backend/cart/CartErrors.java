package com.bonsaimarket.backend.cart;

import com.bonsaimarket.backend.marketplace.MarketplaceException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Same JSON error shape as CheckoutErrors: {code, message[, fields]}, which the frontend already understands. */
@RestControllerAdvice(assignableTypes = CartController.class)
public class CartErrors {
    @ExceptionHandler(MarketplaceException.class)
    ResponseEntity<?> business(MarketplaceException e) {
        return ResponseEntity.status(e.status).cacheControl(CacheControl.noStore())
                .body(Map.of("code", e.code, "message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        var fields = new LinkedHashMap<String, String>();
        e.getBindingResult().getFieldErrors().forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(Map.of("code", "VALIDATION_FAILED",
                "message", "Vui lòng kiểm tra sản phẩm và số lượng.", "fields", fields));
    }

    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    ResponseEntity<?> malformed() {
        return business(MarketplaceException.invalid("Dữ liệu giỏ hàng không hợp lệ."));
    }

    @ExceptionHandler({org.springframework.dao.ConcurrencyFailureException.class,
            org.springframework.dao.DataIntegrityViolationException.class,
            jakarta.persistence.PessimisticLockException.class, jakarta.persistence.LockTimeoutException.class})
    ResponseEntity<?> conflict() {
        return business(new MarketplaceException(409, "CART_CONFLICT", "Giỏ hàng vừa thay đổi. Vui lòng tải lại và thử lại."));
    }
}