package com.ssafy.e106.feature.analysis

import com.ssafy.e106.data.repository.BatchLookupReviewHint
import com.ssafy.e106.feature.analysis.model.AnalysisCandidate
import com.ssafy.e106.feature.analysis.model.AppUsageSnapshot
import com.ssafy.e106.feature.analysis.model.NormalizedPaymentRecord
import com.ssafy.e106.feature.analysis.model.PaymentRecord
import com.ssafy.e106.feature.analysis.model.ServiceCatalog
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnDeviceAiContractsTest {

    @Test
    fun parseOnDeviceAiResponse_handlesFencedJsonObject() {
        val rawResponse = """
            ```json
            {
              "items": [
                {
                  "reviewId": 101,
                  "decision": "REVIEW_NEEDED",
                  "serviceId": 1,
                  "servicePlanId": 11,
                  "subscriptionConfidence": 0.61,
                  "reasonCodes": ["AI_CONFLICTING_SIGNALS"]
                }
              ]
            }
            ```
        """.trimIndent()

        val parsed = parseOnDeviceAiResponse(rawResponse)

        assertNotNull(parsed)
        assertEquals(1, parsed?.items?.size)
        assertEquals(101L, parsed?.items?.first()?.reviewId)
        assertEquals(REVIEW_NEEDED, parsed?.items?.first()?.decision)
    }

    @Test
    fun parseOnDeviceAiResponse_handlesBareJsonArray() {
        val rawResponse = """
            [
              {
                "reviewId": 101,
                "decision": "CONFIRMED_NON_SUBSCRIPTION",
                "subscriptionConfidence": 0.93,
                "reasonCodes": ["AI_CONFIRMED_NON_SUBSCRIPTION"]
              }
            ]
        """.trimIndent()

        val parsed = parseOnDeviceAiResponse(rawResponse)

        assertNotNull(parsed)
        assertEquals(1, parsed?.items?.size)
        assertEquals(CONFIRMED_NON_SUBSCRIPTION, parsed?.items?.first()?.decision)
    }

    @Test
    fun buildOnDeviceAiPrompt_embedsContractAndInputPayload() {
        val prompt = buildOnDeviceAiPrompt(listOf(sampleHint()))

        assertTrue(prompt.contains("Keep reviewId unchanged."))
        assertTrue(prompt.contains("Use only serviceId and servicePlanId values listed in allowedServices."))
        assertTrue(prompt.contains("\"reviewId\":101"))
        assertTrue(prompt.contains("\"serviceId\":1"))
        assertTrue(prompt.contains("\"servicePlanId\":11"))
    }

    private fun sampleHint(): BatchLookupReviewHint {
        val service = ServiceCatalog.Service(
            serviceId = 1L,
            code = "NETFLIX",
            name = "Netflix",
            aliases = setOf("NETFLIX"),
            packageNames = setOf("com.netflix.mediaclient"),
            knownMerchantPatterns = setOf("NETFLIX"),
            priceTolerance = 2000,
            plans = listOf(
                ServiceCatalog.Plan(
                    servicePlanId = 11L,
                    planName = "Standard",
                    billingCycle = "MONTHLY",
                    monthlyPrice = 17000,
                ),
            ),
        )
        val paymentRecord = PaymentRecord(
            paymentId = "pay_101",
            merchantRaw = "NETFLIX.COM",
            amount = 17000,
            paymentDate = LocalDate.of(2026, 3, 1),
        )
        val normalizedPayment = NormalizedPaymentRecord(
            paymentRecord = paymentRecord,
            merchantNormalized = "NETFLIX COM",
            merchantTokens = listOf("NETFLIX", "COM"),
            normalizedAmount = 17000,
            paymentDate = paymentRecord.paymentDate,
            ruleTags = setOf("recurring"),
        )
        val candidate = AnalysisCandidate(
            reviewId = 101L,
            paymentRecord = paymentRecord,
            normalizedPaymentRecord = normalizedPayment,
            usageSignals = listOf(
                AppUsageSnapshot(
                    packageName = "com.netflix.mediaclient",
                    usage7dMs = 120_000L,
                    usage30dMs = 900_000L,
                    lastUsedEpochMs = 1_710_000_000_000L,
                    permissionGranted = true,
                    reason = null,
                    queriedAtEpochMs = 1_710_000_100_000L,
                    timezone = "Asia/Seoul",
                ),
            ),
            serviceCatalogHints = listOf(service),
            recurrenceLabel = "MONTHLY",
            ruleScores = mapOf("candidateScore" to 8.0),
        )
        return BatchLookupReviewHint(
            candidate = candidate,
            matched = true,
            serviceId = 1L,
            servicePlanId = 11L,
            serviceName = "Netflix",
            hitCount = 2,
        )
    }

    private companion object {
        const val CONFIRMED_NON_SUBSCRIPTION = "CONFIRMED_NON_SUBSCRIPTION"
        const val REVIEW_NEEDED = "REVIEW_NEEDED"
    }
}
