package com.bonsaimarket.backend.management;

import com.bonsaimarket.backend.marketplace.AuditPresentation;

import java.util.*;

import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class AdminAuditPresentation {
    public record Detail(String reason, String before, String after) {
    }

    private final ObjectMapper json;

    public AdminAuditPresentation(ObjectMapper json) {
        this.json = json;
    }

    private static final Map<String, String> STATES = Map.ofEntries(Map.entry("ACTIVE", "Đang hoạt động"), Map.entry("INACTIVE", "Đã đình chỉ"), Map.entry("REJECTED", "Đã hạn chế / từ chối"), Map.entry("ARCHIVED", "Đã gỡ / lưu trữ"), Map.entry("ENABLED", "Đang hoạt động"), Map.entry("LOCKED", "Đã khóa"), Map.entry("PENDING", "Đang chờ xử lý"), Map.entry("APPROVED", "Đã duyệt"), Map.entry("OPEN", "Mới gửi"), Map.entry("IN_REVIEW", "Đang xem xét"), Map.entry("RESOLVED", "Đã xử lý"), Map.entry("CLOSED", "Đã đóng"), Map.entry("DISMISSED", "Không đủ cơ sở"), Map.entry("CONFIRMED", "Đã xác nhận"), Map.entry("SHIPPING", "Đang giao"), Map.entry("DELIVERED", "Đã giao"), Map.entry("COMPLETED", "Hoàn thành"), Map.entry("CANCELLED", "Đã hủy"));

    static String state(String code) {
        return code == null ? null : STATES.getOrDefault(code, "Đã ghi nhận trạng thái");
    }

    public Detail detail(String action, String raw, String before, String after) {
        String reason = AdminHistory.LABELS.containsKey(action) && !action.equals("APPLICATION_SUBMITTED") ? raw : AuditPresentation.reason(action, raw);
        try {
            if (action.startsWith("ORDER_") && raw != null) {
                var data = json.readTree(raw);
                if (data.path("version").asInt() == 1 && action.equals("ORDER_" + data.path("to").asText()))
                    return new Detail(null, state(data.path("from").asText()), state(data.path("to").asText()));
            }
            if (action.startsWith("COMMUNITY_") && !action.equals("COMMUNITY_REPORTED") && raw != null) {
                var data = json.readTree(raw);
                if (data.has("previousState") && data.has("newState") && data.has("reason"))
                    return new Detail(data.path("reason").asText(), AuditPresentation.state(data.path("previousState").asText()), AuditPresentation.state(data.path("newState").asText()));
            }
        } catch (RuntimeException ignored) {/* Historical opaque payload remains private. */}
        return new Detail(reason, state(before), state(after));
    }

    public String action(String code) {
        String label = AdminHistory.label(code);
        if (!label.equals("Đã ghi nhận hoạt động quản trị")) return label;
        if (code.startsWith("ORDER_")) {
            String s = STATES.get(code.substring(6));
            if (s != null) return "Đơn hàng: " + s;
        }
        if (code.startsWith("REFUND_")) return switch (code) {
            case "REFUND_REQUESTED" -> "Đã lập hồ sơ hoàn tiền";
            case "REFUND_APPROVED" -> "Đã duyệt hồ sơ hoàn tiền nội bộ";
            case "REFUND_DENIED" -> "Đã từ chối hồ sơ hoàn tiền";
            default -> "Đã cập nhật tiến độ hồ sơ hoàn tiền";
        };
        return switch (code) {
            case "CATEGORY_CREATED" -> "Đã tạo danh mục";
            case "CATEGORY_UPDATED" -> "Đã cập nhật danh mục";
            case "CATEGORY_DELETED" -> "Đã xóa danh mục";
            case "REPORT_CREATED" -> "Đã gửi báo cáo vi phạm";
            case "CANCELLATION_REASON" -> "Đã ghi nhận lý do hủy đơn";
            default -> label;
        };
    }
}
