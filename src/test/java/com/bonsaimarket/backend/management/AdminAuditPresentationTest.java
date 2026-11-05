package com.bonsaimarket.backend.management;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class AdminAuditPresentationTest {
    final AdminAuditPresentation view = new AdminAuditPresentation(new ObjectMapper());

    @Test
    void communityStateProjectionUsesKnownContract() {
        var d = view.detail("COMMUNITY_HIDE", "{\"previousState\":\"PUBLIC; locked=false; pinned=false; revision=2\",\"newState\":\"HIDDEN; locked=false; pinned=false; revision=3\",\"reason\":\"Vi phạm quy định\"}", null, null);
        assertEquals("Vi phạm quy định", d.reason());
        assertTrue(d.before().startsWith("Công khai"));
        assertTrue(d.after().startsWith("Đã ẩn"));
        assertFalse(d.before().contains("revision"));
    }

    @Test
    void orderProjectionNeverLeaksJsonOrUnknownEnums() {
        var d = view.detail("ORDER_CANCELLED", "{\"version\":1,\"from\":\"PENDING\",\"to\":\"CANCELLED\",\"actorRole\":\"ADMIN\"}", null, null);
        assertNull(d.reason());
        assertEquals("Đã hủy", d.after());
        assertFalse(view.action("ORDER_CANCELLED").contains("CANCELLED"));
    }

    @Test
    void rawIntegrityPayloadIsNotAHumanReason() {
        var d = view.detail("COMMUNITY_REPORTED", "bodySHA256=123; revision=2", null, null);
        assertNull(d.reason());
        assertNull(d.before());
        assertNull(d.after());
    }

    @Test
    void newStatesKeepHumanReasonAndLegacyMissingState() {
        var d = view.detail("STORE_SUSPEND", "Chờ kiểm tra", "ACTIVE", "INACTIVE");
        assertEquals("Chờ kiểm tra", d.reason());
        assertEquals("Đã đình chỉ", d.after());
        assertNull(view.detail("APPLICATION_SUBMITTED", "", null, null).after());
    }
}
