package com.ssafy.e106.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateOptionalConsentRequest(
    @NotNull(message = "선택 사항 동의 여부는 필수입니다.")
    Boolean optionalConsentAgreed) {
}
