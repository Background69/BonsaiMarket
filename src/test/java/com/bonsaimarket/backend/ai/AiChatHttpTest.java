package com.bonsaimarket.backend.ai;

import com.bonsaimarket.backend.category.CategoryRepository;
import com.bonsaimarket.backend.product.ProductRepository;
import com.bonsaimarket.backend.product.ProductStatus;
import com.bonsaimarket.backend.store.StoreRepository;
import com.bonsaimarket.backend.store.StoreStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.autoconfigure.exclude="
        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
        + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration,"
        + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
        + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration")
@AutoConfigureMockMvc
class AiChatHttpTest {
    @Autowired MockMvc mvc;
    @MockitoBean ProductRepository products;
    @MockitoBean CategoryRepository categories;
    @MockitoBean StoreRepository stores;
    @MockitoBean AiProviderClient provider;
    private static final AtomicInteger peer = new AtomicInteger();

    private MockHttpServletRequestBuilder chat(String body) {
        return post("/api/ai/chat").contentType("application/json").content(body)
                .with(r -> { r.setRemoteAddr("test-peer-" + peer.incrementAndGet()); return r; });
    }

    @Test void invalidInputIsRejectedBeforeProvider() throws Exception {
        for (String body : List.of("{}", "{\"message\":null}", "{\"message\":\" \"}", "{\"message\":\"" + "x".repeat(2001) + "\"}", "not json")) {
            mvc.perform(chat(body)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_MESSAGE"));
        }
        verifyNoInteractions(provider);
    }

    @Test void onlyExactAiPostIsPublicAndMessagesAreTrimmed() throws Exception {
        when(provider.answer(eq("Cách tưới bonsai"), anyString(), eq(false))).thenReturn("Kiểm tra độ ẩm.");
        mvc.perform(chat("{\"message\":\"  Cách tưới bonsai  \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.answer").value("Kiểm tra độ ẩm."))
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/api/ai/chat")).andExpect(status().isForbidden());
        mvc.perform(post("/api/ai/other").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/products").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());
    }

    @Test void disabledAndProviderErrorsUseSafeJson() throws Exception {
        for (int status : new int[]{503, 504, 429, 502}) {
            doThrow(new AiException(status, "SAFE_ERROR", "Trợ lý chưa sẵn sàng.")).when(provider).requireReady();
            mvc.perform(chat("{\"message\":\"Cách tưới bonsai\"}"))
                    .andExpect(status().is(status)).andExpect(jsonPath("$.code").value("SAFE_ERROR"))
                    .andExpect(jsonPath("$.error").value("Trợ lý chưa sẵn sàng."));
        }
    }

    @Test void bodyLimitsApplyToDeclaredAndChunkedRequests() throws Exception {
        mvc.perform(chat("x".repeat(16385))).andExpect(status().isPayloadTooLarge());
        mvc.perform(chat("x".repeat(16385)).with(r -> {
            r.setRemoteAddr("chunked-peer");
            return new org.springframework.mock.web.MockHttpServletRequest(r.getServletContext()) {
                { setMethod("POST"); setRequestURI("/api/ai/chat"); setContentType("application/json"); setContent("x".repeat(16385).getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
                @Override public long getContentLengthLong() { return -1; }
                @Override public int getContentLength() { return -1; }
            };
        })).andExpect(status().isPayloadTooLarge());
        verifyNoInteractions(provider);
    }

    @Test void rateLimitIgnoresSpoofedIpHeaders() throws Exception {
        when(provider.answer(anyString(), anyString(), eq(false))).thenReturn("Kiểm tra độ ẩm.");
        for (int i = 0; i < 10; i++) {
            mvc.perform(chat("{\"message\":\"Cách tưới bonsai\"}")
                    .header("X-Forwarded-For", "spoof-" + i).with(r -> { r.setRemoteAddr("same-peer"); return r; }))
                    .andExpect(status().isOk());
        }
        mvc.perform(chat("{\"message\":\"Cách tưới bonsai\"}").header("X-Real-IP", "different")
                .with(r -> { r.setRemoteAddr("same-peer"); return r; }))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "60"));
        verify(provider, times(10)).answer(anyString(), anyString(), eq(false));
    }

    @Test void allExistingCatalogReadsRemainPublic() throws Exception {
        when(categories.findByIsActiveTrueOrderBySortOrderAscIdAsc()).thenReturn(List.of());
        when(products.findByStatusOrderByIdAsc(ProductStatus.ACTIVE)).thenReturn(List.of());
        when(stores.findByStatusOrderByIdAsc(StoreStatus.ACTIVE)).thenReturn(List.of());
        when(products.findByIdAndStatus(123L, ProductStatus.ACTIVE)).thenReturn(Optional.empty());
        when(stores.findByIdAndStatus(123L, StoreStatus.ACTIVE)).thenReturn(Optional.empty());
        for (String path : List.of("/api/categories", "/api/products", "/api/stores")) mvc.perform(get(path)).andExpect(status().isOk());
        mvc.perform(get("/api/products/123")).andExpect(status().isNotFound());
        mvc.perform(get("/api/stores/123")).andExpect(status().isNotFound());
    }
}
