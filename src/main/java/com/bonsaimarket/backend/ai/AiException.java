package com.bonsaimarket.backend.ai;

public class AiException extends RuntimeException {
    private final int status;
    private final String code;

    public AiException(int status, String code, String safeMessage) {
        super(safeMessage); // Không giữ nội dung phản hồi, khóa hay exception gốc của nhà cung cấp.
        this.status = status;
        this.code = code;
    }

    public int status() { return status; }
    public String code() { return code; }
}
