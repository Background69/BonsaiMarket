package com.bonsaimarket.backend.marketplace;

import java.util.Map;
import java.util.regex.Pattern;

/** Business history projections. Original integrity evidence stays in marketplace_audit. */
public final class AuditPresentation {
    private AuditPresentation() {}
    private static final Map<String,String> ACTIONS = Map.ofEntries(
        Map.entry("COMMUNITY_REPORTED", "Đã gửi báo cáo nội dung"),
        Map.entry("COMMUNITY_HIDE", "Đã ẩn nội dung"), Map.entry("COMMUNITY_RESTORE", "Đã khôi phục nội dung"),
        Map.entry("COMMUNITY_REMOVE", "Đã gỡ nội dung"), Map.entry("COMMUNITY_PIN", "Đã ghim bài viết"),
        Map.entry("COMMUNITY_UNPIN", "Đã bỏ ghim bài viết"), Map.entry("COMMUNITY_LOCK", "Đã đóng thảo luận"),
        Map.entry("COMMUNITY_UNLOCK", "Đã mở lại thảo luận"), Map.entry("REPORT_IN_REVIEW", "Đang xem xét báo cáo"),
        Map.entry("REPORT_RESOLVED", "Đã xử lý báo cáo"), Map.entry("REPORT_DISMISSED", "Đã bác bỏ báo cáo"));
    private static final Pattern STATE = Pattern.compile("^(PUBLIC|HIDDEN|REMOVED); locked=(true|false); pinned=(true|false); revision=\\d+$");
    public static String action(String code) {return ACTIONS.getOrDefault(code == null ? "" : code, "Đã ghi nhận hoạt động quản trị");}
    public static String reason(String action, String storedReason) {
        // Only these report decisions have a human-entered reason in the current contract.
        // Other events may store integrity snapshots or future internal payloads.
        return java.util.Set.of("REPORT_IN_REVIEW","REPORT_RESOLVED","REPORT_DISMISSED")
            .contains(action == null ? "" : action) ? storedReason : null;
    }
    public static String state(String storedState) {
        if (storedState == null) return "Chưa có thông tin trạng thái";
        var match = STATE.matcher(storedState);
        if (!match.matches()) return "Chưa có thông tin trạng thái";
        String visibility = switch (match.group(1)) {
            case "PUBLIC" -> "Công khai"; case "HIDDEN" -> "Đã ẩn"; default -> "Đã gỡ";
        };
        return visibility + ("true".equals(match.group(2)) ? " · Đã đóng thảo luận" : " · Thảo luận đang mở")
            + ("true".equals(match.group(3)) ? " · Đã ghim" : "");
    }
}
