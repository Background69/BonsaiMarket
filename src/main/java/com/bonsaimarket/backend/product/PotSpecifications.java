package com.bonsaimarket.backend.product;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record PotSpecifications(
    @Size(max=160) String material,
    @Pattern(regexp="ROUND|RECTANGULAR|SQUARE|OVAL|OTHER") String shape,
    @Size(max=100) String color,
    @DecimalMin("0.1") @DecimalMax("500") @Digits(integer=3,fraction=1) BigDecimal diameterCm,
    @DecimalMin("0.1") @DecimalMax("500") @Digits(integer=3,fraction=1) BigDecimal lengthCm,
    @DecimalMin("0.1") @DecimalMax("500") @Digits(integer=3,fraction=1) BigDecimal widthCm,
    @DecimalMin("0.1") @DecimalMax("500") @Digits(integer=3,fraction=1) BigDecimal heightCm,
    @Pattern(regexp="YES|NO|UNKNOWN") String drainageStatus,
    @tools.jackson.databind.annotation.JsonDeserialize(using=com.bonsaimarket.backend.cart.CartQuantityDeserializer.class) @Min(0) @Max(1000) Integer drainageHoleCount,
    @tools.jackson.databind.annotation.JsonDeserialize(using=com.bonsaimarket.backend.cart.CartQuantityDeserializer.class) @Min(1) @Max(1000000) Integer weightGrams,
    Boolean hasTray,
    @Size(max=1000) String suitablePlants,
    @Size(max=2000) String usageInstructions
) {
    @AssertTrue(message="Số lỗ thoát nước phải phù hợp trạng thái đã chọn.")
    public boolean isDrainageConsistent(){return drainageHoleCount==null||(!"NO".equals(drainageStatus)||drainageHoleCount==0)&&(!"YES".equals(drainageStatus)||drainageHoleCount>0);}
    public static PotSpecifications from(ProductPotSpecs row){
        return row==null?null:new PotSpecifications(row.getMaterial(),row.getShape(),row.getColor(),row.getDiameterCm(),row.getLengthCm(),row.getWidthCm(),row.getHeightCm(),row.getDrainageStatus(),row.getDrainageHoleCount(),row.getWeightGrams(),row.getHasTray(),row.getSuitablePlants(),row.getUsageInstructions());
    }
    public void apply(ProductPotSpecs row){
        row.setMaterial(text(material));row.setShape(shape);row.setColor(text(color));row.setDiameterCm(diameterCm);row.setLengthCm(lengthCm);row.setWidthCm(widthCm);row.setHeightCm(heightCm);
        row.setDrainageStatus(drainageStatus);row.setDrainageHoleCount(drainageHoleCount);row.setWeightGrams(weightGrams);row.setHasTray(hasTray);row.setSuitablePlants(text(suitablePlants));row.setUsageInstructions(text(usageInstructions));
    }
    private static String text(String value){return value==null||value.isBlank()?null:value.strip();}
}
