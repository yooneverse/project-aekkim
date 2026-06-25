package com.ssafy.e106.data.repository

import com.ssafy.e106.core.model.ErrorResponse
import com.ssafy.e106.core.result.Result
import com.ssafy.e106.data.api.CheckInApi
import com.ssafy.e106.data.api.MerchantMappingApi
import com.ssafy.e106.data.api.SubscriptionApi
import com.ssafy.e106.data.dto.subscription.CheckInCreateRequest
import com.ssafy.e106.data.dto.subscription.CheckInHistoryItemResponse
import com.ssafy.e106.data.dto.subscription.MerchantMappingBatchConfirmItemRequest
import com.ssafy.e106.data.dto.subscription.MerchantMappingBatchConfirmRequest
import com.ssafy.e106.data.dto.subscription.MerchantMappingBatchLookupItemRequest
import com.ssafy.e106.data.dto.subscription.MerchantMappingBatchLookupItemResponse
import com.ssafy.e106.data.dto.subscription.MerchantMappingBatchLookupRequest
import com.ssafy.e106.data.dto.subscription.SubscriptionCreateRequest
import com.ssafy.e106.data.dto.subscription.SubscriptionDetailResponse
import com.ssafy.e106.data.dto.subscription.SubscriptionListItemResponse
import com.ssafy.e106.data.dto.subscription.SubscriptionListResponse
import com.ssafy.e106.data.dto.subscription.SubscriptionRecommendationResponse
import com.ssafy.e106.data.dto.subscription.SubscriptionRecommendationServiceResponse
import com.ssafy.e106.data.dto.subscription.SubscriptionRecentUsageResponse
import com.ssafy.e106.data.dto.subscription.SubscriptionUpdateRequest
import com.ssafy.e106.feature.analysis.model.AnalysisCandidate
import com.ssafy.e106.feature.analysis.model.AnalysisSessionResult
import com.ssafy.e106.feature.analysis.model.CandidateResolution
import com.ssafy.e106.feature.analysis.primaryHintServiceId
import com.ssafy.e106.feature.analysis.primaryHintServicePlanId
import com.ssafy.e106.feature.analysis.primaryHintServiceName
import com.ssafy.e106.feature.analysis.toBilledAtLabel
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import retrofit2.HttpException

const val PendingReviewApiNote = "[확인 필요: FE/BE] '구독 아님/제외' API 미정"

data class PendingReviewCandidate(
    val reviewId: Long,
    val merchantRaw: String,
    val monthlyAmount: Int,
    val billedAtLabel: String,
    val billingDay: Int? = null,
    val predictedServiceId: Long? = null,
    val predictedServicePlanId: Long? = null,
    val suggestedServiceName: String? = null,
)

data class BatchLookupReviewHint(
    val candidate: AnalysisCandidate,
    val matched: Boolean,
    val serviceId: Long? = null,
    val servicePlanId: Long? = null,
    val serviceName: String? = null,
    val hitCount: Int? = null,
    val offlinePenaltyScore: Float = 0.0f,
)

data class BatchLookupAnalysisOutcome(
    val resolved: List<CandidateResolution> = emptyList(),
    val aiHints: List<BatchLookupReviewHint> = emptyList(),
)

data class DashboardSubscriptionsData(
    val monthlyTotalAmount: Int,
    val subscriptions: List<SubscriptionOverview>,
    val pendingCheckinBanner: PendingCheckinBanner? = null,
    val pendingReviews: List<PendingReviewItem> = emptyList(),
)

data class SubscriptionOverview(
    val subscriptionId: Long,
    val serviceId: Long,
    val servicePlanId: Long,
    val subscriptionType: String = "SINGLE",
    val bundleCode: String? = null,
    val serviceName: String,
    val planName: String? = null,
    val billingCycle: String? = null,
    val logoUrl: String? = null,
    val monthlyPrice: Int,
    val nextBillingDate: String? = null,
    val coveredServices: List<CoveredServiceOverview> = emptyList(),
    val expectedSavingAmount: Int? = null,
    val savingLabel: String? = null,
)

data class CoveredServiceOverview(
    val serviceId: Long,
    val serviceCode: String? = null,
    val serviceName: String,
    val logoUrl: String? = null,
)

data class PendingCheckinBanner(
    val subscriptionId: Long,
    val title: String,
    val description: String,
)

data class PendingReviewItem(
    val reviewId: Long,
    val merchantName: String,
    val suggestedServiceName: String?,
    val monthlyAmount: Int,
    val billedAtLabel: String,
    val billingDay: Int? = null,
)

data class SubscriptionDetailData(
    val subscriptionId: Long,
    val subscriptionType: String = "SINGLE",
    val bundleCode: String? = null,
    val serviceName: String,
    val planName: String,
    val billingCycle: String? = null,
    val logoUrl: String? = null,
    val monthlyPrice: Int,
    val nextBillingDate: String? = null,
    val daysUntilBilling: Int? = null,
    val coveredServices: List<CoveredServiceOverview> = emptyList(),
    val recentUsage: List<SubscriptionUsageEntry> = emptyList(),
    val recommendations: List<SubscriptionRecommendation> = emptyList(),
    val cancelGuideUrl: String? = null,
    val customerServicePhone: String? = null,
    val contactEmail: String? = null,
)

data class SubscriptionUsageEntry(
    val cycleYm: String,
    val response: String,
    val serviceName: String? = null,
)

