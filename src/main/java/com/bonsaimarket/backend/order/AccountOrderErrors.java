package com.bonsaimarket.backend.order;

import com.bonsaimarket.backend.marketplace.MarketplaceException;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = AccountOrderController.class)
public class AccountOrderErrors {
    @ExceptionHandler(MarketplaceException.class)
    ResponseEntity<?> business(MarketplaceException e) {
        return ResponseEntity.status(e.status).cacheControl(CacheControl.noStore()).body(Map.of("code", e.code, "message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        var fields = new LinkedHashMap<String, String>();
        e.getBindingResult().getFieldErrors().forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(Map.of(
                "code", "VALIDATION_FAILED",
                "message", "Yêu cầu không hợp lệ.",
                "fields", fields
        ));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<?> fallback(Exception e) {
        return ResponseEntity.internalServerError().cacheControl(CacheControl.noStore()).body(Map.of(
                "code", "ORDER_ERROR",
                "message", "Có lỗi xảy ra: " + (e.getMessage() != null ? e.getMessage() : "")
        ));
    }
}
