package com.bonsaimarket.backend.address;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public final class AddressDtos {
    private AddressDtos() { }
    public record Write(@Size(max=80, message="Tên địa chỉ tối đa 80 ký tự.") String label,
        @NotBlank(message="Nhập tên người nhận.") @Size(max=150, message="Tên người nhận tối đa 150 ký tự.") String recipientName,
        @NotBlank(message="Nhập số điện thoại.") @Pattern(regexp="(?:0[2-9][0-9]{8,9}|\\+84[2-9][0-9]{8,9})", message="Nhập số điện thoại Việt Nam hợp lệ.") String phone,
        @NotBlank(message="Nhập tỉnh hoặc thành phố.") @Size(max=150, message="Tỉnh/thành tối đa 150 ký tự.") String provinceName,
        @Size(max=150, message="Quận/huyện tối đa 150 ký tự.") String districtName,
        @NotBlank(message="Nhập phường hoặc xã.") @Size(max=150, message="Phường/xã tối đa 150 ký tự.") String wardName,
        @NotBlank(message="Nhập địa chỉ chi tiết.") @Size(max=500, message="Địa chỉ chi tiết tối đa 500 ký tự.") String addressDetail,
        boolean isDefault) {
        public Write { label=trim(label); recipientName=trim(recipientName); phone=trim(phone); provinceName=trim(provinceName); districtName=trim(districtName); wardName=trim(wardName); addressDetail=trim(addressDetail); }
        private static String trim(String value) { return value == null ? "" : value.strip(); }
    }
    public record View(Long id, String label, String recipientName, String phone, String provinceName, String districtName,
                       String wardName, String addressDetail, boolean isDefault, LocalDateTime createdAt, LocalDateTime updatedAt) {
        static View from(Address a) { return new View(a.getId(),a.getLabel(),a.getRecipientName(),a.getPhone(),a.getProvinceName(),a.getDistrictName(),a.getWardName(),a.getAddressDetail(),Boolean.TRUE.equals(a.getIsDefault()),a.getCreatedAt(),a.getUpdatedAt()); }
    }
    /** Lưu snapshot bất biến vào orders.shipping_address_snapshot trong giao dịch đặt hàng. */
    public record ShippingSnapshot(String recipientName, String phone, String provinceName, String districtName, String wardName, String addressDetail) { }
}
