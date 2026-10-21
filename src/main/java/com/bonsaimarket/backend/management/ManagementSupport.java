package com.bonsaimarket.backend.management;

import com.bonsaimarket.backend.marketplace.MarketplaceException;
import com.bonsaimarket.backend.order.OrderStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Shared parsing and time rules for the admin/seller statistics and order-management APIs. */
final class ManagementSupport {
    /** Business days are Vietnamese calendar days; timestamps in MySQL are stored as UTC. */
    static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    static final int PAGE_SIZE = 20;
    static final int MAX_RANGE_DAYS = 366;
    /** Revenue is the goods value (subtotal, shipping excluded) of delivered or completed orders. */
    static final List<OrderStatus> REVENUE_STATUSES = List.of(OrderStatus.DELIVERED, OrderStatus.COMPLETED);

    private ManagementSupport() { }

    /** Inclusive local-date range plus its UTC half-open bounds [start, end). */
    record Range(LocalDate from, LocalDate to, LocalDateTime start, LocalDateTime end) { }

    static LocalDate date(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value.strip());
        } catch (DateTimeParseException e) {
            throw MarketplaceException.invalid("Ngày không hợp lệ. Vui lòng chọn lại.");
        }
    }

    private static LocalDateTime utc(LocalDate day) {
        return day.atStartOfDay(BUSINESS_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /** Statistics range: defaults to the last 30 days ending today. */
    static Range range(String fromText, String toText, Clock clock) {
        LocalDate today = LocalDate.ofInstant(clock.instant(), BUSINESS_ZONE);
        LocalDate to = date(toText);
        LocalDate from = date(fromText);
        if (to == null) to = today;
        if (from == null) from = to.minusDays(29);
        if (from.isAfter(to)) throw MarketplaceException.invalid("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_RANGE_DAYS) throw MarketplaceException.invalid("Khoảng thống kê tối đa 366 ngày.");
        return new Range(from, to, utc(from), utc(to.plusDays(1)));
    }

    static LocalDateTime startOf(LocalDate day) {
        return utc(day);
    }

    static LocalDateTime endOf(LocalDate day) {
        return utc(day.plusDays(1));
    }

    static LocalDate businessDay(LocalDateTime utcTime) {
        return utcTime.atZone(ZoneOffset.UTC).withZoneSameInstant(BUSINESS_ZONE).toLocalDate();
    }

    static String iso(LocalDateTime utcTime) {
        return utcTime == null ? null : DateTimeFormatter.ISO_INSTANT.format(utcTime.toInstant(ZoneOffset.UTC));
    }

    static OrderStatus status(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return OrderStatus.valueOf(value.strip().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw MarketplaceException.invalid("Trạng thái đơn hàng không hợp lệ.");
        }
    }

    /** Lower-cased LIKE pattern with wildcard characters removed; null when there is nothing to search. */
    static String like(String value) {
        if (value == null) return null;
        String text = value.strip();
        if (text.length() > 100) text = text.substring(0, 100);
        text = text.replace("%", "").replace("_", "").replace("\\", "").strip();
        return text.isEmpty() ? null : "%" + text.toLowerCase(java.util.Locale.ROOT) + "%";
    }
}
