package com.bonsaimarket.backend.adminai;

import com.bonsaimarket.backend.ai.AiException;
import com.bonsaimarket.backend.auth.AuthException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.Map;

@RestControllerAdvice(assignableTypes=AdminAiController.class)
public class AdminAiErrors {
    @ExceptionHandler(AiException.class)
    public ResponseEntity<Map<String,String>> ai(AiException e) {return error(e.status(),e.code(),e.getMessage());}
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String,String>> auth(AuthException e) {return error(e.status,e.code,e.getMessage());}
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String,String>> invalid() {return error(400,"ADMIN_AI_INVALID_QUERY","Cần JSON hợp lệ với trường message và khoảng ngày tùy chọn.");}
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,String>> unexpected() {return error(503,"ADMIN_AI_UNAVAILABLE","Trợ lý hiện chưa sẵn sàng. Vui lòng thử lại sau.");}
    private ResponseEntity<Map<String,String>> error(int status,String code,String message) {
        var builder=ResponseEntity.status(status).header("Cache-Control","no-store");
        if(status==429) builder.header("Retry-After","60");
        return builder.body(Map.of("code",code,"message",message));
    }
}
