package com.memories.platform.guest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateMemoryGuestRequest(
        @NotBlank @Size(max = 200) String fullName,
        @Email @Size(max = 255) String email,
        @Pattern(regexp = "^\\+[1-9][0-9]{1,14}$") String phone,
        @Size(max = 100) String guestGroup,
        @NotNull @Min(1) @Max(50) Integer maxPartySize,
        @Size(max = 1000) String note
) {
}
