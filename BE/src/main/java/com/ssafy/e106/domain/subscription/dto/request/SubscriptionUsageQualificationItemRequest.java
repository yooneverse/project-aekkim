package com.ssafy.e106.domain.subscription.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record SubscriptionUsageQualificationItemRequest(
    @NotNull(message = "subscriptionId는 필수입니다.") Long subscriptionId,
    @NotNull(message = "lowUsageDetected는 필수입니다.") Boolean lowUsageDetected,
    @NotBlank(message = "cycleYm은 필수입니다.")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "cycleYm은 YYYY-MM 형식이어야 합니다.")
    String cycleYm) {
}
