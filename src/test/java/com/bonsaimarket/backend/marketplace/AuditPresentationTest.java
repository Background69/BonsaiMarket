package com.bonsaimarket.backend.marketplace;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuditPresentationTest {
    @Test void onlyKnownSerializedStatesArePresented() {
        assertEquals("Đã ẩn · Đã đóng thảo luận · Đã ghim",AuditPresentation.state("HIDDEN; locked=true; pinned=true; revision=42"));
        assertEquals("Chưa có thông tin trạng thái",AuditPresentation.state(null));
        assertEquals("Chưa có thông tin trạng thái",AuditPresentation.state("bodySHA256=secret"));
        assertEquals("Chưa có thông tin trạng thái",AuditPresentation.state("PUBLIC; locked=false; pinned=false; revision=1; secret=oops"));
    }
    @Test void unknownEventsDoNotLeakCodesAndUserReasonsArePreserved() {
        assertEquals("Đã xử lý báo cáo",AuditPresentation.action("REPORT_RESOLVED"));
        assertEquals("Đã ghi nhận hoạt động quản trị",AuditPresentation.action("FUTURE_INTERNAL_EVENT"));
        assertNull(AuditPresentation.reason("COMMUNITY_REPORTED","COMMUNITY_POST:7; bodySHA256=x"));
        assertNull(AuditPresentation.reason("FUTURE_INTERNAL_EVENT","COMMUNITY_POST:7; bodySHA256=x"));
        assertNull(AuditPresentation.reason(null,"Internal legacy payload"));
        String actualReason="Tác giả dùng từ test trong hướng dẫn; không vi phạm.";
        assertEquals(actualReason,AuditPresentation.reason("REPORT_DISMISSED",actualReason));
    }
}
