package com.ssafy.e106.data.repository

import com.ssafy.e106.core.model.ErrorResponse
import com.ssafy.e106.core.result.Result
import com.ssafy.e106.data.api.ServiceApi
import com.ssafy.e106.data.dto.service.ServiceDetailResponse
import com.ssafy.e106.data.dto.service.ServiceListItemResponse
import com.ssafy.e106.data.dto.subscription.BundleListItemResponse
import com.ssafy.e106.feature.analysis.model.ServiceCatalog
import com.ssafy.e106.feature.analysis.model.TrackedSubscriptionPackages
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import retrofit2.HttpException

const val PendingManualAddServiceScopeNote =
    "[확인 필요: FE/BE] 수동 추가 플로우는 현재 카테고리별 서비스 API 응답을 합쳐서 동작"

data class OttService(
    val serviceId: Long,
    val code: String,
    val name: String,
    val logoUrl: String? = null,
)

data class ServicePlanBundle(
    val serviceId: Long,
    val code: String,
    val name: String,
    val category: String,
    val logoUrl: String? = null,
    val plans: List<ServicePlanOption>,
)

data class ServicePlanOption(
    val servicePlanId: Long,
    val planName: String,
    val billingCycle: String,
    val monthlyPrice: Int,
)

data class SubscriptionAddCatalog(
    val services: List<OttService>,
    val bundles: List<BundleSubscriptionOption>,
)

data class BundleSubscriptionOption(
    val code: String,
    val name: String,
    val planName: String,
    val billingCycle: String,
    val monthlyPrice: Int,
    val originalPrice: Int? = null,
    val logoUrl: String? = null,
    val includedServices: List<OttService> = emptyList(),
)

