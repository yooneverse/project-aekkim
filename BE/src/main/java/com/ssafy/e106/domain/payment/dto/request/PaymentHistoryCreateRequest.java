package com.ssafy.e106.domain.payment.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PaymentHistoryCreateRequest(
    @NotBlank(message = "paymentId는 필수입니다.")
    @Size(max = 100, message = "paymentId는 100자 이하여야 합니다.")
    String paymentId,

    @NotBlank(message = "merchantRaw는 필수입니다.")
    @Size(max = 255, message = "merchantRaw는 255자 이하여야 합니다.")
    String merchantRaw,

    @NotNull(message = "amount는 필수입니다.")
    @Positive(message = "amount는 0보다 커야 합니다.")
    Integer amount,

    @NotNull(message = "paymentDate는 필수입니다.")
    LocalDate paymentDate,

    @Size(max = 50, message = "category는 50자 이하여야 합니다.")
    String category) {
}
