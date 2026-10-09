package com.bonsaimarket.backend.ai;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(assignableTypes = AiChatController.class)
public class AiErrorHandler {
    @ExceptionHandler(AiException.class)
    public ResponseEntity<Map<String, String>> aiError(AiException error) {
        var response = ResponseEntity.status(error.status()).header("Cache-Control", "no-store");
        if (error.status() == 429) response.header("Retry-After", "60");
        return response.body(Map.of("code", error.code(), "error", error.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, String>> invalid() {
        return ResponseEntity.badRequest().body(Map.of("code", "INVALID_MESSAGE",
                "error", "Câu hỏi phải có từ 1 đến 2.000 ký tự và đúng định dạng JSON."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> unexpected() {
        return ResponseEntity.status(503).body(Map.of("code", "AI_UNAVAILABLE",
                "error", "Trợ lý AI hiện chưa sẵn sàng."));
    }
}
