package com.bonsaimarket.backend.shipping;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface ShippingProvider {
    record Parcel(int weightGrams,int lengthCm,int widthCm,int heightCm) {}
    record Destination(String recipientName,String phone,String provinceName,String districtName,String wardName,String addressDetail,
        Integer provinceId,Integer districtId,String wardCode,boolean newAddress) {}
    record Pickup(int shopId,int districtId,String wardCode,String name,String phone,String address) {}
    record Rate(int serviceType,String serviceName,BigDecimal fee) {}
    record Item(String name,int quantity,int price) {}
    record Create(String clientReference,Destination destination,Pickup pickup,Parcel parcel,int serviceType,int insuranceValue,
        int feePayer,int codAmount,List<Item> items) {}
    record Remote(String trackingCode,String clientReference,int shopId,String status,Instant eventAt) {}
    Pickup pickup(int shopId);
    Destination validateAddress(int shopId,Destination address);
    List<Rate> quote(int shopId,Pickup pickup,Destination destination,Parcel parcel,int insuranceValue);
    default List<Rate> quoteCod(int shopId,Pickup pickup,Destination destination,Parcel parcel,int insuranceValue,int codAmount) {
        throw new com.bonsaimarket.backend.marketplace.MarketplaceException(409,"COD_QUOTE_UNSUPPORTED","Provider chưa có hợp đồng báo phí COD.");
    }
    Remote create(int shopId,Create request);
    Remote find(int shopId,String trackingCode,String clientReference);
    boolean cancel(int shopId,String trackingCode);
    default boolean returnToSender(int shopId,String trackingCode){throw new com.bonsaimarket.backend.marketplace.MarketplaceException(409,"CARRIER_RETURN_UNSUPPORTED","Provider chưa hỗ trợ yêu cầu hoàn về.");}
}
