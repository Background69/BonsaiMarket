package com.bonsaimarket.backend.ai;

import com.bonsaimarket.backend.product.*;
import com.bonsaimarket.backend.store.*;
import com.bonsaimarket.backend.category.Category;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.ObjectMapper;
import java.net.http.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Real Gemini parser + retrieval + grounding, with mocked HTTP and repository only.
class GeminiGroundingTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = mock(HttpClient.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final StoreRepository stores = mock(StoreRepository.class);
    private final AiChatService service = new AiChatService(
            new AiProviderClient(new AiSettings(true, "test-placeholder", "gemini-fixture"), mapper, http),
            new CatalogContextService(products, stores), new FaqContextService(), mapper);

    GeminiGroundingTest() throws Exception {}

    private void respond(String text) throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(mapper.writeValueAsString(Map.of("candidates", List.of(Map.of(
                "finishReason", "STOP", "content", Map.of("role", "model", "parts", List.of(Map.of("text", text))))))));
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    }

    @Test void geminiSelectsIdsButSpringOwnsAllCommercialFactsAndSources() throws Exception {
        var store = new Store(); store.setId(7L); store.setName("Vườn thật"); store.setStatus(StoreStatus.ACTIVE);
        var category = new Category(); category.setName("Bonsai");
        var product = new Product(); product.setId(1L); product.setName("Tùng La Hán");
        product.setStatus(ProductStatus.ACTIVE); product.setStore(store); product.setCategory(category);
        product.setPrice(new BigDecimal("450000")); product.setStock(3);
        when(products.findAiPublicCatalog(anyString(), any(), any(), anyBoolean(), any(), any())).thenReturn(List.of(product));
        respond("{\"productIds\":[1,999],\"storeIds\":[999],\"faqIds\":[999]}");
        var result = service.chat("Có Tùng La Hán dưới 500.000 đồng?");
        assertTrue(result.answer().contains("Tùng La Hán (#1)"));
        assertTrue(result.answer().contains("450.000 đồng"));
        assertTrue(result.answer().contains("tồn kho: 3"));
        assertTrue(result.answer().contains("Vườn thật"));
        assertFalse(result.answer().contains("999"));
        assertEquals(List.of(1L, 7L), result.sources().stream().map(s -> s.id()).toList());
        verify(http, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test void forgedIdsCannotCreateProductsStoresOrPricesWithEmptyCatalog() throws Exception {
        when(products.findAiPublicCatalog(anyString(), any(), any(), anyBoolean(), any(), any())).thenReturn(List.of());
        respond("{\"productIds\":[999],\"storeIds\":[999],\"faqIds\":[999]}");
        var result = service.chat("Có cây nào dưới 500000 đồng?");
        assertEquals("Hiện BonsaiMarket chưa tìm thấy sản phẩm phù hợp.", result.answer());
        assertTrue(result.sources().isEmpty());
    }

    @Test void modelProseCannotBypassCatalogSchema() throws Exception {
        when(products.findAiPublicCatalog(anyString(), any(), any(), anyBoolean(), any(), any())).thenReturn(List.of());
        respond("Cây giả giá 1 đồng, gian hàng giả.");
        var error = assertThrows(AiException.class, () -> service.chat("Tìm cây bonsai"));
        assertEquals("AI_INVALID_RESPONSE", error.code());
        assertFalse(error.getMessage().contains("giả"));
    }

    @ParameterizedTest @ValueSource(strings = {"Cách tưới bonsai", "Ánh sáng cho bonsai", "Đất trồng bonsai",
            "Cắt tỉa bonsai", "Bón phân bonsai", "Bonsai lá vàng", "Bonsai thối rễ",
            "Chăm cây trong nhà", "Chăm cây ngoài trời"})
    void allRequiredCareTopicsRetainFaqRetrievalWithoutDatabase(String question) throws Exception {
        respond("Tham khảo FAQ và kiểm tra điều kiện của cây.");
        var result = service.chat(question);
        assertTrue(result.sources().stream().anyMatch(s -> s.type().equals("FAQ")));
        verifyNoInteractions(products, stores);
    }
}