data class SubscriptionRecommendation(
    val promotionId: Long,
    val promotionType: String,
    val title: String,
    val monthlySavingAmount: Int? = null,
    val billingCycle: String? = null,
    val headline: String,
    val imageUrl: String? = null,
    val services: List<PromotionServiceData> = emptyList(),
)

enum class SubscriptionMutationType {
    Created,
    Updated,
    Deleted,
}

// Subscription entities are re-synced from the server after CRUD; local optimistic updates stay in UI-only state.
sealed interface SubscriptionSyncEvent {
    data class SubscriptionChanged(
        val subscriptionId: Long,
        val mutationType: SubscriptionMutationType,
    ) : SubscriptionSyncEvent

    data object PendingReviewsChanged : SubscriptionSyncEvent
}

sealed interface CheckInSubmitResult {
    data object Success : CheckInSubmitResult

    data class AlreadySubmitted(
        val message: String,
    ) : CheckInSubmitResult

    data class Failure(
        val message: String,
    ) : CheckInSubmitResult
}

@Singleton
class SubscriptionRepository @Inject constructor(
    private val subscriptionApi: SubscriptionApi,
    private val checkInApi: CheckInApi,
    private val merchantMappingApi: MerchantMappingApi,
) {

    @Volatile
    private var cachedSubscriptions: List<SubscriptionOverview> = emptyList()
    private var latestPendingReviewSeedKey: String? = null

    // Raw candidate payments must be seeded by an upstream analysis source; this repository only resolves and confirms them.
    private val pendingReviewCandidates = mutableListOf<PendingReviewCandidate>()
    private val pendingReviews = mutableListOf<PendingReviewItem>()
    private val _syncEvents = MutableSharedFlow<SubscriptionSyncEvent>(extraBufferCapacity = 4)
    val syncEvents: SharedFlow<SubscriptionSyncEvent> = _syncEvents.asSharedFlow()
    suspend fun getDashboardData(): Result<DashboardSubscriptionsData> {
        return try {
            val response = subscriptionApi.getSubscriptions()
            if (response.success && response.data != null) {
                val subscriptions = response.data.toSubscriptionOverviewList()
                cachedSubscriptions = subscriptions
                Result.Success(
                    response.data.toDomain(
                        subscriptions = subscriptions,
                        pendingReviews = pendingReviews.toList(),
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

    suspend fun getSubscriptionDetail(subscriptionId: Long): Result<SubscriptionDetailData> {
        return try {
            val response = subscriptionApi.getSubscriptionDetail(subscriptionId = subscriptionId)
            if (response.success && response.data != null) {
                Result.Success(response.data.toDomain())
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

    suspend fun getCheckInHistory(
        subscriptionId: Long,
        size: Int = DEFAULT_CHECKIN_HISTORY_SIZE,
    ): Result<List<SubscriptionUsageEntry>> {
        return when (val serviceIdResult = resolveServiceId(subscriptionId)) {
            is Result.Success -> fetchCheckInHistoryByServiceId(serviceIdResult.data, size)
            is Result.Error -> serviceIdResult
            Result.Loading -> Result.Loading
        }
    }

    suspend fun submitCheckIn(
        subscriptionId: Long,
        cycleYm: String,
        response: String,
    ): CheckInSubmitResult {
        return when (val serviceIdResult = resolveServiceId(subscriptionId)) {
            is Result.Success -> {
                createCheckIn(
                    serviceId = serviceIdResult.data,
                    cycleYm = cycleYm,
                    response = response,
                )
            }

            is Result.Error -> CheckInSubmitResult.Failure(serviceIdResult.message)
            Result.Loading -> CheckInSubmitResult.Failure(DEFAULT_CHECKIN_SUBMIT_ERROR_MESSAGE)
        }
    }

    suspend fun createSubscription(
        subscriptionType: String,
        serviceId: Long? = null,
        servicePlanId: Long? = null,
        bundleCode: String? = null,
        billingDay: Int? = null,
        nextBillingDate: String? = null,
    ): Result<Unit> {
        val resolvedNextBillingDate = resolveNextBillingDate(
            billingDay = billingDay,
            nextBillingDate = nextBillingDate,
        ) ?: return Result.Error("결제일을 선택해 주세요.")
        return try {
            val response = subscriptionApi.createSubscription(
                SubscriptionCreateRequest(
                    subscriptionType = subscriptionType,
                    serviceId = serviceId,
                    servicePlanId = servicePlanId,
                    bundleCode = bundleCode,
                    nextBillingDate = resolvedNextBillingDate,
                ),
            )
            if (response.success && response.data != null) {
                _syncEvents.emit(
                    SubscriptionSyncEvent.SubscriptionChanged(
                        subscriptionId = response.data.subscriptionId,
                        mutationType = SubscriptionMutationType.Created,
                    ),
                )
                Result.Success(Unit)
            } else {
                Result.Error(response.message ?: DEFAULT_CREATE_ERROR_MESSAGE)
            }
        } catch (e: UnknownHostException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: ConnectException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: SocketTimeoutException) {
            Result.Error(DEFAULT_TIMEOUT_ERROR_MESSAGE)
        } catch (e: HttpException) {
            Result.Error(parseHttpError(e) ?: DEFAULT_CREATE_ERROR_MESSAGE, e.code())
        } catch (e: IOException) {
            Result.Error(e.message ?: DEFAULT_CREATE_ERROR_MESSAGE)
        } catch (e: Exception) {
            Result.Error(e.message ?: DEFAULT_CREATE_ERROR_MESSAGE)
        }
    }

    suspend fun updateSubscription(
        subscriptionId: Long,
        servicePlanId: Long? = null,
        bundleCode: String? = null,
        billingDay: Int? = null,
        nextBillingDate: String? = null,
    ): Result<Unit> {
        val resolvedNextBillingDate = resolveNextBillingDate(
            billingDay = billingDay,
            nextBillingDate = nextBillingDate,
        ) ?: return Result.Error("결제일을 선택해 주세요.")
        return try {
            val response = subscriptionApi.updateSubscription(
                subscriptionId = subscriptionId,
                request = SubscriptionUpdateRequest(
                    servicePlanId = servicePlanId,
                    bundleCode = bundleCode,
                    nextBillingDate = resolvedNextBillingDate,
                ),
            )
            if (response.success && response.data != null) {
                _syncEvents.emit(
                    SubscriptionSyncEvent.SubscriptionChanged(
                        subscriptionId = subscriptionId,
                        mutationType = SubscriptionMutationType.Updated,
                    ),
                )
                Result.Success(Unit)
            } else {
                Result.Error(response.message ?: DEFAULT_UPDATE_ERROR_MESSAGE)
            }
        } catch (e: UnknownHostException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: ConnectException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: SocketTimeoutException) {
            Result.Error(DEFAULT_TIMEOUT_ERROR_MESSAGE)
        } catch (e: HttpException) {
            Result.Error(parseHttpError(e) ?: DEFAULT_UPDATE_ERROR_MESSAGE, e.code())
        } catch (e: IOException) {
            Result.Error(e.message ?: DEFAULT_UPDATE_ERROR_MESSAGE)
        } catch (e: Exception) {
            Result.Error(e.message ?: DEFAULT_UPDATE_ERROR_MESSAGE)
        }
    }

    suspend fun deleteSubscription(subscriptionId: Long): Result<Unit> {
        return try {
            val response = subscriptionApi.deleteSubscription(subscriptionId = subscriptionId)
            if (response.success) {
                _syncEvents.emit(
                    SubscriptionSyncEvent.SubscriptionChanged(
                        subscriptionId = subscriptionId,
                        mutationType = SubscriptionMutationType.Deleted,
                    ),
                )
                Result.Success(Unit)
            } else {
                Result.Error(response.message ?: DEFAULT_DELETE_ERROR_MESSAGE)
            }
        } catch (e: UnknownHostException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: ConnectException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: SocketTimeoutException) {
            Result.Error(DEFAULT_TIMEOUT_ERROR_MESSAGE)
        } catch (e: HttpException) {
            Result.Error(parseHttpError(e) ?: DEFAULT_DELETE_ERROR_MESSAGE, e.code())
        } catch (e: IOException) {
            Result.Error(e.message ?: DEFAULT_DELETE_ERROR_MESSAGE)
        } catch (e: Exception) {
            Result.Error(e.message ?: DEFAULT_DELETE_ERROR_MESSAGE)
        }
    }

    suspend fun refreshPendingReviews(
        candidates: List<PendingReviewCandidate>,
    ): Result<List<PendingReviewItem>> {
        pendingReviewCandidates.clear()
        pendingReviewCandidates.addAll(candidates)

        if (candidates.isEmpty()) {
            val hadPendingReviews = pendingReviews.isNotEmpty()
            pendingReviews.clear()
            if (hadPendingReviews) {
                _syncEvents.emit(SubscriptionSyncEvent.PendingReviewsChanged)
            }
            return Result.Success(emptyList())
        }

        val supportedCandidates = candidates.filter { candidate ->
            candidate.predictedServiceId != null && candidate.predictedServicePlanId != null
        }

        if (supportedCandidates.size != candidates.size) {
            pendingReviews.clear()
            pendingReviews.addAll(candidates.map { candidate -> candidate.toPendingReviewItem() })
            _syncEvents.emit(SubscriptionSyncEvent.PendingReviewsChanged)
            return Result.Success(pendingReviews.toList())
        }

        return when (val lookupResult = executeMerchantBatchLookupCandidates(supportedCandidates)) {
            is Result.Success -> {
                if (lookupResult.data.size != supportedCandidates.size) {
                    Result.Error(DEFAULT_PENDING_REVIEW_LOOKUP_ERROR_MESSAGE)
                } else {
                    pendingReviews.clear()
                    pendingReviews.addAll(
                        supportedCandidates.zip(lookupResult.data).mapNotNull { (candidate, result) ->
                            result.toPendingReviewItem(candidate)
                        },
                    )
                    _syncEvents.emit(SubscriptionSyncEvent.PendingReviewsChanged)
                    Result.Success(pendingReviews.toList())
                }
            }

            is Result.Error -> Result.Error(lookupResult.message, lookupResult.code)
            Result.Loading -> Result.Loading
        }
    }

    suspend fun getPendingReviews(
        forceRefresh: Boolean = false,
    ): Result<List<PendingReviewItem>> {
        if (pendingReviewCandidates.isEmpty()) {
            return Result.Success(pendingReviews.toList())
        }

        return if (forceRefresh || pendingReviews.isEmpty()) {
            refreshPendingReviews(pendingReviewCandidates.toList())
        } else {
            Result.Success(pendingReviews.toList())
        }
    }

    suspend fun resolveCandidatesWithBatchLookup(
        candidates: List<AnalysisCandidate>,
    ): Result<BatchLookupAnalysisOutcome> {
        if (candidates.isEmpty()) {
            return Result.Success(BatchLookupAnalysisOutcome())
        }

        val lookupItems = candidates.mapNotNull { candidate ->
            candidate.toBatchLookupRequestItem()
        }

        if (lookupItems.size != candidates.size) {
            return Result.Success(createBatchLookupFallback(candidates))
        }

        return when (val lookupResult = executeMerchantBatchLookupItems(lookupItems)) {
            is Result.Success -> {
                if (lookupResult.data.size != candidates.size) {
                    Result.Error(DEFAULT_PENDING_REVIEW_LOOKUP_ERROR_MESSAGE)
                } else {
                    Result.Success(
                        candidates.zip(lookupResult.data).fold(BatchLookupAnalysisOutcome()) { outcome, pair ->
                            val (candidate, lookupItem) = pair
                            when {
                                lookupItem.matched && (lookupItem.hitCount ?: 0) >= LOOKUP_HIGH_CONFIDENCE_THRESHOLD -> {
                                    outcome.copy(
                                        resolved = outcome.resolved + lookupItem.toBatchLookupResolution(candidate),
                                    )
                                }

                                else -> {
                                    outcome.copy(
                                        aiHints = outcome.aiHints + lookupItem.toBatchLookupHint(candidate),
                                    )
                                }
                            }
                        },
                    )
                }
            }

            is Result.Error -> Result.Error(lookupResult.message, lookupResult.code)
            Result.Loading -> Result.Loading
        }
    }

    fun createBatchLookupFallback(candidates: List<AnalysisCandidate>): BatchLookupAnalysisOutcome {
        return BatchLookupAnalysisOutcome(
            resolved = emptyList(),
            aiHints = candidates.map { candidate ->
                BatchLookupReviewHint(
                    candidate = candidate,
                    matched = false,
                    serviceId = candidate.primaryHintServiceId(),
                    servicePlanId = candidate.primaryHintServicePlanId(),
                    serviceName = candidate.primaryHintServiceName(),
                    offlinePenaltyScore = (candidate.ruleScores["offlinePenalty"] ?: 0.0).toFloat(),
                )
            },
        )
    }

    fun seedPendingReviewsFromAnalysisSession(result: AnalysisSessionResult?) {
        val seedKey = result.toPendingReviewSeedKey()
        if (seedKey != null && seedKey == latestPendingReviewSeedKey) {
            return
        }

        pendingReviewCandidates.clear()
        pendingReviews.clear()
        latestPendingReviewSeedKey = seedKey

        result?.pendingReviewSummary?.items?.forEach { resolution ->
            pendingReviewCandidates += resolution.toPendingReviewCandidate()
            pendingReviews += resolution.toPendingReviewItem()
        }

        _syncEvents.tryEmit(SubscriptionSyncEvent.PendingReviewsChanged)
    }

    suspend fun confirmPendingReview(
        reviewId: Long,
        serviceId: Long,
        servicePlanId: Long,
        billingDay: Int,
    ): Result<Unit> {
        val candidate = pendingReviewCandidates.firstOrNull { pendingReview ->
            pendingReview.reviewId == reviewId
        } ?: return Result.Error(DEFAULT_PENDING_REVIEW_SOURCE_ERROR_MESSAGE)

        return try {
            val response = merchantMappingApi.batchConfirm(
                MerchantMappingBatchConfirmRequest(
                    items = listOf(
                        MerchantMappingBatchConfirmItemRequest(
                            merchantRaw = candidate.merchantRaw,
                            serviceId = serviceId,
                            servicePlanId = servicePlanId,
                            nextBillingDate = buildNextBillingDate(billingDay),
                        ),
                    ),
                ),
            )
            val createdSubscription = response.data?.results?.firstOrNull()
            if (response.success && createdSubscription != null) {
                removePendingReviewLocally(reviewId)
                _syncEvents.emit(
                    SubscriptionSyncEvent.SubscriptionChanged(
                        subscriptionId = createdSubscription.subscriptionId,
                        mutationType = SubscriptionMutationType.Created,
                    ),
                )
                _syncEvents.emit(SubscriptionSyncEvent.PendingReviewsChanged)
                Result.Success(Unit)
            } else {
                Result.Error(response.message ?: DEFAULT_PENDING_REVIEW_CONFIRM_ERROR_MESSAGE)
            }
        } catch (e: UnknownHostException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: ConnectException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: SocketTimeoutException) {
            Result.Error(DEFAULT_TIMEOUT_ERROR_MESSAGE)
        } catch (e: HttpException) {
            Result.Error(parseHttpError(e) ?: DEFAULT_PENDING_REVIEW_CONFIRM_ERROR_MESSAGE, e.code())
        } catch (e: IOException) {
            Result.Error(e.message ?: DEFAULT_PENDING_REVIEW_CONFIRM_ERROR_MESSAGE)
        } catch (e: Exception) {
            Result.Error(e.message ?: DEFAULT_PENDING_REVIEW_CONFIRM_ERROR_MESSAGE)
        }
    }

    suspend fun dismissPendingReview(reviewId: Long): Result<Unit> {
        val removed = removePendingReviewLocally(reviewId)
        if (removed) {
            _syncEvents.emit(SubscriptionSyncEvent.PendingReviewsChanged)
        }
        return Result.Success(Unit)
    }

    private fun resolveNextBillingDate(
        billingDay: Int?,
        nextBillingDate: String?,
    ): String? {
        if (!nextBillingDate.isNullOrBlank()) return nextBillingDate
        return billingDay?.let(::buildNextBillingDate)
    }

    private fun buildNextBillingDate(billingDay: Int): String {
        val normalizedDay = billingDay.coerceIn(1, 31)
        val today = LocalDate.now()
        val startMonth = YearMonth.from(today)

        repeat(MAX_BILLING_SEARCH_MONTHS) { offset ->
            val yearMonth = startMonth.plusMonths(offset.toLong())
            if (normalizedDay > yearMonth.lengthOfMonth()) return@repeat

            val candidate = yearMonth.atDay(normalizedDay)
            if (!candidate.isBefore(today)) {
                return candidate.toString()
            }
        }

        return today.toString()
    }

    private fun parseHttpError(exception: HttpException): String? {
        val raw = readErrorBody(exception)
        if (raw.isBlank()) return null

        val parsed = parseErrorResponse(raw)
        return parsed?.message ?: raw
    }

    private suspend fun executeMerchantBatchLookupCandidates(
        candidates: List<PendingReviewCandidate>,
    ): Result<List<MerchantMappingBatchLookupItemResponse>> {
        return executeMerchantBatchLookupChunked(
            candidates.map { candidate ->
                MerchantMappingBatchLookupItemRequest(
                    merchantRaw = candidate.merchantRaw,
                    predictedServiceId = candidate.predictedServiceId!!,
                    predictedServicePlanId = candidate.predictedServicePlanId!!,
                )
            },
        )
    }

    private suspend fun executeMerchantBatchLookupItems(
        items: List<MerchantMappingBatchLookupItemRequest>,
    ): Result<List<MerchantMappingBatchLookupItemResponse>> {
        return executeMerchantBatchLookupChunked(items)
    }

    private suspend fun executeMerchantBatchLookupChunked(
        items: List<MerchantMappingBatchLookupItemRequest>,
    ): Result<List<MerchantMappingBatchLookupItemResponse>> {
        return try {
            val aggregatedResults = mutableListOf<MerchantMappingBatchLookupItemResponse>()
            for (chunk in items.chunked(MAX_MERCHANT_BATCH_LOOKUP_SIZE)) {
                val response = merchantMappingApi.batchLookup(
                    MerchantMappingBatchLookupRequest(items = chunk),
                )
                if (!response.success || response.data == null) {
                    return Result.Error(response.message ?: DEFAULT_PENDING_REVIEW_LOOKUP_ERROR_MESSAGE)
                }
                if (response.data.results.size != chunk.size) {
                    return Result.Error(DEFAULT_PENDING_REVIEW_LOOKUP_ERROR_MESSAGE)
                }
                aggregatedResults += response.data.results
            }
            Result.Success(aggregatedResults)
        } catch (e: UnknownHostException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: ConnectException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: SocketTimeoutException) {
            Result.Error(DEFAULT_TIMEOUT_ERROR_MESSAGE)
        } catch (e: HttpException) {
            Result.Error(parseHttpError(e) ?: DEFAULT_PENDING_REVIEW_LOOKUP_ERROR_MESSAGE, e.code())
        } catch (e: IOException) {
            Result.Error(e.message ?: DEFAULT_PENDING_REVIEW_LOOKUP_ERROR_MESSAGE)
        } catch (e: Exception) {
            Result.Error(e.message ?: DEFAULT_PENDING_REVIEW_LOOKUP_ERROR_MESSAGE)
        }
    }

    private fun parseErrorResponse(raw: String): ErrorResponse? {
        if (raw.isBlank()) return null
        return runCatching {
            json.decodeFromString<ErrorResponse>(raw)
        }.getOrNull()
    }

    private fun readErrorBody(exception: HttpException): String {
        return exception.response()?.errorBody()?.string()?.trim().orEmpty()
    }

    private fun removePendingReviewLocally(reviewId: Long): Boolean {
        val removedCandidates =
            pendingReviewCandidates.removeAll { pendingReview -> pendingReview.reviewId == reviewId }
        val removedReviews =
            pendingReviews.removeAll { pendingReview -> pendingReview.reviewId == reviewId }
        return removedCandidates || removedReviews
    }

    private fun AnalysisSessionResult?.toPendingReviewSeedKey(): String? {
        return this?.let { result ->
            val sessionId = result.sessionId?.takeIf { id -> id.isNotBlank() } ?: "analysis"
            "$sessionId:${result.analyzedAtEpochMs}"
        }
    }

    private suspend fun resolveServiceId(subscriptionId: Long): Result<Long> {
        cachedSubscriptions.firstOrNull { subscription ->
            subscription.subscriptionId == subscriptionId
        }?.let { subscription ->
            return Result.Success(subscription.serviceId)
        }

        return try {
            val response = subscriptionApi.getSubscriptions()
            if (response.success && response.data != null) {
                val subscriptions = response.data.toSubscriptionOverviewList()
                cachedSubscriptions = subscriptions

                subscriptions.firstOrNull { subscription ->
                    subscription.subscriptionId == subscriptionId
                }?.let { subscription ->
                    Result.Success(subscription.serviceId)
                } ?: Result.Error(DEFAULT_CHECKIN_CONTEXT_ERROR_MESSAGE)
            } else {
                Result.Error(response.message ?: DEFAULT_CHECKIN_CONTEXT_ERROR_MESSAGE)
            }
        } catch (e: UnknownHostException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: ConnectException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: SocketTimeoutException) {
            Result.Error(DEFAULT_TIMEOUT_ERROR_MESSAGE)
        } catch (e: HttpException) {
            Result.Error(parseHttpError(e) ?: DEFAULT_CHECKIN_CONTEXT_ERROR_MESSAGE, e.code())
        } catch (e: IOException) {
            Result.Error(e.message ?: DEFAULT_CHECKIN_CONTEXT_ERROR_MESSAGE)
        } catch (e: Exception) {
            Result.Error(e.message ?: DEFAULT_CHECKIN_CONTEXT_ERROR_MESSAGE)
        }
    }

    private suspend fun createCheckIn(
        serviceId: Long,
        cycleYm: String,
        response: String,
    ): CheckInSubmitResult {
        return try {
            val apiResponse = checkInApi.createCheckIn(
                request = CheckInCreateRequest(
                    serviceId = serviceId,
                    cycleYm = cycleYm,
                    response = response,
                ),
            )

            if (apiResponse.success && apiResponse.data != null) {
                CheckInSubmitResult.Success
            } else {
                CheckInSubmitResult.Failure(
                    apiResponse.message ?: DEFAULT_CHECKIN_SUBMIT_ERROR_MESSAGE,
                )
            }
        } catch (e: UnknownHostException) {
            CheckInSubmitResult.Failure(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: ConnectException) {
            CheckInSubmitResult.Failure(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: SocketTimeoutException) {
            CheckInSubmitResult.Failure(DEFAULT_TIMEOUT_ERROR_MESSAGE)
        } catch (e: HttpException) {
            val raw = readErrorBody(e)
            val parsed = parseErrorResponse(raw)
            if (parsed?.code == CHECKIN_ALREADY_SUBMITTED_ERROR_CODE) {
                CheckInSubmitResult.AlreadySubmitted(
                    parsed.message.ifBlank { DEFAULT_CHECKIN_ALREADY_SUBMITTED_MESSAGE },
                )
            } else {
                CheckInSubmitResult.Failure(
                    parsed?.message ?: raw.ifBlank { DEFAULT_CHECKIN_SUBMIT_ERROR_MESSAGE },
                )
            }
        } catch (e: IOException) {
            CheckInSubmitResult.Failure(e.message ?: DEFAULT_CHECKIN_SUBMIT_ERROR_MESSAGE)
        } catch (e: Exception) {
            CheckInSubmitResult.Failure(e.message ?: DEFAULT_CHECKIN_SUBMIT_ERROR_MESSAGE)
        }
    }

    private suspend fun fetchCheckInHistoryByServiceId(
        serviceId: Long,
        size: Int,
    ): Result<List<SubscriptionUsageEntry>> {
        return try {
            val apiResponse = checkInApi.getCheckIns(
                serviceId = serviceId,
                size = size,
            )

            if (apiResponse.success && apiResponse.data != null) {
                Result.Success(apiResponse.data.checkins.map { checkIn -> checkIn.toDomain() })
            } else {
                Result.Error(apiResponse.message ?: DEFAULT_CHECKIN_HISTORY_ERROR_MESSAGE)
            }
        } catch (e: UnknownHostException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: ConnectException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: SocketTimeoutException) {
            Result.Error(DEFAULT_TIMEOUT_ERROR_MESSAGE)
        } catch (e: HttpException) {
            Result.Error(parseHttpError(e) ?: DEFAULT_CHECKIN_HISTORY_ERROR_MESSAGE, e.code())
        } catch (e: IOException) {
            Result.Error(e.message ?: DEFAULT_CHECKIN_HISTORY_ERROR_MESSAGE)
        } catch (e: Exception) {
            Result.Error(e.message ?: DEFAULT_CHECKIN_HISTORY_ERROR_MESSAGE)
        }
    }

    private fun SubscriptionListResponse.toDomain(
        subscriptions: List<SubscriptionOverview>,
        pendingReviews: List<PendingReviewItem>,
    ): DashboardSubscriptionsData {
        return DashboardSubscriptionsData(
            monthlyTotalAmount = monthlyTotalAmount,
            subscriptions = subscriptions,
            pendingCheckinBanner = null,
            pendingReviews = pendingReviews,
        )
    }

    private fun SubscriptionListResponse.toSubscriptionOverviewList(): List<SubscriptionOverview> {
        return subscriptions.map { item -> item.toDomain() }
    }

    private fun SubscriptionListItemResponse.toDomain(): SubscriptionOverview {
        val covered = coveredServices.map { coveredService ->
            CoveredServiceOverview(
                serviceId = coveredService.serviceId,
                serviceCode = coveredService.serviceCode,
                serviceName = coveredService.serviceName,
                logoUrl = resolveMediaUrl(coveredService.logoUrl),
            )
        }
        return SubscriptionOverview(
            subscriptionId = subscriptionId,
            serviceId = serviceId ?: covered.firstOrNull()?.serviceId ?: 0L,
            servicePlanId = servicePlanId ?: 0L,
            subscriptionType = subscriptionType,
            bundleCode = bundleCode,
            serviceName = serviceName,
            planName = planName,
            billingCycle = billingCycle,
            logoUrl = resolveMediaUrl(logoUrl),
            monthlyPrice = monthlyPrice,
            nextBillingDate = nextBillingDate,
            coveredServices = covered,
            expectedSavingAmount = expectedSavingAmount,
            savingLabel = savingLabel,
        )
    }

    private fun SubscriptionDetailResponse.toDomain(): SubscriptionDetailData {
        return SubscriptionDetailData(
            subscriptionId = subscriptionId,
            subscriptionType = subscriptionType,
            bundleCode = bundleCode,
            serviceName = serviceName,
            planName = planName,
            billingCycle = billingCycle,
            logoUrl = resolveMediaUrl(logoUrl),
            monthlyPrice = monthlyPrice,
            nextBillingDate = nextBillingDate,
            daysUntilBilling = daysUntilBilling,
            coveredServices = coveredServices.map { coveredService ->
                CoveredServiceOverview(
                    serviceId = coveredService.serviceId,
                    serviceCode = coveredService.serviceCode,
                    serviceName = coveredService.serviceName,
                    logoUrl = resolveMediaUrl(coveredService.logoUrl),
                )
            },
            recentUsage = recentUsage.map { usage -> usage.toDomain() },
            recommendations = listOfNotNull(recommendation?.toDomain()),
            cancelGuideUrl = cancelGuideUrl,
            customerServicePhone = customerServicePhone,
            contactEmail = contactEmail,
        )
    }

    private fun SubscriptionRecentUsageResponse.toDomain(): SubscriptionUsageEntry {
        return SubscriptionUsageEntry(
            cycleYm = cycleYm,
            response = response,
            serviceName = serviceName,
        )
    }

    private fun CheckInHistoryItemResponse.toDomain(): SubscriptionUsageEntry {
        return SubscriptionUsageEntry(
            cycleYm = cycleYm,
            response = response,
            serviceName = null,
        )
    }

    private fun SubscriptionRecommendationResponse.toDomain(): SubscriptionRecommendation {
        return SubscriptionRecommendation(
            promotionId = promotionId,
            promotionType = promotionType,
            title = title,
            monthlySavingAmount = monthlySavingAmount,
            billingCycle = billingCycle,
            headline = headline,
            imageUrl = resolveMediaUrl(imageUrl),
            services = services.map { service -> service.toPromotionServiceData() },
        )
    }

    private fun SubscriptionRecommendationServiceResponse.toPromotionServiceData(): PromotionServiceData {
        return PromotionServiceData(
            serviceId = serviceId,
            code = null,
            name = serviceName,
            logoUrl = resolveMediaUrl(logoUrl),
        )
    }

    private fun MerchantMappingBatchLookupItemResponse.toPendingReviewItem(
        candidate: PendingReviewCandidate,
    ): PendingReviewItem? {
        if (matched) {
            return null
        }

        return PendingReviewItem(
            reviewId = candidate.reviewId,
            merchantName = candidate.merchantRaw,
            suggestedServiceName = candidate.suggestedServiceName,
            monthlyAmount = candidate.monthlyAmount,
            billedAtLabel = candidate.billedAtLabel,
            billingDay = candidate.billingDay ?: candidate.billedAtLabel.toBillingDayOrNull(),
        )
    }

    private fun MerchantMappingBatchLookupItemResponse.toBatchLookupResolution(
        candidate: AnalysisCandidate,
    ): CandidateResolution {
        val confidence = ((hitCount ?: LOOKUP_HIGH_CONFIDENCE_THRESHOLD).coerceAtLeast(LOOKUP_HIGH_CONFIDENCE_THRESHOLD) * 0.15f)
            .coerceIn(0.85f, 0.97f)
        return CandidateResolution(
            reviewId = candidate.reviewId,
            decision = CandidateResolution.Decision.CONFIRMED_SUBSCRIPTION,
            resolutionSource = CandidateResolution.ResolutionSource.BATCH_LOOKUP,
            merchantRaw = candidate.paymentRecord.merchantRaw,
            merchantNormalized = candidate.normalizedPaymentRecord.merchantNormalized,
            monthlyAmount = candidate.normalizedPaymentRecord.normalizedAmount,
            billedAtLabel = candidate.paymentRecord.toBilledAtLabel(),
            serviceId = serviceId,
            servicePlanId = servicePlanId,
            serviceName = candidate.resolveServiceName(serviceId),
            subscriptionConfidence = confidence,
            serviceConfidence = confidence,
            planConfidence = confidence,
            highConfidence = true,
            reasonCodes = buildList {
                add("BATCH_LOOKUP_CONFIRMED")
                hitCount?.let { count -> add("BATCH_LOOKUP_HIT_COUNT_$count") }
            },
        )
    }

    private fun MerchantMappingBatchLookupItemResponse.toBatchLookupHint(
        candidate: AnalysisCandidate,
    ): BatchLookupReviewHint {
        return BatchLookupReviewHint(
            candidate = candidate,
            matched = matched,
            serviceId = serviceId,
            servicePlanId = servicePlanId,
            serviceName = candidate.resolveServiceName(serviceId),
            hitCount = hitCount,
            offlinePenaltyScore = (candidate.ruleScores["offlinePenalty"] ?: 0.0).toFloat(),
        )
    }

    private fun CandidateResolution.toPendingReviewCandidate(): PendingReviewCandidate {
        return PendingReviewCandidate(
            reviewId = reviewId,
            merchantRaw = merchantRaw,
            monthlyAmount = monthlyAmount,
            billedAtLabel = billedAtLabel,
            billingDay = billedAtLabel.toBillingDayOrNull(),
            predictedServiceId = serviceId,
            predictedServicePlanId = servicePlanId,
            suggestedServiceName = serviceName,
        )
    }

    private fun CandidateResolution.toPendingReviewItem(): PendingReviewItem {
        return PendingReviewItem(
            reviewId = reviewId,
            merchantName = merchantRaw,
            suggestedServiceName = serviceName,
            monthlyAmount = monthlyAmount,
            billedAtLabel = billedAtLabel,
            billingDay = billedAtLabel.toBillingDayOrNull(),
        )
    }

    private fun PendingReviewCandidate.toPendingReviewItem(): PendingReviewItem {
        return PendingReviewItem(
            reviewId = reviewId,
            merchantName = merchantRaw,
            suggestedServiceName = suggestedServiceName,
            monthlyAmount = monthlyAmount,
            billedAtLabel = billedAtLabel,
            billingDay = billingDay ?: billedAtLabel.toBillingDayOrNull(),
        )
    }

    private fun String.toBillingDayOrNull(): Int? {
        return runCatching {
            LocalDate.parse(this, BILLED_AT_FORMATTER).dayOfMonth
        }.getOrNull()
    }

    private fun AnalysisCandidate.toBatchLookupRequestItem(): MerchantMappingBatchLookupItemRequest? {
        val predictedServiceId = primaryHintServiceId() ?: return null
        val predictedServicePlanId = primaryHintServicePlanId() ?: return null
        return MerchantMappingBatchLookupItemRequest(
            merchantRaw = paymentRecord.merchantRaw,
            predictedServiceId = predictedServiceId,
            predictedServicePlanId = predictedServicePlanId,
        )
    }

    private fun AnalysisCandidate.resolveServiceName(serviceId: Long?): String? {
        return serviceCatalogHints.firstOrNull { service -> service.serviceId == serviceId }?.name
            ?: primaryHintServiceName()
    }

    companion object {
        private const val DEFAULT_LIST_ERROR_MESSAGE = "구독 목록 조회에 실패했어요."
        private const val DEFAULT_DETAIL_ERROR_MESSAGE = "구독 상세 조회에 실패했어요."
        private const val DEFAULT_CREATE_ERROR_MESSAGE = "구독 생성에 실패했어요."
        private const val DEFAULT_UPDATE_ERROR_MESSAGE = "구독 수정에 실패했어요."
        private const val DEFAULT_DELETE_ERROR_MESSAGE = "구독 삭제에 실패했어요."
        private const val DEFAULT_PENDING_REVIEW_LOOKUP_ERROR_MESSAGE = "확인 필요 결제 조회에 실패했어요."
        private const val DEFAULT_PENDING_REVIEW_CONFIRM_ERROR_MESSAGE = "확인 필요 결제 저장에 실패했어요."
        private const val DEFAULT_PENDING_REVIEW_SOURCE_ERROR_MESSAGE = "확인 필요 결제 원본 정보를 찾지 못했어요."
        private const val DEFAULT_CHECKIN_CONTEXT_ERROR_MESSAGE = "체크인 제출에 필요한 구독 정보를 찾지 못했어요."
        private const val DEFAULT_CHECKIN_HISTORY_ERROR_MESSAGE = "체크인 이력 조회에 실패했어요."
        private const val DEFAULT_CHECKIN_ALREADY_SUBMITTED_MESSAGE = "이미 이번 달 체크인을 제출했어요."
        private const val DEFAULT_CHECKIN_SUBMIT_ERROR_MESSAGE = "체크인 제출에 실패했어요."
        private const val DEFAULT_NETWORK_ERROR_MESSAGE = "네트워크 연결을 확인해 주세요."
        private const val DEFAULT_TIMEOUT_ERROR_MESSAGE = "서비스 응답이 지연되고 있어요."
        private const val MAX_BILLING_SEARCH_MONTHS = 24
        private const val MAX_MERCHANT_BATCH_LOOKUP_SIZE = 100
        private const val DEFAULT_CHECKIN_HISTORY_SIZE = 3
        private const val CHECKIN_ALREADY_SUBMITTED_ERROR_CODE = "CHECKIN_ALREADY_SUBMITTED"
        private const val LOOKUP_HIGH_CONFIDENCE_THRESHOLD = 3

        private val json = Json { ignoreUnknownKeys = true }
        private val BILLED_AT_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

        val pendingReviewApiStatusNote: String = PendingReviewApiNote
        val subscriptionsListEndpoint: String = "GET /api/v1/subscriptions"
        val subscriptionDetailEndpoint: String = "GET /api/v1/subscriptions/{subscriptionId}"
        val createSubscriptionEndpoint: String = "POST /api/v1/subscriptions"
        val updateSubscriptionEndpoint: String = "PATCH /api/v1/subscriptions/{subscriptionId}"
        val deleteSubscriptionEndpoint: String = "DELETE /api/v1/subscriptions/{subscriptionId}"
        val merchantMappingBatchLookupEndpoint: String = "POST /api/v1/merchant-mappings/batch-lookup"
        val merchantMappingBatchConfirmEndpoint: String = "POST /api/v1/merchant-mappings/batch-confirm"
        val checkInHistoryEndpoint: String = "GET /api/v1/checkins"
        val createCheckInEndpoint: String = "POST /api/v1/checkins"
    }
}
