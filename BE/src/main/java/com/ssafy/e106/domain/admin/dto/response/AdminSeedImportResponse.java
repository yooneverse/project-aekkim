package com.ssafy.e106.domain.admin.dto.response;

import java.util.List;

public record AdminSeedImportResponse(
    String importType,
    List<String> sourceFiles,
    int processedCount) {
}
