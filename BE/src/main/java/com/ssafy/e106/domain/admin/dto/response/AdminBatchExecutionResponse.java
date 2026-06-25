package com.ssafy.e106.domain.admin.dto.response;

public record AdminBatchExecutionResponse(
    String jobName,
    Long jobExecutionId,
    String status) {
}
