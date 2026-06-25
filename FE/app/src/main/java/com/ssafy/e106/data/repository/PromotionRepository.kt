package com.ssafy.e106.data.repository

import com.ssafy.e106.core.model.ErrorResponse
import com.ssafy.e106.core.result.Result
import com.ssafy.e106.data.api.PromotionApi
import com.ssafy.e106.data.dto.promotion.PromotionDetailResponse
import com.ssafy.e106.data.dto.promotion.PromotionDetailServiceResponse
import com.ssafy.e106.data.dto.promotion.PromotionRecommendationCategoryResponse
import com.ssafy.e106.data.dto.promotion.PromotionRecommendationItemResponse
import com.ssafy.e106.data.dto.promotion.PromotionRecommendationServiceResponse
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.NumberFormat
import java.time.LocalDateTime
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import retrofit2.HttpException

enum class PromotionRecommendationGroupType {
    SubscriptionList,
    Conditional,
}

enum class PromotionType {
    Bundle,
    CardBenefit,
    Promo,
}

data class PromotionPriceData(
    val originalPrice: Int?,
    val discountPrice: Int?,
    val monthlySavedAmount: Int,
)

data class PromotionFeedData(
    val nickname: String,
    val categories: List<PromotionRecommendationCategoryData>,
)

data class PromotionRecommendationCategoryData(
    val category: String,
    val bundles: List<PromotionRecommendationData>,
    val promotions: List<PromotionRecommendationData>,
    val cardBenefits: List<PromotionRecommendationData>,
)

data class PromotionRecommendationData(
    val promotionId: Long,
    val groupType: PromotionRecommendationGroupType,
    val promotionType: PromotionType,
    val headline: String,
    val summary: String?,
    val price: PromotionPriceData,
    val billingCycle: String? = null,
    val recommendationScore: Int = 0,
    val recommendationReasons: List<String> = emptyList(),
    val conditionDescription: String? = null,
    val sourceUrl: String? = null,
    val imageUrl: String? = null,
    val services: List<PromotionServiceData> = emptyList(),
    val startsAt: String,
    val endsAt: String,
)

data class PromotionDetailData(
    val promotionId: Long,
    val promotionType: PromotionType,
    val headline: String,
    val summary: String?,
    val price: PromotionPriceData,
    val yearlySavedAmount: Int,
    val yearlyBenefitDescription: String,
    val conditionDescription: String? = null,
    val detailDescription: String,
    val applySteps: List<String>,
    val sourceUrl: String? = null,
    val imageUrl: String? = null,
    val services: List<PromotionServiceData> = emptyList(),
    val startsAt: String,
    val endsAt: String,
)

data class PromotionServiceData(
    val serviceId: Long,
    val code: String? = null,
    val name: String,
    val logoUrl: String? = null,
)

