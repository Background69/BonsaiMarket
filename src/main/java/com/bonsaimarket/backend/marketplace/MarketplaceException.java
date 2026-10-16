package com.bonsaimarket.backend.marketplace;

public class MarketplaceException extends RuntimeException {
    public final int status;
    public final String code;
    public MarketplaceException(int status, String code, String message) { super(message); this.status = status; this.code = code; }
    public static MarketplaceException missing() { return new MarketplaceException(404, "NOT_FOUND", "Không tìm thấy dữ liệu."); }
    public static MarketplaceException invalid(String message) { return new MarketplaceException(400, "VALIDATION_FAILED", message); }
}
