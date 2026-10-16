package com.bonsaimarket.backend.catalog;

import com.bonsaimarket.backend.product.*;
import com.bonsaimarket.backend.store.StoreStatus;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogSearch {
    private final ProductRepository products;
    public CatalogSearch(ProductRepository products) { this.products = products; }

    @Transactional(readOnly = true)
    public Page<Product> search(Map<String, String> input) {
        var q = Parameters.parse(input);
        Specification<Product> spec = (root, query, cb) -> {
            var store = root.join("store");
            var category = root.join("category");
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal(root.get("status"), ProductStatus.ACTIVE));
            predicates.add(cb.equal(store.get("status"), StoreStatus.ACTIVE));
            predicates.add(cb.isTrue(category.get("isActive")));
            if (!q.search().isEmpty()) for (String token : q.search().split(" ")) {
                String pattern = "%" + token.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("name")), pattern, '!'),
                    cb.like(cb.lower(root.get("description")), pattern, '!'),
                    cb.like(cb.lower(root.get("slug")), pattern, '!'),
                    cb.like(cb.lower(category.get("name")), pattern, '!')));
            }
            if (q.category() != null) predicates.add(cb.equal(category.get("slug"), q.category()));
            if (q.categoryId() != null) predicates.add(cb.equal(category.get("id"), q.categoryId()));
            if (q.store() != null) predicates.add(cb.equal(store.get("id"), q.store()));
            if (q.min() != null) predicates.add(cb.ge(root.get("price"), q.min()));
            if (q.max() != null) predicates.add(cb.le(root.get("price"), q.max()));
            if (q.inStock() != null) predicates.add(q.inStock() ? cb.gt(root.get("stock"), 0) : cb.equal(root.get("stock"), 0));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return products.findAll(spec, PageRequest.of(q.page(), q.size(), q.sort()));
    }

    public record Parameters(String search, String category, Long categoryId, Long store, BigDecimal min,
                             BigDecimal max, Boolean inStock, Sort sort, int page, int size) {
        public static Parameters parse(Map<String, String> input) {
            var allowed = Set.of("search", "q", "category", "categoryId", "store", "min_price", "max_price", "in_stock", "sort", "page", "size");
            if (!allowed.containsAll(input.keySet())) throw invalid("Tham số bộ lọc không được hỗ trợ.");
            String search = input.getOrDefault("search", input.getOrDefault("q", ""));
            if (search.length() > 160) throw invalid("Từ khóa tối đa 160 ký tự.");
            search = Normalizer.normalize(search, Normalizer.Form.NFC).replaceAll("(?U)\\s+", " ").strip();
            if (!search.isEmpty() && search.split(" ").length > 12) throw invalid("Từ khóa tối đa 12 từ.");
            if (input.containsKey("q") && input.containsKey("search") && !input.get("q").equals(input.get("search"))) throw invalid("Chỉ dùng một từ khóa tìm kiếm.");
            String category = blank(input.get("category"));
            if (category != null && category.length() > 180) throw invalid("Danh mục không hợp lệ.");
            var min = money(input.get("min_price")); var max = money(input.get("max_price"));
            if (min != null && max != null && min.compareTo(max) > 0) throw invalid("Giá tối thiểu phải nhỏ hơn hoặc bằng giá tối đa.");
            Boolean stock = switch (input.getOrDefault("in_stock", "")) {
                case "" -> null; case "1", "true" -> true; case "0", "false" -> false;
                default -> throw invalid("Bộ lọc tồn kho không hợp lệ.");
            };
            Sort sort = switch (input.getOrDefault("sort", "default")) {
                case "default" -> Sort.by("id").ascending();
                case "latest" -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
                case "price_asc" -> Sort.by(Sort.Order.asc("price"), Sort.Order.asc("id"));
                case "price_desc" -> Sort.by(Sort.Order.desc("price"), Sort.Order.asc("id"));
                case "name_asc" -> Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"));
                default -> throw invalid("Thứ tự sắp xếp không hợp lệ.");
            };
            return new Parameters(search, category, id(input.get("categoryId")), id(input.get("store")), min, max,
                stock, sort, number(input.getOrDefault("page", "0"), 0, 10000), number(input.getOrDefault("size", "20"), 1, 50));
        }
        private static String blank(String value) { return value == null || value.isBlank() ? null : value.strip(); }
        private static Long id(String value) {
            if (blank(value) == null) return null;
            try { long id = Long.parseLong(value); if (id > 0) return id; } catch (NumberFormatException ignored) { }
            throw invalid("Mã danh mục hoặc gian hàng không hợp lệ.");
        }
        private static int number(String value, int min, int max) {
            try { int n = Integer.parseInt(value); if (n >= min && n <= max) return n; } catch (NumberFormatException ignored) { }
            throw invalid("Trang hoặc số sản phẩm mỗi trang không hợp lệ.");
        }
        private static BigDecimal money(String value) {
            if (blank(value) == null) return null;
            if (!value.matches("[0-9]{1,13}(\\.[0-9]{1,2})?")) throw invalid("Giá phải là số không âm, tối đa 13 chữ số và 2 số thập phân.");
            return new BigDecimal(value);
        }
        private static IllegalArgumentException invalid(String message) { return new IllegalArgumentException(message); }
    }
}
