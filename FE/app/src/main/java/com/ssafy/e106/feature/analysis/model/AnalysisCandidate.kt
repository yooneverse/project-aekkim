package com.ssafy.e106.feature.analysis.model

data class AnalysisCandidate(
    val reviewId: Long,
    val paymentRecord: PaymentRecord,
    val normalizedPaymentRecord: NormalizedPaymentRecord,
    val usageSignals: List<AppUsageSnapshot> = emptyList(),
    val serviceCatalogHints: List<ServiceCatalog.Service> = emptyList(),
    val bundleCatalogHints: List<ServiceCatalog.Bundle> = emptyList(),
    val activeUsageServiceIds: Set<Long> = emptySet(),
    val activeUsageServiceCodes: Set<String> = emptySet(),
    val recurrenceLabel: String? = null,
    val ruleScores: Map<String, Double> = emptyMap(),
)
