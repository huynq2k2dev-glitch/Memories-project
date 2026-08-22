package com.memories.platform.auth.dto;

import java.util.UUID;

public record RegistrationResponse(UUID id, String email, String status) {
}
