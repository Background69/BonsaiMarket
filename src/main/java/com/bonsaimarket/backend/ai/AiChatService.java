package com.bonsaimarket.backend.ai;

import com.bonsaimarket.backend.ai.dto.*;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import java.text.NumberFormat;
import java.util.*;

@Service
public class AiChatService {
    private final AiProviderClient provider;
    private final CatalogContextService catalog;
    private final FaqContextService faq;
    private final ObjectMapper mapper;

    public AiChatService(AiProviderClient provider, CatalogContextService catalog, FaqContextService faq, ObjectMapper mapper) {
        this.provider = provider; this.catalog = catalog; this.faq = faq; this.mapper = mapper;
    }

    public AiChatResponse chat(String message) {
        if (message == null || message.isBlank() || message.strip().length() > 2000)
            throw new AiException(400, "INVALID_MESSAGE", "Câu hỏi phải có từ 1 đến 2.000 ký tự.");
        String question = message.strip();
        provider.requireReady(); // Missing configuration cannot trigger a DB query or paid call.
        boolean catalogMode = catalog.relevant(question);
        var knowledge = faq.retrieve(question);
        var context = catalogMode ? catalog.retrieve(question) : new CatalogContextService.Context(List.of(), List.of(), false);
        String data = mapper.writeValueAsString(Map.of("catalogMode", catalogMode, "catalog", context,
                "knowledgeVersion", faq.version(), "faq", knowledge));
        // Selection-only for care too: no model-authored facts, URLs or instructions reach the user.
        String answer = provider.answer(question, data, true);
        return groundedCatalog(answer, context, knowledge, catalogMode);
    }

    private AiChatResponse groundedCatalog(String output, CatalogContextService.Context context, List<FaqContextService.Entry> knowledge, boolean catalogMode) {
        JsonNode selection;
        try { selection = mapper.reader().with(tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(output); }
        catch (RuntimeException e) { throw AiProviderClient.invalidResponse(); }
        if (selection == null || !selection.isObject() || selection.size() != 3) throw AiProviderClient.invalidResponse();
        Set<Long> productIds = ids(selection, "productIds", 10);
        Set<Long> storeIds = ids(selection, "storeIds", 10);
        Set<Long> faqIds = ids(selection, "faqIds", 4);
        var selected = context.products().stream().filter(item -> productIds.contains(item.id())).toList();
        List<AiSourceDto> sources = new ArrayList<>();
        List<String> parts = new ArrayList<>();
        if (context.unresolvedReference()) parts.add("Vui lòng cho biết tên cây hoặc mã sản phẩm (ví dụ: sản phẩm #1). Mỗi câu hỏi được xử lý độc lập; tôi chưa xác định được cây bạn đang hỏi.");
        else if (catalogMode && context.products().isEmpty() && context.stores().isEmpty()) parts.add("Hiện BonsaiMarket chưa tìm thấy sản phẩm phù hợp.");
        else if (catalogMode && selected.isEmpty() && context.stores().isEmpty()) parts.add("Chưa chọn được sản phẩm phù hợp từ catalog đã truy xuất. Bạn có thể nêu tên cây hoặc ngân sách cụ thể hơn.");
        else if (!selected.isEmpty()) parts.add("Các sản phẩm AI chọn từ catalog công khai của BonsaiMarket:");
        NumberFormat money = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        money.setMaximumFractionDigits(2);
        for (var item : selected) {
            parts.add(item.name() + " (#" + item.id() + ") — " + money.format(item.price()) + " đồng; "
                    + (item.stock() == null ? "chưa có dữ liệu tồn kho" : "tồn kho: " + item.stock())
                    + "; gian hàng: " + item.storeName() + ".");
            sources.add(item.source()); sources.add(item.storeSource());
        }
        if (!selected.isEmpty()) parts.add("Giá và tồn kho là dữ liệu tại lúc truy xuất; hãy mở sản phẩm để kiểm tra lại trước khi giao dịch.");
        for (var store : context.stores()) {
            if (storeIds.contains(store.id())) { parts.add("Gian hàng: " + store.name() + "."); sources.add(store.source()); }
        }
        for (var entry : knowledge) {
            if (faqIds.contains(entry.id())) {
                parts.add(entry.title() + ": " + entry.text()); sources.add(entry.source());
            }
        }
        if (sources.stream().anyMatch(s -> s.type().equals("FAQ")))
            parts.add("Hướng dẫn tham khảo chung, cần đối chiếu loài cây và khí hậu địa phương; chưa thay thế đánh giá trực tiếp của người có chuyên môn.");
        if (parts.isEmpty()) parts.add("Chưa có nguồn kiến thức phù hợp để trả lời. Vui lòng nêu tên loài, điều kiện trồng và triệu chứng cụ thể.");
        return new AiChatResponse(String.join("\n\n", parts), sources.stream().distinct().toList());
    }

    private Set<Long> ids(JsonNode selection, String field, int max) {
        JsonNode array = selection.path(field);
        if (!array.isArray() || array.size() > max) throw AiProviderClient.invalidResponse();
        Set<Long> ids = new LinkedHashSet<>();
        for (JsonNode value : array) {
            if (!value.isIntegralNumber() || !value.canConvertToLong() || value.asLong() <= 0) throw AiProviderClient.invalidResponse();
            ids.add(value.asLong());
        }
        return ids;
    }
}
