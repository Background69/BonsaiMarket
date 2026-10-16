package com.bonsaimarket.backend.marketplace;

import java.util.List;

public record ManagementPage<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    public static <T> ManagementPage<T> of(org.springframework.data.domain.Page<T> page) {
        return new ManagementPage<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
