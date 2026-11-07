package com.bonsaimarket.backend.catalog;

import com.bonsaimarket.backend.product.Product;
import com.bonsaimarket.backend.product.ProductPotSpecs;
import jakarta.persistence.criteria.*;
import java.math.BigDecimal;
import java.util.*;

/** EXISTS against the one-to-one table keeps counts and pagination independent of optional detail loading. */
record PotCatalogFilters(String material,String shape,String color,BigDecimal min,BigDecimal max,String drainage) {
    static PotCatalogFilters parse(Map<String,String> input){
        String shape=text(input.get("pot_shape"),20),drainage=text(input.get("pot_drainage"),20);
        if(shape!=null&&!Set.of("ROUND","RECTANGULAR","SQUARE","OVAL","OTHER").contains(shape))throw new IllegalArgumentException("Kiểu dáng chậu không hợp lệ.");
        if(drainage!=null&&!Set.of("YES","NO","UNKNOWN").contains(drainage))throw new IllegalArgumentException("Bộ lọc thoát nước không hợp lệ.");
        var min=diameter(input.get("pot_min_diameter"));var max=diameter(input.get("pot_max_diameter"));
        if(min!=null&&max!=null&&min.compareTo(max)>0)throw new IllegalArgumentException("Đường kính tối thiểu phải nhỏ hơn hoặc bằng tối đa.");
        return new PotCatalogFilters(text(input.get("pot_material"),160),shape,text(input.get("pot_color"),100),min,max,drainage);
    }
    Predicate predicate(Root<Product> product,CriteriaQuery<?> query,CriteriaBuilder cb){
        if(material==null&&shape==null&&color==null&&min==null&&max==null&&drainage==null)return cb.conjunction();
        var sub=query.subquery(Long.class);var pot=sub.from(ProductPotSpecs.class);var conditions=new ArrayList<Predicate>();
        conditions.add(cb.equal(pot.get("productId"),product.get("id")));
        if(material!=null)conditions.add(cb.equal(cb.lower(pot.get("material")),material.toLowerCase(Locale.ROOT)));
        if(shape!=null)conditions.add(cb.equal(pot.get("shape"),shape));
        if(color!=null)conditions.add(cb.equal(cb.lower(pot.get("color")),color.toLowerCase(Locale.ROOT)));
        if(min!=null)conditions.add(cb.ge(pot.get("diameterCm"),min));
        if(max!=null)conditions.add(cb.le(pot.get("diameterCm"),max));
        if(drainage!=null)conditions.add("UNKNOWN".equals(drainage)?cb.or(cb.isNull(pot.get("drainageStatus")),cb.equal(pot.get("drainageStatus"),"UNKNOWN")):cb.equal(pot.get("drainageStatus"),drainage));
        sub.select(pot.get("productId")).where(conditions.toArray(Predicate[]::new));return cb.exists(sub);
    }
    private static String text(String value,int max){if(value==null||value.isBlank())return null;if(value.length()>max)throw new IllegalArgumentException("Bộ lọc chậu quá dài.");return value.strip();}
    private static BigDecimal diameter(String value){
        if(value==null||value.isBlank())return null;
        if(!value.matches("[0-9]{1,3}(\\.[0-9])?"))throw new IllegalArgumentException("Đường kính cần từ 0,1 đến 500 cm, tối đa một số thập phân.");
        var result=new BigDecimal(value);if(result.compareTo(new BigDecimal("0.1"))<0||result.compareTo(new BigDecimal("500"))>0)throw new IllegalArgumentException("Đường kính cần từ 0,1 đến 500 cm.");return result;
    }
}
