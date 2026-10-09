package com.bonsaimarket.backend.ai;

import com.bonsaimarket.backend.product.*;
import com.bonsaimarket.backend.store.*;
import com.bonsaimarket.backend.category.Category;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

class AiChatServiceTest {
    private final ProductRepository products = mock(ProductRepository.class);
    private final StoreRepository stores = mock(StoreRepository.class);
    private final CatalogContextService catalog = new CatalogContextService(products, stores);
    private final AiProviderClient provider = mock(AiProviderClient.class);
    private final FaqContextService faq = new FaqContextService();
    private final AiChatService service = new AiChatService(provider, catalog, faq, new ObjectMapper());
    AiChatServiceTest() throws Exception {}

    private Product item(long id, ProductStatus status, StoreStatus storeStatus) {
        var store = new Store(); store.setId(7L); store.setName("Vườn thật"); store.setStatus(storeStatus);
        store.setAddress("private address"); store.setPhone("private phone");
        var category = new Category(); category.setName("Bonsai");
        var p = new Product(); p.setId(id); p.setName("Tùng La Hán"); p.setStatus(status); p.setStore(store);
        p.setCategory(category); p.setPrice(new BigDecimal("450000")); p.setStock(3); p.setDescription("Cây bonsai");
        p.setMetadataJson("private metadata"); return p;
    }

    @Test void onlyPublicProductsAndBoundedFieldsReachProviderAndSources() {
        when(products.findAiPublicCatalog(anyString(), any(), any(), anyBoolean(), any(), any())).thenReturn(List.of(
                item(1, ProductStatus.ACTIVE, StoreStatus.ACTIVE), item(2, ProductStatus.DRAFT, StoreStatus.ACTIVE),
                item(3, ProductStatus.ACTIVE, StoreStatus.INACTIVE)));
        when(provider.answer(anyString(), anyString(), eq(true))).thenReturn("{\"productIds\":[1,999,2,3],\"storeIds\":[888],\"faqIds\":[],\"answer\":\"Hàng giả giá 1 đồng\"}");
        var result = service.chat("Tìm Tùng dưới 500.000 đồng");
        assertTrue(result.answer().contains("450.000"));
        assertFalse(result.answer().contains("Hàng giả"));
        assertEquals(List.of(1L, 7L), result.sources().stream().map(s -> s.id()).toList());
        var context = ArgumentCaptor.forClass(String.class);
        verify(provider).answer(anyString(), context.capture(), eq(true));
        assertFalse(context.getValue().contains("private"));
        assertTrue(context.getValue().length() < AiProviderClient.MAX_CONTEXT_CHARS);
        var page = ArgumentCaptor.forClass(Pageable.class);
        verify(products).findAiPublicCatalog(eq("tung"), isNull(), eq(new BigDecimal("500000")), eq(false), isNull(), page.capture());
        assertEquals(10, page.getValue().getPageSize());
    }

    @Test void noResultsCannotProduceFabricatedSourcesOrPrices() {
        when(products.findAiPublicCatalog(anyString(), any(), any(), anyBoolean(), any(), any())).thenReturn(List.of());
        when(provider.answer(anyString(), anyString(), eq(true))).thenReturn("{\"productIds\":[999],\"storeIds\":[999],\"faqIds\":[]}");
        var response = service.chat("Có bonsai nào dưới 500000 đồng?");
        assertEquals("Hiện BonsaiMarket chưa tìm thấy sản phẩm phù hợp.", response.answer());
        assertTrue(response.sources().isEmpty());
    }

    @Test void faqCareQuestionSkipsDatabase() {
        when(provider.answer(anyString(), anyString(), eq(false))).thenReturn("Kiểm tra độ ẩm trước khi tưới.");
        var result = service.chat("Cách tưới bonsai");
        assertFalse(result.sources().isEmpty());
        assertTrue(result.sources().stream().allMatch(s -> s.type().equals("FAQ")));
        verifyNoInteractions(products, stores);
    }

    @Test void mixedQuestionIncludesCatalogAndFaq() {
        when(products.findAiPublicCatalog(anyString(), any(), any(), anyBoolean(), any(), any())).thenReturn(List.of(item(1, ProductStatus.ACTIVE, StoreStatus.ACTIVE)));
        when(provider.answer(anyString(), anyString(), eq(true))).thenReturn("{\"productIds\":[1],\"storeIds\":[],\"faqIds\":[1]}");
        var response = service.chat("Mua cây dưới 500000 đồng, tưới nước bao nhiêu lần?");
        assertTrue(response.sources().stream().anyMatch(s -> s.type().equals("FAQ")));
        assertTrue(response.sources().stream().anyMatch(s -> s.type().equals("PRODUCT")));
    }

    @Test void unresolvedReferenceAsksForIdWithoutInventingContext() {
        when(provider.answer(anyString(), anyString(), eq(true))).thenReturn("{\"productIds\":[],\"storeIds\":[],\"faqIds\":[]}");
        assertTrue(service.chat("Cây này còn hàng không?").answer().contains("mã sản phẩm"));
        verifyNoInteractions(products, stores);
    }

