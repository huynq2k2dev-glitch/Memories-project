package com.memories.platform.memory.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateMemoryLocationRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 500) String address,
        @Digits(integer = 3, fraction = 7)
        @DecimalMin("-90") @DecimalMax("90")
        BigDecimal latitude,
        @Digits(integer = 3, fraction = 7)
        @DecimalMin("-180") @DecimalMax("180")
        BigDecimal longitude,
        @Size(max = 2048) String mapUrl,
        @Size(max = 1000) String note,
        @NotNull @PositiveOrZero Long version
) {
    public UpdateMemoryLocationRequest {
        name = normalizeRequired(name);
        address = normalizeOptional(address);
        mapUrl = normalizeOptional(mapUrl);
        note = normalizeOptional(note);
    }

    private static String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