@Singleton
class ServiceRepository @Inject constructor(
    private val serviceApi: ServiceApi,
) {

    @Volatile
    private var cachedServices: List<OttService> = emptyList()

    @Volatile
    private var cachedPlanBundles: Map<Long, ServicePlanBundle> = emptyMap()

    suspend fun getAvailableServices(): Result<List<OttService>> {
        return try {
            val mergedServices = buildList {
                for (category in supportedCategories) {
                    val response = serviceApi.getServices(category = category)
                    if (!response.success || response.data == null) {
                        return Result.Error(response.message ?: DEFAULT_SERVICE_LIST_ERROR_MESSAGE)
                    }
                    addAll(response.data.services.map { item -> item.toDomain() })
                }
            }
            val services = mergedServices.distinctBy { service -> service.serviceId }
            cachedServices = services
            Result.Success(services)
        } catch (e: UnknownHostException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: ConnectException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: SocketTimeoutException) {
            Result.Error(DEFAULT_TIMEOUT_ERROR_MESSAGE)
        } catch (e: HttpException) {
            Result.Error(parseHttpError(e) ?: DEFAULT_SERVICE_LIST_ERROR_MESSAGE, e.code())
        } catch (e: IOException) {
            Result.Error(e.message ?: DEFAULT_SERVICE_LIST_ERROR_MESSAGE)
        } catch (e: Exception) {
            Result.Error(e.message ?: DEFAULT_SERVICE_LIST_ERROR_MESSAGE)
        }
    }

    suspend fun getServicePlans(serviceId: Long): Result<ServicePlanBundle> {
        return try {
            val response = serviceApi.getServicePlans(serviceId = serviceId)
            if (response.success && response.data != null) {
                val bundle = response.data.toDomain()
                cachePlanBundle(bundle)
                Result.Success(bundle)
            } else {
                Result.Error(response.message ?: DEFAULT_SERVICE_PLAN_ERROR_MESSAGE)
            }
        } catch (e: UnknownHostException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: ConnectException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: SocketTimeoutException) {
            Result.Error(DEFAULT_TIMEOUT_ERROR_MESSAGE)
        } catch (e: HttpException) {
            Result.Error(parseHttpError(e) ?: DEFAULT_SERVICE_PLAN_ERROR_MESSAGE, e.code())
        } catch (e: IOException) {
            Result.Error(e.message ?: DEFAULT_SERVICE_PLAN_ERROR_MESSAGE)
        } catch (e: Exception) {
            Result.Error(e.message ?: DEFAULT_SERVICE_PLAN_ERROR_MESSAGE)
        }
    }

    suspend fun getSubscriptionAddCatalog(): Result<SubscriptionAddCatalog> {
        return when (val servicesResult = getAvailableServices()) {
            is Result.Success -> {
                when (val bundlesResult = getAvailableBundles()) {
                    is Result.Success -> {
                        Result.Success(
                            SubscriptionAddCatalog(
                                services = servicesResult.data,
                                bundles = bundlesResult.data,
                            ),
                        )
                    }

                    is Result.Error -> Result.Error(bundlesResult.message, bundlesResult.code)
                    Result.Loading -> Result.Loading
                }
            }

            is Result.Error -> Result.Error(servicesResult.message, servicesResult.code)
            Result.Loading -> Result.Loading
        }
    }

    private suspend fun getAvailableBundles(): Result<List<BundleSubscriptionOption>> {
        return try {
            val response = serviceApi.getBundles()
            if (response.success && response.data != null) {
                Result.Success(
                    response.data.bundles
                        .map { bundle -> bundle.toDomain() }
                        .sortedWith(
                            compareBy<BundleSubscriptionOption>(
                                { bundle -> bundle.name },
                                { bundle -> bundle.monthlyPrice },
                            ),
                        ),
                )
            } else {
                Result.Error(response.message ?: DEFAULT_BUNDLE_LIST_ERROR_MESSAGE)
            }
        } catch (e: UnknownHostException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: ConnectException) {
            Result.Error(DEFAULT_NETWORK_ERROR_MESSAGE)
        } catch (e: SocketTimeoutException) {
            Result.Error(DEFAULT_TIMEOUT_ERROR_MESSAGE)
        } catch (e: HttpException) {
            Result.Error(parseHttpError(e) ?: DEFAULT_BUNDLE_LIST_ERROR_MESSAGE, e.code())
        } catch (e: IOException) {
            Result.Error(e.message ?: DEFAULT_BUNDLE_LIST_ERROR_MESSAGE)
        } catch (e: Exception) {
            Result.Error(e.message ?: DEFAULT_BUNDLE_LIST_ERROR_MESSAGE)
        }
    }

    fun getCachedOttServiceCatalog(): ServiceCatalog = getCachedServiceCatalog()

    fun getCachedServiceCatalog(): ServiceCatalog {
        if (cachedServices.isEmpty()) {
            return TrackedSubscriptionPackages.toServiceCatalog()
        }

        val mappedServices = cachedServices.map { service ->
            service.toCatalogService(cachedPlanBundles[service.serviceId])
        }
        val servicesByCode = mappedServices.associateBy { service -> service.code }
        return ServiceCatalog(
            services = mappedServices,
            bundles = TrackedSubscriptionPackages.bundleRegistry.map { bundle ->
                ServiceCatalog.Bundle(
                    code = bundle.code,
                    name = bundle.bundleName,
                    aliases = bundle.aliases,
                    serviceCodes = bundle.serviceCodes,
                    serviceIds = bundle.serviceCodes.mapNotNull { serviceCode ->
                        servicesByCode[serviceCode]?.serviceId
                    }.toSet(),
                    plans = bundle.plans.map { plan ->
                        ServiceCatalog.BundlePlan(
                            planName = plan.planName,
                            billingCycle = plan.billingCycle,
                            monthlyPrice = plan.monthlyPrice,
                        )
                    },
                )
            },
            loadedAtEpochMs = System.currentTimeMillis(),
        )
    }

    private fun cachePlanBundle(bundle: ServicePlanBundle) {
        cachedPlanBundles = cachedPlanBundles + (bundle.serviceId to bundle)
        if (cachedServices.none { service -> service.serviceId == bundle.serviceId }) {
            cachedServices = cachedServices + bundle.toOttService()
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

    private fun ServiceListItemResponse.toDomain(): OttService {
        return OttService(
            serviceId = serviceId,
            code = code,
            name = name,
            logoUrl = resolveMediaUrl(logoUrl),
        )
    }

    private fun ServiceDetailResponse.toDomain(): ServicePlanBundle {
        return ServicePlanBundle(
            serviceId = serviceId,
            code = code,
            name = name,
            category = category,
            logoUrl = resolveMediaUrl(logoUrl),
            plans = plans.map { plan ->
                ServicePlanOption(
                    servicePlanId = plan.servicePlanId,
                    planName = plan.planName,
                    billingCycle = plan.billingCycle,
                    monthlyPrice = plan.monthlyPrice,
                )
            },
        )
    }

    private fun ServicePlanBundle.toOttService(): OttService {
        return OttService(
            serviceId = serviceId,
            code = code,
            name = name,
            logoUrl = logoUrl,
        )
    }

    private fun OttService.toCatalogService(planBundle: ServicePlanBundle?): ServiceCatalog.Service {
        val trackedService = TrackedSubscriptionPackages.findServiceByCode(code)
            ?: TrackedSubscriptionPackages.findServiceByName(name)

        return ServiceCatalog.Service(
            serviceId = serviceId,
            code = code,
            name = name,
            logoUrl = logoUrl,
            aliases = trackedService?.aliases ?: emptySet(),
            packageNames = trackedService?.packageNames ?: emptySet(),
            plans = planBundle?.plans?.map { plan -> plan.toCatalogPlan() } ?: emptyList(),
        )
    }

    private fun ServicePlanOption.toCatalogPlan(): ServiceCatalog.Plan {
        return ServiceCatalog.Plan(
            servicePlanId = servicePlanId,
            planName = planName,
            billingCycle = billingCycle,
            monthlyPrice = monthlyPrice,
        )
    }

    private fun BundleListItemResponse.toDomain(): BundleSubscriptionOption {
        return BundleSubscriptionOption(
            code = code,
            name = name,
            planName = planName,
            billingCycle = billingCycle,
            monthlyPrice = monthlyPrice,
            originalPrice = originalPrice,
            logoUrl = resolveMediaUrl(logoUrl),
            includedServices = coveredServices.map { coveredService ->
                OttService(
                    serviceId = coveredService.serviceId,
                    code = coveredService.serviceCode.orEmpty(),
                    name = coveredService.serviceName,
                    logoUrl = resolveMediaUrl(coveredService.logoUrl),
                )
            },
        )
    }

    companion object {
        private const val DEFAULT_SERVICE_LIST_ERROR_MESSAGE = "서비스 목록 조회에 실패했어요"
        private const val DEFAULT_SERVICE_PLAN_ERROR_MESSAGE = "요금제 목록 조회에 실패했어요"
        private const val DEFAULT_BUNDLE_LIST_ERROR_MESSAGE = "번들 목록 조회에 실패했어요"
        private const val DEFAULT_NETWORK_ERROR_MESSAGE = "네트워크 연결을 확인해 주세요"
        private const val DEFAULT_TIMEOUT_ERROR_MESSAGE = "서비스 응답이 지연되고 있어요"

        private val supportedCategories = listOf("OTT", "MUSIC", "AI")
        private val json = Json { ignoreUnknownKeys = true }

        val serviceScopeNote: String = PendingManualAddServiceScopeNote
        val serviceListEndpoint: String = "GET /api/v1/services?category={OTT|MUSIC|AI}"
        val bundleListEndpoint: String = "GET /api/v1/bundles"
        val servicePlansEndpoint: String = "GET /api/v1/services/{serviceId}/plans"
    }
}
