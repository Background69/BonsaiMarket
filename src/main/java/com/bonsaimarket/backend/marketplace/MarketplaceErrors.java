package com.bonsaimarket.backend.marketplace;

import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(basePackages={"com.bonsaimarket.backend.community", "com.bonsaimarket.backend.address", "com.bonsaimarket.backend.inventory", "com.bonsaimarket.backend.category", "com.bonsaimarket.backend.profile", "com.bonsaimarket.backend.seller.application", "com.bonsaimarket.backend.store", "com.bonsaimarket.backend.media", "com.bonsaimarket.backend.report", "com.bonsaimarket.backend.product", "com.bonsaimarket.backend.management"})
public class MarketplaceErrors {
    @ExceptionHandler(MarketplaceException.class)
    ResponseEntity<?> business(MarketplaceException e) { return ResponseEntity.status(e.status).cacheControl(CacheControl.noStore()).body(Map.of("code",e.code,"message",e.getMessage())); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        var fields = new LinkedHashMap<String,String>();
        e.getBindingResult().getFieldErrors().forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(Map.of("code","VALIDATION_FAILED","message","Vui lòng kiểm tra thông tin đã nhập.","fields",fields));
    }
    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class, org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    ResponseEntity<?> malformed() { return business(MarketplaceException.invalid("Dữ liệu gửi lên không hợp lệ.")); }
    @ExceptionHandler({org.springframework.dao.ConcurrencyFailureException.class, org.springframework.dao.DataIntegrityViolationException.class,
        jakarta.persistence.PessimisticLockException.class, jakarta.persistence.LockTimeoutException.class})
    ResponseEntity<?> conflict() { return business(new MarketplaceException(409,"DATA_CONFLICT","Dữ liệu vừa thay đổi. Vui lòng tải lại và thử lại.")); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<?> unexpected() { return business(new MarketplaceException(500,"INTERNAL_ERROR","Không thể xử lý yêu cầu lúc này.")); }
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ResponseEntity<?> uploadSize() {return business(new MarketplaceException(413,"IMAGE_TOO_LARGE","Ảnh vượt giới hạn dung lượng."));}
    @ExceptionHandler({org.springframework.web.multipart.support.MissingServletRequestPartException.class,org.springframework.web.bind.MissingServletRequestParameterException.class})
    ResponseEntity<?> missingInput() {return business(MarketplaceException.invalid("Thiếu dữ liệu bắt buộc."));}
}
