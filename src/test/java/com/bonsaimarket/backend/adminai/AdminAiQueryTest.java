package com.bonsaimarket.backend.adminai;

import com.bonsaimarket.backend.ai.AiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static com.bonsaimarket.backend.adminai.AdminAiQuery.Operation.*;

class AdminAiQueryTest {
    final ObjectMapper json=new ObjectMapper();
    final Clock clock=Clock.fixed(Instant.parse("2026-10-20T01:00:00Z"),ZoneOffset.UTC);
    AdminAiQuery parse(String message) {return AdminAiQuery.parse(json.valueToTree(Map.of("message",message)),clock);}
    @Test void supportedVietnameseIntentsAreDeterministic() {
        assertEquals(OVERVIEW,parse("Hiện có bao nhiêu sản phẩm hết hàng?").operation());
        assertEquals(PRODUCT_QUALITY,parse("Kiểm tra chất lượng nội dung sản phẩm").operation());
        assertEquals(STORES,parse("Gian hàng nào có nhiều sản phẩm nhất?").operation());
        assertEquals(CATEGORIES,parse("Danh mục nào có nhiều sản phẩm nhất?").operation());
        assertEquals(REPORTS,parse("Tóm tắt báo cáo chờ xem xét").operation());
    }
    @Test void dateRangeUsesLocalDaysConvertedToUtcWithExclusiveEnd() {
        var q=parse("Từ 01/10/2026 đến 15/10/2026 có bao nhiêu sản phẩm mới?");
        assertEquals(NEW_PRODUCTS,q.operation());assertEquals(LocalDateTime.parse("2026-09-30T17:00:00"),q.startUtc());
        assertEquals(LocalDateTime.parse("2026-10-15T17:00:00"),q.endUtc());
        var recent=parse("Trong bảy ngày qua có bao nhiêu gian hàng mới?");
        assertEquals(NEW_STORES,recent.operation());assertEquals(LocalDate.parse("2026-10-14"),recent.from());
        assertEquals(LocalDate.parse("2026-10-01"),parse("Tháng này có bao nhiêu sản phẩm mới?").from());
    }
    @ParameterizedTest @ValueSource(strings={"Từ 01/10 đến 15/10 có bao nhiêu sản phẩm mới?","Có bao nhiêu sản phẩm mới?","Sản phẩm từ 2026-10-10 đến 2026-10-01","Sản phẩm từ 2026-02-30 đến 2026-03-01","Sản phẩm trong 91 ngày qua","Sản phẩm từ 2026-10-01 đến 2026-12-01","Gian hàng đó thế nào?","SELECT * FROM users","Đơn hàng tháng này","Xóa sản phẩm","Email người dùng"})
    void unsupportedAmbiguousOrInvalidRequestsFailBeforeDataAccess(String message) {
        assertThrows(AiException.class,()->parse(message));
    }
    @Test void rejectsSpoofedActorUnknownFieldsAndConflictingDates() {
        for(var body:java.util.List.of(Map.of("message","Tổng quan","role","ADMIN"),Map.of("message","Tổng quan","userId",1),Map.of("message","Sản phẩm từ 2026-10-01 đến 2026-10-05","from","2026-10-02","to","2026-10-05"),Map.of("message","Tổng quan","from","2026-10-01"),Map.of("message"," "),Map.of("message","a".repeat(2001))))
            assertThrows(AiException.class,()->AdminAiQuery.parse(json.valueToTree(body),clock));
        assertEquals(NEW_PRODUCTS,AdminAiQuery.parse(json.valueToTree(Map.of("message","Đếm sản phẩm","from","2026-10-01","to","2026-10-05")),clock).operation());
    }
}
