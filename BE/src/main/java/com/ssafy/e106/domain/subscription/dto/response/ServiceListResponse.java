package com.ssafy.e106.domain.subscription.dto.response;

import java.util.List;

public record ServiceListResponse(
    String category,
    List<ServiceListItemResponse> services) {
}
