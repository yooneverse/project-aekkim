package com.ssafy.e106.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequest(
    @NotBlank
    String kakaoAccessToken) {
}
