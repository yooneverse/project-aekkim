package com.ssafy.e106.domain.subscription.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record MerchantMappingBatchLookupRequest(
    @NotEmpty(message = "items는 최소 1건 이상 필요합니다.")
    @Size(max = 100, message = "items는 최대 100건까지 조회할 수 있습니다.")
    List<@Valid MerchantMappingBatchLookupItemRequest> items) {
}
