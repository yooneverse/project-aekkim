package com.ssafy.e106.domain.subscription.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CheckinCreateRequest(
    @NotNull Long serviceId,
    @NotBlank String cycleYm,
    @NotBlank String response) {
}
