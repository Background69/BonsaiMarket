package com.bonsaimarket.backend.auth;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.*;

@RestControllerAdvice(basePackages="com.bonsaimarket.backend.auth")
public class AuthErrors {
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<?> auth(AuthException e) {
        var builder=ResponseEntity.status(e.status).cacheControl(CacheControl.noStore());
        if(e.retryAfter>0) builder.header("Retry-After",Integer.toString(e.retryAfter));
        return builder.body(Map.of("code",e.code,"message",e.getMessage(),"retryAfterSeconds",e.retryAfter));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> invalid(MethodArgumentNotValidException e) {
        var fields=new LinkedHashMap<String,String>();
        e.getBindingResult().getFieldErrors().forEach(error-> {
            String label=Map.of("email","email","password","mật khẩu","confirmPassword","mật khẩu xác nhận","name","họ và tên","code","mã xác minh","provider","nhà cung cấp").getOrDefault(error.getField(),"thông tin");
            String message=switch(error.getCode()==null?"":error.getCode()) {
                case "NotBlank", "NotNull" -> "Vui lòng nhập "+label+".";
                case "Email" -> "Email không hợp lệ.";
                case "Size" -> "Độ dài "+label+" không hợp lệ.";
                case "Pattern" -> "Mã xác minh phải gồm 6 chữ số.";
                default -> "Thông tin không hợp lệ.";
            };
            fields.putIfAbsent(error.getField(),message);
        });
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(Map.of("code","VALIDATION_FAILED","message","Vui lòng kiểm tra thông tin đã nhập.","fields",fields));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> malformed() {return auth(new AuthException(400,"INVALID_REQUEST","Dữ liệu gửi lên không hợp lệ."));}
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> collision() {return auth(new AuthException(409,"ACCOUNT_CONFLICT","Email hoặc tài khoản liên kết đã được sử dụng. Vui lòng đăng nhập để kiểm tra."));}
}
