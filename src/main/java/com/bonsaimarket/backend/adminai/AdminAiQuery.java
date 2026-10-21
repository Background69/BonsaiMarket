package com.bonsaimarket.backend.adminai;

import com.bonsaimarket.backend.ai.AiException;
import tools.jackson.databind.JsonNode;
import java.text.Normalizer;
import java.time.*;
import java.time.format.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;

/** Closed intent routing. User text is never a query fragment or forwarded to Gemini. */
public record AdminAiQuery(Operation operation, LocalDate from, LocalDate to) {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public enum Operation { OVERVIEW, PRODUCT_QUALITY, STORES, STORE_COMPLETENESS, CATEGORIES, REPORTS, NEW_PRODUCTS, NEW_STORES }
    private static final Pattern DATES = Pattern.compile("(?<![0-9])([0-9]{4}-[0-9]{2}-[0-9]{2}|[0-9]{2}/[0-9]{2}/[0-9]{4})(?![0-9])");
    private static final DateTimeFormatter VI_DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    public AdminAiQuery {
        if(operation==null) throw invalid("Loại phân tích không hợp lệ.");
        boolean dated=operation==Operation.NEW_PRODUCTS || operation==Operation.NEW_STORES;
        if(dated && (from==null || to==null || to.isBefore(from) || ChronoUnit.DAYS.between(from,to)>=90)
            || !dated && (from!=null || to!=null)) throw invalid("Khoảng ngày không hợp lệ cho loại phân tích.");
    }

