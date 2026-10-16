package com.bonsaimarket.backend.auth;

public class AuthException extends RuntimeException {
    public final int status;
    public final String code;
    public final int retryAfter;
    public AuthException(int status, String code, String message) { this(status, code, message, 0); }
    public AuthException(int status, String code, String message, int retryAfter) {
        super(message); this.status = status; this.code = code; this.retryAfter = retryAfter;
    }
}