@Singleton
class PromotionRepository @Inject constructor(
    private val promotionApi: PromotionApi,
) {

    suspend fun getPromotionFeed(): Result<PromotionFeedData> {
        return try {
            val response = promotionApi.getRecommendations(
                promotionType = DEFAULT_PROMOTION_TYPE,
                page = DEFAULT_PAGE,
                size = DEFAULT_SIZE,
            )

            if (response.success && response.data != null) {
                val activeCategories = response.data.categories
                    .map { category -> category.toCategoryData() }
                    .filter { category ->
                        category.bundles.isNotEmpty() ||
                            category.promotions.isNotEmpty() ||
                            category.cardBenefits.isNotEmpty()
                    }

                Result.Success(
                    PromotionFeedData(
                        nickname = DEFAULT_NICKNAME,
                        categories = activeCategories,
                    ),
                )
            } else {
                Result.Error(response.message ?: DEFAULT_LIST_ERROR_MESSAGE)
            }
        } catch (e: UnknownHostException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: ConnectException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: SocketTimeoutException) {
            Result.Error(DEFAULT_TIMEOUT_ERROR_MESSAGE)
        } catch (e: HttpException) {
            Result.Error(parseHttpError(e) ?: DEFAULT_LIST_ERROR_MESSAGE, e.code())
        } catch (e: IOException) {
            Result.Error(e.message ?: DEFAULT_LIST_ERROR_MESSAGE)
        } catch (e: Exception) {
            Result.Error(e.message ?: DEFAULT_LIST_ERROR_MESSAGE)
        }
    }

    suspend fun getPromotionDetail(promotionId: Long): Result<PromotionDetailData> {
        return try {
            val response = promotionApi.getPromotionDetail(promotionId = promotionId)
            if (response.success && response.data != null) {
                Result.Success(response.data.toDetailData())
            } else {
                Result.Error(response.message ?: DEFAULT_DETAIL_ERROR_MESSAGE)
            }
        } catch (e: UnknownHostException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: ConnectException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: SocketTimeoutException) {
            Result.Error(DEFAULT_TIMEOUT_ERROR_MESSAGE)
        } catch (e: HttpException) {
            Result.Error(parseHttpError(e) ?: DEFAULT_DETAIL_ERROR_MESSAGE, e.code())
        } catch (e: IOException) {
            Result.Error(e.message ?: DEFAULT_DETAIL_ERROR_MESSAGE)
        } catch (e: Exception) {
            Result.Error(e.message ?: DEFAULT_DETAIL_ERROR_MESSAGE)
        }
    }

    private fun PromotionRecommendationCategoryResponse.toCategoryData(): PromotionRecommendationCategoryData {
        return PromotionRecommendationCategoryData(
            category = category,
            bundles = bundles.toActiveRecommendations(),
            promotions = promotions.toActiveRecommendations(),
            cardBenefits = cardBenefits.toActiveRecommendations(),
        )
    }

    private fun List<PromotionRecommendationItemResponse>.toActiveRecommendations(): List<PromotionRecommendationData> {
        val now = LocalDateTime.now()
        return map { item -> item.toRecommendationData() }
            .filter { recommendation ->
                recommendation.endsAt.toLocalDateTimeOrNull()?.isAfter(now) ?: true
            }
            .sortedWith(recommendationComparator)
    }

    private fun PromotionRecommendationItemResponse.toRecommendationData(): PromotionRecommendationData {
        val mappedPromotionType = promotionType.toPromotionType()
        val groupType = PromotionRecommendationGroupingPolicy.classify(mappedPromotionType)

        return PromotionRecommendationData(
            promotionId = promotionId,
            groupType = groupType,
            promotionType = mappedPromotionType,
            headline = title,
            summary = summary,
            price = PromotionPricingPolicy.toPriceData(
                originalPrice = originalPrice,
                discountPrice = discountPrice,
            ),
            billingCycle = billingCycle,
            recommendationScore = recommendationScore,
            recommendationReasons = recommendationReasons,
            conditionDescription = PromotionRecommendationGroupingPolicy.buildConditionDescription(
                groupType = groupType,
                promotionType = mappedPromotionType,
            ),
            sourceUrl = sourceUrl,
            imageUrl = imageUrl,
            services = services.map { service -> service.toDomain() },
            startsAt = startsAt,
            endsAt = endsAt,
        )
    }

    private fun PromotionDetailResponse.toDetailData(): PromotionDetailData {
        val mappedPromotionType = promotionType.toPromotionType()
        val mappedServices = services.map { service -> service.toDomain() }
        val priceData = PromotionPricingPolicy.toPriceData(
            originalPrice = originalPrice,
            discountPrice = discountPrice,
        )
        val yearlySavedAmount = PromotionPricingPolicy.calculateYearlySavedAmount(
            monthlySavedAmount = priceData.monthlySavedAmount,
        )
        val serviceSummary = mappedServices.toServiceSummaryLabel()

        return PromotionDetailData(
            promotionId = promotionId,
            promotionType = mappedPromotionType,
            headline = title,
            summary = summary,
            price = priceData,
            yearlySavedAmount = yearlySavedAmount,
            yearlyBenefitDescription = buildYearlyBenefitDescription(
                promotionType = mappedPromotionType,
                yearlySavedAmount = yearlySavedAmount,
                serviceSummary = serviceSummary,
            ),
            conditionDescription = buildDetailConditionDescription(
                promotionType = mappedPromotionType,
                serviceSummary = serviceSummary,
            ),
            detailDescription = buildDetailDescription(
                promotionType = mappedPromotionType,
                serviceSummary = serviceSummary,
            ),
            applySteps = buildApplySteps(
                promotionType = mappedPromotionType,
                serviceSummary = serviceSummary,
                sourceUrl = sourceUrl,
            ),
            sourceUrl = sourceUrl,
            imageUrl = resolveMediaUrl(imageUrl),
            services = mappedServices,
            startsAt = startsAt,
            endsAt = endsAt,
        )
    }

    private fun PromotionFixture.toRecommendationData(): PromotionRecommendationData {
        return PromotionRecommendationData(
            promotionId = promotionId,
            groupType = groupType,
            promotionType = promotionType,
            headline = headline,
            summary = summary,
            price = price,
            billingCycle = null,
            recommendationScore = 0,
            recommendationReasons = emptyList(),
            conditionDescription = conditionDescription,
            sourceUrl = sourceUrl,
            imageUrl = imageUrl,
            services = services,
            startsAt = startsAt,
            endsAt = endsAt,
        )
    }

    private fun PromotionFixture.toDetailData(): PromotionDetailData {
        return PromotionDetailData(
            promotionId = promotionId,
            promotionType = promotionType,
            headline = headline,
            summary = summary,
            price = price,
            yearlySavedAmount = yearlySavedAmount,
            yearlyBenefitDescription = yearlyBenefitDescription,
            conditionDescription = conditionDescription,
            detailDescription = detailDescription,
            applySteps = applySteps,
            sourceUrl = sourceUrl,
            imageUrl = imageUrl,
            services = services,
            startsAt = startsAt,
            endsAt = endsAt,
        )
    }

    private fun PromotionRecommendationServiceResponse.toDomain(): PromotionServiceData {
        return PromotionServiceData(
            serviceId = serviceId,
            code = null,
            name = serviceName,
            logoUrl = resolveMediaUrl(logoUrl),
        )
    }

    private fun PromotionDetailServiceResponse.toDomain(): PromotionServiceData {
        return PromotionServiceData(
            serviceId = serviceId,
            code = code,
            name = name,
            logoUrl = resolveMediaUrl(logoUrl),
        )
    }

    private fun PromotionRecommendationData.toFallbackDetailData(): PromotionDetailData {
        val yearlySavedAmount = price.monthlySavedAmount * MONTHS_PER_YEAR

        return PromotionDetailData(
            promotionId = promotionId,
            promotionType = promotionType,
            headline = headline,
            summary = summary,
            price = price,
            yearlySavedAmount = yearlySavedAmount,
            yearlyBenefitDescription = if (yearlySavedAmount > 0) {
                "1년이면 ${formatWon(yearlySavedAmount)}까지 절약할 수 있어요."
            } else {
                DEFAULT_FALLBACK_YEARLY_DESCRIPTION
            },
            conditionDescription = conditionDescription,
            detailDescription = summary?.takeIf { it.isNotBlank() } ?: DEFAULT_FALLBACK_DETAIL_DESCRIPTION,
            applySteps = buildFallbackApplySteps(sourceUrl),
            sourceUrl = sourceUrl,
            imageUrl = imageUrl,
            services = services,
            startsAt = startsAt,
            endsAt = endsAt,
        )
    }

    private data class PromotionFixture(
        val promotionId: Long,
        val groupType: PromotionRecommendationGroupType,
        val promotionType: PromotionType,
        val headline: String,
        val summary: String?,
        val price: PromotionPriceData,
        val yearlySavedAmount: Int,
        val yearlyBenefitDescription: String,
        val conditionDescription: String? = null,
        val detailDescription: String,
        val applySteps: List<String>,
        val sourceUrl: String?,
        val imageUrl: String?,
        val services: List<PromotionServiceData>,
        val startsAt: String,
        val endsAt: String,
    )

    private fun String.toLocalDateTimeOrNull(): LocalDateTime? {
        return runCatching { LocalDateTime.parse(this) }.getOrNull()
    }

    private fun String.toPromotionType(): PromotionType {
        return when (uppercase(Locale.ROOT)) {
            "BUNDLE" -> PromotionType.Bundle
            "CARD_BENEFIT" -> PromotionType.CardBenefit
            "PROMO" -> PromotionType.Promo
            else -> PromotionType.Promo
        }
    }

    private fun List<PromotionServiceData>.toServiceSummaryLabel(): String {
        return joinToString(separator = ", ") { service ->
            service.name.ifBlank { service.code ?: DEFAULT_SERVICE_LABEL }
        }
    }

    private fun buildYearlyBenefitDescription(
        promotionType: PromotionType,
        yearlySavedAmount: Int,
        serviceSummary: String,
    ): String {
        if (yearlySavedAmount > 0) {
            return when {
                serviceSummary.isBlank() -> "1년이면 ${formatWon(yearlySavedAmount)}까지 절약할 수 있어요."
                else -> "$serviceSummary 기준으로 1년이면 ${formatWon(yearlySavedAmount)}까지 절약할 수 있어요."
            }
        }

        return when (promotionType) {
            PromotionType.CardBenefit ->
                "월별 할인액은 카드 실적과 제휴 조건 충족 여부에 따라 달라질 수 있어요."

            PromotionType.Bundle,
            PromotionType.Promo,
            -> "상세 가격과 적용 가능 여부는 외부 안내에서 다시 확인해 주세요."
        }
    }

    private fun buildDetailConditionDescription(
        promotionType: PromotionType,
        serviceSummary: String,
    ): String? {
        return when (promotionType) {
            PromotionType.CardBenefit -> {
                val targetLabel = serviceSummary.ifBlank { DEFAULT_SERVICE_LABEL }
                "$targetLabel 혜택은 카드 실적, 제휴 대상, 정기결제 등록 여부를 먼저 확인해 주세요."
            }

            PromotionType.Bundle,
            PromotionType.Promo,
            -> null
        }
    }

    private fun buildDetailDescription(
        promotionType: PromotionType,
        serviceSummary: String,
    ): String {
        val targetLabel = serviceSummary.ifBlank { DEFAULT_SERVICE_LABEL }

        return when (promotionType) {
            PromotionType.CardBenefit ->
                "$targetLabel 관련 카드/제휴 혜택입니다. 실제 적용 조건과 대상 여부를 확인한 뒤 진행해 주세요."

            PromotionType.Bundle,
            PromotionType.Promo,
            -> "$targetLabel 구독 요금과 적용 조건을 비교한 뒤 혜택을 적용해 주세요."
        }
    }

    private fun buildApplySteps(
        promotionType: PromotionType,
        serviceSummary: String,
        sourceUrl: String?,
    ): List<String> {
        val targetLabel = serviceSummary.ifBlank { DEFAULT_SERVICE_LABEL }

        if (sourceUrl.isNullOrBlank()) {
            return listOf(
                "$targetLabel 대상 여부와 진행 기간을 먼저 확인해 주세요.",
                "적용 조건과 가격 비교 내용을 확인한 뒤 다시 시도해 주세요.",
                "운영 링크가 준비되면 상세 화면 CTA에서 바로 이동할 수 있어요.",
            )
        }

        return when (promotionType) {
            PromotionType.CardBenefit -> listOf(
                "외부 안내 페이지에서 카드 실적과 제휴 대상 여부를 확인해 주세요.",
                "$targetLabel 정기결제 또는 제휴 등록 조건을 적용해 주세요.",
                "다음 결제 주기부터 혜택 반영 여부를 확인해 주세요.",
            )

            PromotionType.Bundle,
            PromotionType.Promo,
            -> listOf(
                "외부 안내 페이지에서 $targetLabel 대상 여부를 확인해 주세요.",
                "기존 구독 요금과 변경 후 가격을 비교한 뒤 혜택을 적용해 주세요.",
                "가입 또는 연동 완료 후 실제 결제 금액을 다시 확인해 주세요.",
            )
        }
    }

    private fun parseHttpError(exception: HttpException): String? {
        val raw = exception.response()?.errorBody()?.string()?.trim().orEmpty()
        if (raw.isBlank()) return null

        val parsed = runCatching {
            json.decodeFromString<ErrorResponse>(raw)
        }.getOrNull()

        return parsed?.message ?: raw
    }

    private fun buildFallbackApplySteps(sourceUrl: String?): List<String> {
        return if (sourceUrl.isNullOrBlank()) {
            listOf(
                "프로모션 설명과 적용 조건을 먼저 확인해 주세요.",
                "운영 링크가 준비되면 상세 CTA에서 바로 이동할 수 있어요.",
                "필요하면 추천 목록을 다시 불러와 최신 상태를 확인해 주세요.",
            )
        } else {
            listOf(
                "프로모션 설명과 가격 비교를 확인해 주세요.",
                "외부 페이지에서 적용 조건과 대상 여부를 확인해 주세요.",
                "가입 또는 제휴 혜택 적용을 완료한 뒤 구독 요금을 다시 확인해 주세요.",
            )
        }
    }

    private fun formatWon(amount: Int): String {
        return NumberFormat.getNumberInstance(Locale.KOREA).format(amount) + "원"
    }

    private companion object {
        const val DEFAULT_LIST_ERROR_MESSAGE = "추천 정보를 불러오지 못했어요."
        const val DEFAULT_NETWORK_ERROR_MESSAGE = "네트워크 연결을 확인해 주세요."
        const val DEFAULT_TIMEOUT_ERROR_MESSAGE = "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요."
        const val DEFAULT_PROMOTION_TYPE = "ALL"
        const val DEFAULT_PAGE = 0
        const val DEFAULT_SIZE = 20
        const val MONTHS_PER_YEAR = 12
        const val DEFAULT_FALLBACK_DETAIL_DESCRIPTION =
            "프로모션 상세 API가 연결되기 전까지는 목록 정보를 기준으로 먼저 보여주고 있어요."
        const val DEFAULT_FALLBACK_YEARLY_DESCRIPTION =
            "연간 절감 예상 금액은 현재 목록의 월 절약 금액 기준으로 계산했어요."
        const val DEFAULT_SERVICE_LABEL = "프로모션 대상 서비스"

        val promotionRecommendationsEndpoint: String = "GET /api/v1/promotions/recommendations"
        val promotionDetailEndpoint: String = "GET /api/v1/promotions/{promotionId}"
        val promotionGroupingPolicyNote: String =
            PromotionRecommendationGroupingPolicy.temporaryRuleNote

        val recommendationComparator =
            compareByDescending<PromotionRecommendationData> { recommendation ->
                recommendation.isCardBenefitWithVisibleValue()
            }.thenByDescending { recommendation ->
                recommendation.recommendationScore
            }.thenByDescending { recommendation ->
                recommendation.price.monthlySavedAmount
            }.thenByDescending { recommendation ->
                runCatching { LocalDateTime.parse(recommendation.startsAt) }.getOrNull()
            }

        val json = Json { ignoreUnknownKeys = true }
        const val DEFAULT_DETAIL_ERROR_MESSAGE = "프로모션을 찾을 수 없어요."
        const val DEFAULT_NICKNAME = "이동섭"

        val promotionFixtures = listOf(
            PromotionFixture(
                promotionId = 1001L,
                groupType = PromotionRecommendationGroupType.SubscriptionList,
                promotionType = PromotionType.Bundle,
                headline = "네이버플러스 멤버십으로 넷플릭스 광고형을 더 저렴하게 보기",
                summary = "네이버플러스 멤버십에 가입하면 넷플릭스 광고형 스탠다드를 기본 혜택으로 받을 수 있어요.",
                price = PromotionPriceData(
                    originalPrice = 13_500,
                    discountPrice = 4_900,
                    monthlySavedAmount = 8_600,
                ),
                yearlySavedAmount = 103_200,
                yearlyBenefitDescription = "넷플릭스 7개월치 구독료와 비슷해요.",
                detailDescription = "네이버플러스 멤버십(월 4,900원)에 가입하면 넷플릭스 광고형 스탠다드 요금제가 기본 혜택으로 제공됩니다.",
                applySteps = listOf(
                    "네이버플러스 멤버십 페이지에서 멤버십에 가입해요.",
                    "멤버십 혜택 목록에서 넷플릭스 연결을 선택해요.",
                    "넷플릭스 계정을 연결하면 광고형 스탠다드 혜택이 적용돼요.",
                ),
                sourceUrl = "https://plus.naver.com/",
                imageUrl = "https://cdn.example.com/promotions/naver-netflix-membership.png",
                services = listOf(
                    PromotionServiceData(
                        serviceId = 1L,
                        name = "넷플릭스",
                        logoUrl = "https://cdn.example.com/services/netflix.png",
                    ),
                    PromotionServiceData(
                        serviceId = 2L,
                        name = "네이버플러스",
                        logoUrl = "https://cdn.example.com/services/naver-plus.png",
                    ),
                ),
                startsAt = "2026-03-01T00:00:00",
                endsAt = "2099-12-31T23:59:59",
            ),
            PromotionFixture(
                promotionId = 1002L,
                groupType = PromotionRecommendationGroupType.Conditional,
                promotionType = PromotionType.Promo,
                headline = "KT 5G 요금제를 쓰고 있다면 티빙 베이직을 무료로 볼 수 있어요",
                summary = "KT 5G 초이스 또는 제휴 대상 요금제를 이용하면 티빙 베이직 혜택을 받을 수 있어요.",
                price = PromotionPriceData(
                    originalPrice = 9_500,
                    discountPrice = 0,
                    monthlySavedAmount = 9_500,
                ),
                yearlySavedAmount = 114_000,
                yearlyBenefitDescription = "1년이면 11만원 넘게 아낄 수 있어요.",
                conditionDescription = "KT 5G 초이스 이상 요금제 유지 + 티빙 제휴 혜택 등록이 필요해요.",
                detailDescription = "KT 제휴 요금제를 유지하는 동안 티빙 베이직 이용권을 매달 무료로 받을 수 있습니다. 제휴 페이지에서 별도 등록이 필요할 수 있어요.",
                applySteps = listOf(
                    "KT 고객센터 또는 마이KT에서 내 요금제가 제휴 대상인지 확인해요.",
                    "티빙 제휴 혜택 페이지에서 휴대폰 번호 인증을 진행해요.",
                    "티빙 계정을 연결하면 베이직 이용권이 적용돼요.",
                ),
                sourceUrl = "https://www.kt.com/",
                imageUrl = "https://cdn.example.com/promotions/kt-tving-benefit.png",
                services = listOf(
                    PromotionServiceData(
                        serviceId = 3L,
                        name = "티빙",
                        logoUrl = "https://cdn.example.com/services/tving.png",
                    ),
                    PromotionServiceData(
                        serviceId = 4L,
                        name = "KT",
                        logoUrl = "https://cdn.example.com/services/kt.png",
                    ),
                ),
                startsAt = "2026-03-01T00:00:00",
                endsAt = "2099-12-31T23:59:59",
            ),
            PromotionFixture(
                promotionId = 1003L,
                groupType = PromotionRecommendationGroupType.Conditional,
                promotionType = PromotionType.CardBenefit,
                headline = "현대카드 M을 쓰면 디즈니플러스 구독료 일부를 캐시백으로 돌려받을 수 있어요",
                summary = "월 정기결제를 현대카드 M으로 설정하면 최대 4,000원 캐시백 혜택을 받을 수 있어요.",
                price = PromotionPriceData(
                    originalPrice = 9_900,
                    discountPrice = 5_900,
                    monthlySavedAmount = 4_000,
                ),
                yearlySavedAmount = 48_000,
                yearlyBenefitDescription = "콘텐츠 한두 개 값이 아니라 연간 구독료 절감에 가까워요.",
                conditionDescription = "전월 실적 30만원 이상 + 정기결제 카드 지정이 필요해요.",
                detailDescription = "현대카드 M 계열 카드로 디즈니플러스 정기결제를 걸어두면 전월 실적 구간에 따라 월 최대 4,000원까지 캐시백이 제공됩니다.",
                applySteps = listOf(
                    "현대카드 앱에서 카드 혜택 대상 여부를 확인해요.",
                    "디즈니플러스 결제 수단을 현대카드 M으로 변경해요.",
                    "다음 결제일부터 캐시백 반영 내역을 확인해요.",
                ),
                sourceUrl = "https://www.hyundaicard.com/",
                imageUrl = "https://cdn.example.com/promotions/hyundaicard-disney.png",
                services = listOf(
                    PromotionServiceData(
                        serviceId = 5L,
                        name = "디즈니플러스",
                        logoUrl = "https://cdn.example.com/services/disney-plus.png",
                    ),
                    PromotionServiceData(
                        serviceId = 6L,
                        name = "현대카드",
                        logoUrl = "https://cdn.example.com/services/hyundai-card.png",
                    ),
                ),
                startsAt = "2026-03-01T00:00:00",
                endsAt = "2099-12-31T23:59:59",
            ),
            PromotionFixture(
                promotionId = 1004L,
                groupType = PromotionRecommendationGroupType.Conditional,
                promotionType = PromotionType.Bundle,
                headline = "웨이브와 왓챠 번들을 검토해 보세요",
                summary = "현재는 혜택 안내만 열려 있고 실제 가입 링크는 아직 연결되지 않았어요.",
                price = PromotionPriceData(
                    originalPrice = 14_400,
                    discountPrice = 9_900,
                    monthlySavedAmount = 4_500,
                ),
                yearlySavedAmount = 54_000,
                yearlyBenefitDescription = "1년으로 보면 웨이브 반년치에 가까운 절감이에요.",
                conditionDescription = "운영 공지 기준으로 신규 가입자만 대상일 수 있어요.",
                detailDescription = "웨이브와 왓챠를 함께 묶어 구독하면 월 구독료를 낮출 수 있지만, 현재 FE mock 단계에서는 상세 링크 없이 정보만 제공합니다.",
                applySteps = listOf(
                    "프로모션 운영 공지를 확인해요.",
                    "실제 가입 링크가 열리면 상세 화면에서 바로 이동할 수 있어요.",
                    "그전까지는 기존 구독과 절감액만 비교해 보세요.",
                ),
                sourceUrl = null,
                imageUrl = "https://cdn.example.com/promotions/wavve-watcha-bundle.png",
                services = listOf(
                    PromotionServiceData(
                        serviceId = 7L,
                        name = "웨이브",
                        logoUrl = "https://cdn.example.com/services/wavve.png",
                    ),
                    PromotionServiceData(
                        serviceId = 8L,
                        name = "왓챠",
                        logoUrl = "https://cdn.example.com/services/watcha.png",
                    ),
                ),
                startsAt = "2026-03-01T00:00:00",
                endsAt = "2099-12-31T23:59:59",
            ),
            PromotionFixture(
                promotionId = 1999L,
                groupType = PromotionRecommendationGroupType.SubscriptionList,
                promotionType = PromotionType.Bundle,
                headline = "만료된 프로모션 예시",
                summary = "QA용 만료 프로모션이에요.",
                price = PromotionPriceData(
                    originalPrice = 15_000,
                    discountPrice = 7_900,
                    monthlySavedAmount = 7_100,
                ),
                yearlySavedAmount = 85_200,
                yearlyBenefitDescription = "더 이상 노출되면 안 되는 데이터예요.",
                detailDescription = "목록 필터링과 만료 상태 검증을 위한 프로모션입니다.",
                applySteps = listOf(
                    "만료된 혜택이라 외부 링크 이동은 비활성화돼요.",
                    "딥링크나 알림으로 들어와도 종료 안내만 보여줘요.",
                ),
                sourceUrl = "https://cdn.example.com/expired-promotion",
                imageUrl = "https://cdn.example.com/promotions/expired.png",
                services = listOf(
                    PromotionServiceData(
                        serviceId = 9L,
                        name = "웨이브",
                        logoUrl = "https://cdn.example.com/services/wavve.png",
                    ),
                ),
                startsAt = "2025-01-01T00:00:00",
                endsAt = "2025-12-31T23:59:59",
            ),
        )
    }
}

private fun PromotionRecommendationData.isCardBenefitWithVisibleValue(): Boolean {
    if (promotionType != PromotionType.CardBenefit) {
        return false
    }

    val searchable = listOfNotNull(headline, summary)
        .joinToString(separator = " ")

    return listOf("할인", "적립", "%", "원").any(searchable::contains)
}