    public static AdminAiQuery parse(JsonNode body, Clock clock) {
        if (body == null || !body.isObject() || !Set.of("message", "from", "to").containsAll(body.propertyNames())) throw invalid("Chỉ chấp nhận message và khoảng ngày from/to.");
        var value = body.path("message");
        if (!value.isString() || value.asText().isBlank() || value.asText().length() > 2000)
            throw invalid("Câu hỏi phải có từ 1 đến 2.000 ký tự.");
        String text = normalize(value.asText());
        if (text.matches(".*(?:(?:gian hang|san pham|danh muc|bao cao) (?:nay|do)\\b|#[0-9]+|nhu tren|tiep tuc).*"))
            throw new AiException(422, "ADMIN_AI_CLARIFY", "Mỗi lượt hỏi độc lập. Hiện trợ lý hỗ trợ tổng hợp theo phạm vi, chưa phân tích riêng một mã bản ghi; hãy nêu rõ loại tổng hợp và khoảng ngày nếu cần.");
        if (text.matches(".*\\b(xoa|khoa|duyet|phe duyet|tu choi|hoan tien|huy don|doi role|doi quyen|sql|execute|delete|update|insert|drop|ignore previous|api key|mat khau|otp|token)\\b.*"))
            throw new AiException(422, "ADMIN_AI_READ_ONLY", "Trợ lý chỉ đọc và phân tích dữ liệu; không thực hiện thao tác quản trị hoặc truy xuất bí mật.");
        if (text.matches(".*\\b(don hang|doanh thu|thanh toan|refund|payment|order|nguoi dung|tai khoan|email|so dien thoai|dia chi)\\b.*"))
            throw new AiException(422, "ADMIN_AI_UNSUPPORTED", "Chưa hỗ trợ phân tích người dùng, đơn hàng, doanh thu hoặc thanh toán. Hãy dùng mục quản trị tương ứng khi có hợp đồng dữ liệu.");
        LocalDate today = LocalDate.now(clock.withZone(ZONE)), from = null, to = null;
        boolean explicit = body.has("from") || body.has("to");
        var dates = new ArrayList<LocalDate>();
        var matcher = DATES.matcher(text);
        while (matcher.find()) dates.add(date(matcher.group(1)));
        if (explicit) {
            if (!body.path("from").isString() || !body.path("to").isString()) throw invalid("Cần đủ from và to theo YYYY-MM-DD.");
            from = date(body.path("from").asText()); to = date(body.path("to").asText());
            if (!dates.isEmpty() && (dates.size() != 2 || !dates.get(0).equals(from) || !dates.get(1).equals(to))) throw invalid("Khoảng ngày trong câu hỏi và bộ lọc phải trùng nhau.");
        } else if (!dates.isEmpty()) {
            if (dates.size() != 2) throw invalid("Cần ngày bắt đầu và kết thúc, gồm cả năm.");
            from = dates.get(0); to = dates.get(1);
        } else if (text.contains("thang nay")) { from = today.withDayOfMonth(1); to = today; }
        else {
            var days = Pattern.compile("\\b([0-9]{1,3}) ngay (qua|gan day)\\b").matcher(text);
            if (days.find()) { int n = Integer.parseInt(days.group(1)); if (n < 1 || n > 90) throw invalid("Khoảng ngày tối đa 90 ngày."); from = today.minusDays(n - 1); to = today; }
            else if (text.contains("bay ngay qua")) { from = today.minusDays(6); to = today; }
            else if (text.matches(".*(\\d{1,2}/\\d{1,2}|tuan nay|thang truoc|hom qua|tu ngay|den ngay).*")) throw invalid("Hãy ghi đủ hai ngày YYYY-MM-DD hoặc DD/MM/YYYY; có thể dùng “7 ngày qua” hoặc “tháng này”.");
        }
        if (from != null && (to.isBefore(from) || to.isAfter(today) || ChronoUnit.DAYS.between(from, to) >= 90)) throw invalid("Khoảng ngày phải hợp lệ, không ở tương lai và tối đa 90 ngày (gồm hai đầu).");
        boolean store = text.contains("gian hang") || text.contains("cua hang");
        Operation op;
        if (text.contains("bao cao")) op = Operation.REPORTS;
        else if (store && from != null) op = Operation.NEW_STORES;
        else if (from != null && text.contains("san pham")) op = Operation.NEW_PRODUCTS;
        else if (store) op = text.matches(".*(thieu|ho so|gioi thieu|banner).*" ) ? Operation.STORE_COMPLETENESS : Operation.STORES;
        else if (text.matches(".*(chat luong|thieu|qua ngan|thong so|mo ta|hinh anh|anh san pham).*")) op = Operation.PRODUCT_QUALITY;
        else if (text.contains("danh muc")) op = Operation.CATEGORIES;
        else if (text.matches(".*(tong quan|tong hop|tom tat|hoat dong|san pham|ton kho|het hang|con hang).*")) op = Operation.OVERVIEW;
        else throw new AiException(422, "ADMIN_AI_CLARIFY", "Hãy nêu rõ: tổng quan sản phẩm, chất lượng nội dung, gian hàng, danh mục, báo cáo hoặc sản phẩm/gian hàng mới trong khoảng ngày.");
        if (from != null && op != Operation.NEW_PRODUCTS && op != Operation.NEW_STORES) throw invalid("Bộ lọc ngày hiện chỉ hỗ trợ ngày tạo sản phẩm hoặc gian hàng.");
        if (from == null && text.contains("moi") && (store || text.contains("san pham"))) throw invalid("Hãy nêu khoảng ngày để đếm sản phẩm/gian hàng mới, ví dụ “7 ngày qua”.");
        return new AdminAiQuery(op, from, to);
    }
    public LocalDateTime startUtc() { return from.atStartOfDay(ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime(); }
    public LocalDateTime endUtc() { return to.plusDays(1).atStartOfDay(ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime(); }
    public String range() { return from == null ? null : from + " → " + to + " (ngày tạo, Asia/Ho_Chi_Minh; gồm hai đầu)"; }
    private static LocalDate date(String value) {
        try { return value.contains("/") ? LocalDate.parse(value, VI_DATE) : LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE); }
        catch (DateTimeException e) { throw invalid("Ngày không hợp lệ; dùng YYYY-MM-DD hoặc DD/MM/YYYY."); }
    }
    static String normalize(String s) { return Normalizer.normalize(s.toLowerCase(Locale.ROOT).replace('đ', 'd'), Normalizer.Form.NFD).replaceAll("\\p{M}", "").replaceAll("\\s+", " ").strip(); }
    static AiException invalid(String message) { return new AiException(400, "ADMIN_AI_INVALID_QUERY", message); }
}
