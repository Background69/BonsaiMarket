package com.bonsaimarket.backend.product;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;
import com.bonsaimarket.backend.marketplace.MarketplaceException;

public record ProductSpecifications(
    @Size(max=160) String scientificName,
    @Size(max=160) String condition,
    @DecimalMin("0.1") @DecimalMax("2000") @Digits(integer=4,fraction=1) BigDecimal heightCm,
    @DecimalMin("0.1") @DecimalMax("500") @Digits(integer=3,fraction=1) BigDecimal potDiameterCm,
    @Size(max=160) String potMaterial,
    Light light,
    @Size(max=1000) String watering,
    @Size(max=300) String substrate,
    @Size(max=200) String origin,
    @Size(max=2000) String careNotes
) {
    public enum Light { INDIRECT, PARTIAL_SHADE, SUN }
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final List<String> KEYS = List.of("scientificName","condition","heightCm","potDiameterCm","potMaterial","light","watering","substrate","origin","careNotes");
    public static final ProductSpecifications EMPTY = new ProductSpecifications(null,null,null,null,null,null,null,null,null,null);

    public static ProductSpecifications read(String source) {
        if(source==null || source.isBlank()) return EMPTY;
        try {
            var root=JSON.readTree(source);
            if(!root.isObject()) return EMPTY;
            var fields=JSON.createObjectNode();
            for(String key:KEYS) if(root.has(key)) fields.set(key,root.get(key));
            var value=JSON.treeToValue(fields,ProductSpecifications.class);
            // Historical data may predate validation. Do not publish invalid measurements or unbounded text.
            return new ProductSpecifications(text(value.scientificName,160),text(value.condition,160),
                number(value.heightCm,2000),number(value.potDiameterCm,500),text(value.potMaterial,160),value.light,
                text(value.watering,1000),text(value.substrate,300),text(value.origin,200),text(value.careNotes,2000));
        } catch(RuntimeException e) { return EMPTY; }
    }
    public String merge(String existing) {
        try {
            var root=existing==null || existing.isBlank()?JSON.createObjectNode():JSON.readTree(existing);
            if(!root.isObject()) throw new IllegalArgumentException();
            var object=(tools.jackson.databind.node.ObjectNode) root;
            var values=JSON.valueToTree(this);
            for(String key:KEYS) {
                var value=values.get(key);
                if(value==null || value.isNull() || value.isString() && value.asString().isBlank()) object.remove(key);
                else object.set(key,value);
            }
            return JSON.writeValueAsString(object);
        } catch(RuntimeException e) {
            throw new MarketplaceException(409,"LEGACY_SPECIFICATIONS","Thông số cũ chưa có định dạng hỗ trợ. Nội dung chưa được ghi đè; liên hệ quản trị viên.");
        }
    }
    private static String text(String value,int max) {return value==null || value.isBlank() || value.length()>max?null:value;}
    private static BigDecimal number(BigDecimal value,int max) {return value==null || value.compareTo(new BigDecimal("0.1"))<0 || value.compareTo(BigDecimal.valueOf(max))>0 || value.stripTrailingZeros().scale()>1?null:value;}
}
