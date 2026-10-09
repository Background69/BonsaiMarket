package com.bonsaimarket.backend.ai;

public class AiException extends RuntimeException {
    private final int status;
    private final String code;

    public AiException(int status, String code, String safeMessage) {
        super(safeMessage); // Never keep provider bodies, keys or underlying exceptions.
        this.status = status;
        this.code = code;
    }

    public int status() { return status; }
    public String code() { return code; }
}