    @Test void configErrorStopsRetrievalAndProviderCall() {
        doThrow(new AiException(503, "AI_NOT_CONFIGURED", "Trợ lý chưa sẵn sàng.")).when(provider).requireReady();
        assertEquals(503, assertThrows(AiException.class, () -> service.chat("Mua cây dưới 500000 đồng")).status());
        verifyNoInteractions(products, stores);
        verify(provider, never()).answer(anyString(), anyString(), anyBoolean());
    }

    @Test void malformedSelectionIsSafe() {
        when(products.findAiPublicCatalog(anyString(), any(), any(), anyBoolean(), any(), any())).thenReturn(List.of());
        for (String output : List.of("text", "null", "{}", "{\"productIds\":[\"javascript:evil\"],\"storeIds\":[],\"faqIds\":[]}")) {
            when(provider.answer(anyString(), anyString(), eq(true))).thenReturn(output);
            assertEquals(502, assertThrows(AiException.class, () -> service.chat("Mua cây")).status());
        }
    }

    @Test void budgetsNamesAndIdsAreParsedWithoutSqlGeneration() {
        assertEquals(new BigDecimal("1000000"), CatalogContextService.analyze("ngân sách 1 triệu đồng").maxPrice());
        assertEquals(new BigDecimal("500000"), CatalogContextService.analyze("Tùng dưới 500.000 đồng").maxPrice());
        assertEquals(new BigDecimal("500000.0"), CatalogContextService.analyze("dưới 0,5 triệu đồng").maxPrice());
        assertEquals(new BigDecimal("100000"), CatalogContextService.analyze("từ 100.000 đến 500.000 đồng").minPrice());
        assertEquals(new BigDecimal("500000"), CatalogContextService.analyze("từ 100.000 đến 500.000 đồng").maxPrice());
        assertEquals(1L, CatalogContextService.analyze("sản phẩm #1 còn hàng không?").productId());
        assertNull(CatalogContextService.analyze("sản phẩm #1 còn hàng không?").maxPrice());
        assertEquals("xyz", CatalogContextService.analyze("Tìm cây xyz").name());
    }

    @Test void faqHasBoundedRelevantEntriesAndCaution() {
        var entries = faq.retrieve("Tùng bị vàng lá");
        assertTrue(entries.stream().anyMatch(e -> e.id() == 17));
        assertTrue(entries.size() <= 4);
        assertTrue(entries.stream().anyMatch(e -> e.text().contains("Không kết luận")));
    }

    @Test void careVocabularyDoesNotTriggerCommercialRetrieval() {
        assertFalse(catalog.relevant("Thời gian thay chậu bonsai"));
        assertFalse(catalog.relevant("Giá thể và đất trồng nên chọn như thế nào?"));
        assertFalse(catalog.relevant("Cắt tỉa cơ bản"));
        assertTrue(catalog.relevant("Gian hàng nào bán Tùng?"));
        assertTrue(catalog.relevant("Cây này còn hàng không?"));
    }

    @Test void stockAndPriceDefenseAndHardLimitApplyEvenToRepositoryStubs() {
        var zero = item(99, ProductStatus.ACTIVE, StoreStatus.ACTIVE); zero.setStock(0);
        var pricey = item(100, ProductStatus.ACTIVE, StoreStatus.ACTIVE); pricey.setPrice(new BigDecimal("900000"));
        List<Product> fixture = new ArrayList<>(List.of(zero, pricey));
        for (int i = 1; i <= 20; i++) fixture.add(item(i, ProductStatus.ACTIVE, StoreStatus.ACTIVE));
        when(products.findAiPublicCatalog(anyString(), any(), any(), anyBoolean(), any(), any())).thenReturn(fixture);
        var response = catalog.retrieve("Tùng còn hàng dưới 500000 đồng");
        assertEquals(10, response.products().size());
        assertTrue(response.products().stream().allMatch(p -> p.stock() > 0 && p.price().compareTo(new BigDecimal("500000")) <= 0));
    }

    @Test void storesComeOnlyFromPublicRepositoryData() {
        when(products.findAiPublicCatalog(anyString(), any(), any(), anyBoolean(), any(), any())).thenReturn(List.of());
        var good = new Store(); good.setId(7L); good.setName("Vườn thật"); good.setStatus(StoreStatus.ACTIVE);
        var privateStore = new Store(); privateStore.setId(8L); privateStore.setName("Ẩn"); privateStore.setStatus(StoreStatus.INACTIVE);
        when(stores.findByStatusOrderByIdAsc(StoreStatus.ACTIVE)).thenReturn(List.of(good, privateStore));
        when(provider.answer(anyString(), anyString(), eq(true))).thenReturn("{\"productIds\":[],\"storeIds\":[7,8,999],\"faqIds\":[]}");
        var response = service.chat("Tìm gian hàng");
        assertEquals(List.of(7L), response.sources().stream().map(s -> s.id()).toList());
        assertFalse(response.answer().contains("Ẩn"));
    }
}
