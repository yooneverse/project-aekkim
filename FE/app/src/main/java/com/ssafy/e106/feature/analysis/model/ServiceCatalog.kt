package com.ssafy.e106.feature.analysis.model

data class ServiceCatalog(
    val services: List<Service> = emptyList(),
    val bundles: List<Bundle> = emptyList(),
    val loadedAtEpochMs: Long? = null,
) {
    data class Service(
        val serviceId: Long? = null,
        val code: String,
        val name: String,
        val logoUrl: String? = null,
        val aliases: Set<String> = emptySet(),
        val packageNames: Set<String> = emptySet(),
        val knownMerchantPatterns: Set<String> = emptySet(),
        val priceTolerance: Int? = null,
        val plans: List<Plan> = emptyList(),
    )

    data class Plan(
        val servicePlanId: Long? = null,
        val planName: String,
        val billingCycle: String,
        val monthlyPrice: Int,
    )

    data class Bundle(
        val bundleId: Long? = null,
        val code: String,
        val name: String,
        val aliases: Set<String> = emptySet(),
        val knownMerchantPatterns: Set<String> = emptySet(),
        val serviceCodes: Set<String> = emptySet(),
        val serviceIds: Set<Long> = emptySet(),
        val plans: List<BundlePlan> = emptyList(),
        val priceTolerance: Int? = null,
    )

    data class BundlePlan(
        val planName: String,
        val billingCycle: String,
        val monthlyPrice: Int,
    )

    fun findServiceByCode(code: String): Service? {
        return services.firstOrNull { service -> service.code.equals(code, ignoreCase = true) }
    }

    fun findServiceByName(name: String): Service? {
        return services.firstOrNull { service ->
            service.name.equals(name, ignoreCase = true) ||
                service.aliases.any { alias -> alias.equals(name, ignoreCase = true) }
        }
    }

    fun findServiceByPackageName(packageName: String): Service? {
        return services.firstOrNull { service -> packageName in service.packageNames }
    }

    fun findBundleByCode(code: String): Bundle? {
        return bundles.firstOrNull { bundle -> bundle.code.equals(code, ignoreCase = true) }
    }
}
