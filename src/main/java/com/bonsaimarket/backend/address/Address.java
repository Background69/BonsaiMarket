package com.bonsaimarket.backend.address;

import com.bonsaimarket.backend.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "label")
    private String label;

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "province_name")
    private String provinceName;

    @Column(name = "province_id")
    private String provinceId;

    @Column(name = "district_name")
    private String districtName;

    @Column(name = "district_id")
    private String districtId;

    @Column(name = "ward_name")
    private String wardName;

    @Column(name = "ward_code")
    private String wardCode;

    @Column(name = "address_detail")
    private String addressDetail;

    @Column(name = "is_default")
    private Boolean isDefault;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}

