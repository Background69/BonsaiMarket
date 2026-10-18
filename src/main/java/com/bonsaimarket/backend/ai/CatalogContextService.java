package com.bonsaimarket.backend.ai;

import com.bonsaimarket.backend.ai.dto.AiSourceDto;
import com.bonsaimarket.backend.product.*;
import com.bonsaimarket.backend.store.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bonsaimarket.backend.catalog.StoreDiscovery;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class CatalogContextService {
    public record Item(Long id, String name, BigDecimal price, Integer stock, String category,
                       Long storeId, String storeName, String description) {
        public AiSourceDto source() { return new AiSourceDto("PRODUCT", id, name); }
        public AiSourceDto storeSource() { return new AiSourceDto("STORE", storeId, storeName); }
    }
    public record Shop(Long id, String name) {
        public AiSourceDto source() { return new AiSourceDto("STORE", id, name); }
    }
    public record Context(List<Item> products, List<Shop> stores, boolean unresolvedReference) {}
    public record Search(String name, BigDecimal minPrice, BigDecimal maxPrice, boolean inStock, Long productId, Long storeId) {}
    private final ProductRepository products;
    private final StoreDiscovery stores;

    public CatalogContextService(ProductRepository products, StoreDiscovery stores) {
        this.products = products; this.stores = stores;
    }

    public boolean relevant(String message) {
        String intent = FaqContextService.normalize(message).replace("gia the", "dat trong")
                .replace("moi mua", "moi nhan").replace("cham cay de ban", "cham cay")
                .replaceAll("\\bmua (mua|lanh|dong|kho|he|nang)\\b", "thoi tiet");
        return intent.matches("(?s).*\\b(gia|san pham|mua|tim|goi y|de ban|ngan sach|trieu|gian hang|cua hang|con hang|ton kho|ai ban|ban cay|ban bonsai|dang ban|nguoi ban)\\b.*")
                || intent.matches("(?s).*\\d[\\d.,]*\\s*(?:k|tr|trieu|nghin|ngan|dong)\\b.*");
    }

    @Transactional(readOnly = true, timeout = 5)
    public Context retrieve(String message) {
        String question = FaqContextService.normalize(message);
        Search search = analyze(message);
        if (search.storeId() != null || question.matches("(?s).*\\b(gian hang|cua hang)\\b.*")
                && !question.matches("(?s).*\\b(ban|san pham)\\b.*")) {
            Map<String,String> filters = new LinkedHashMap<>();
            filters.put("size", "10");
            String storeName = question.replaceAll("(?:gian hang|cua hang)\\s*(?:#|id\\s*)?\\d+", " ")
                    .replaceAll("\\b(?:tim|gian|hang|cua|danh|sach|cho|toi|xem|nao|co|cong|khai|khong)\\b", " ")
                    .replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").strip();
            if (search.storeId() == null && !storeName.isEmpty()) filters.put("search", shortText(storeName,160));
            // Reuse the public directory's owner visibility and bounded DB pagination.
            var result = search.storeId() == null ? stores.search(filters) : stores.findPublicById(search.storeId());
            var shops = result.content().stream().filter(s -> search.storeId() == null || search.storeId().equals(s.id()))
                    .limit(10).map(s -> new Shop(s.id(), shortText(s.name(),160))).toList();
            return new Context(List.of(), shops, false);
        }
        if (search.productId() == null && (search.name().isEmpty() || Set.of("nay","do").contains(search.name()))
                && question.matches("(?s).*\\b(cay nay|cay do|san pham nay|san pham do)\\b.*")) {
            return new Context(List.of(), List.of(), true);
        }
        List<Item> items = products.findAiPublicCatalog(search.name(), search.minPrice(), search.maxPrice(),
                search.inStock(), search.productId(), PageRequest.of(0, 10)).stream()
                // Defense in depth, including test doubles: no nonpublic entities enter context.
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE && p.getId() != null && p.getId() > 0
                        && p.getStore() != null && p.getStore().getStatus() == StoreStatus.ACTIVE
                        && p.getStore().getId() != null && p.getStore().getId() > 0
                        && p.getCategory() != null && Boolean.TRUE.equals(p.getCategory().getIsActive())
                        && p.getPrice() != null && p.getPrice().signum() >= 0
                        && p.getName() != null && !p.getName().isBlank())
                .filter(p -> search.minPrice() == null || p.getPrice().compareTo(search.minPrice()) >= 0)
                .filter(p -> search.maxPrice() == null || p.getPrice().compareTo(search.maxPrice()) <= 0)
                .filter(p -> !search.inStock() || p.getStock() != null && p.getStock() > 0)
                .filter(p -> search.productId() == null || search.productId().equals(p.getId()))
                .filter(p -> search.name().isEmpty() || FaqContextService.normalize(p.getName()).contains(search.name()))
                .limit(10).map(p -> new Item(p.getId(), shortText(p.getName(), 160), p.getPrice(),
                        p.getStock() == null || p.getStock() < 0 ? null : p.getStock(),
                        p.getCategory() == null ? null : shortText(p.getCategory().getName(), 120),
                        p.getStore().getId(), shortText(p.getStore().getName(), 160), null))
                .toList();
        return new Context(items, List.of(), false);
    }

    static String shortText(String text, int max) {
        if (text == null) return null;
        String safe = text.replaceAll("<[^>]*>", " ").replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", " ").strip();
        return safe.substring(0, Math.min(safe.length(), max));
    }

    static Search analyze(String message) {
        String q = FaqContextService.normalize(message);
        Long productId = null;
        Long storeId = null;
        var shopId = Pattern.compile("(?:gian hang|cua hang)\\s*(?:#|id\\s*)?(\\d{1,12})").matcher(q);
        if (shopId.find()) storeId = Long.valueOf(shopId.group(1));
        var id = Pattern.compile("(?:#|/products/|san pham\\s+(?:id\\s*)?)(\\d{1,12})").matcher(q);
        int idStart = -1, idEnd = -1;
        if (storeId == null && id.find()) {
            productId = Long.valueOf(id.group(1));
            idStart = id.start(1); idEnd = id.end(1);
        }
        BigDecimal min = null, max = null;
        var amounts = Pattern.compile("(\\d[\\d.,]*)(?:\\s*)(trieu|tr|nghin|ngan|k)?").matcher(q);
        List<BigDecimal> values = new ArrayList<>();
        List<Integer> positions = new ArrayList<>();
        while (amounts.find()) {
            if (storeId != null || amounts.start() >= idStart && amounts.start() < idEnd) continue;
            String raw = amounts.group(1);
            String unit = amounts.group(2);
            if (raw.length() > 15) continue;
            if (unit == null) raw = raw.replace(".", "").replace(",", "");
            else raw = raw.replace(',', '.');
            try {
                BigDecimal amount = new BigDecimal(raw);
                if (unit != null) amount = amount.multiply(new BigDecimal(unit.startsWith("tr") ? "1000000" : "1000"));
                values.add(amount); positions.add(amounts.start());
            } catch (NumberFormatException ignored) { /* Not a supported price expression. */ }
        }
        if (values.size() >= 2 && q.matches("(?s).*tu.*(?:den|toi).*")) {
            min = values.get(0); max = values.get(1);
        } else if (!values.isEmpty()) {
            String before = q.substring(0, positions.get(0));
            if (before.matches("(?s).*\\b(tren|tu|toi thieu)\\b.*")) min = values.get(0);
            else max = values.get(0);
        }
        String name = "";
        for (String plant : List.of("chau gom", "tung la han", "mai chieu thuy", "kim ngan", "kim tien", "sen da", "linh sam", "tung", "mai", "sanh", "si")) {
            if (Pattern.compile("(?<![a-z])" + plant + "(?![a-z])").matcher(q).find()) { name = plant; break; }
        }
        // Generic price/desk/stock questions are bounded suggestions; unknown named searches stay narrow.
        if (name.isEmpty() && productId == null && q.matches("(?s).*\\b(tim|mua|san pham|dang ban)\\b.*")) {
            String residual = q.split("\\b(?:duoi|tren|tu|ngan sach)\\b|[,;]",2)[0]
                    .replaceAll("(?:#|/products/|san pham\\s+(?:id\\s*)?)\\d+", " ")
                    .replaceAll("\\d[\\d.,]*\\s*(?:trieu|tr|nghin|ngan|k)?", " ")
                    .replaceAll("\\b(?:toi|muon|tim|mua|cay|bonsai|san|pham|co|nao|duoi|tren|dong|gia|con|hang|khong|cho|xem|nhung|phu|hop|ngan|sach|de|ban|gian|cua|danh|theo|voi|mot|va|trong|phong)\\b", " ")
                    .replaceAll("\\s+", " ").strip();
            name = residual.replaceAll("[^a-z ]", "").strip();
        }
        return new Search(name, min, max, productId == null && (q.contains("con hang") || q.contains("ton kho")), productId, storeId);
    }
}
