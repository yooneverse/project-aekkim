package com.ssafy.e106.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
    @NotBlank
    String refreshToken) {
}
