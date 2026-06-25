package com.ssafy.e106.feature.analysis.model

object TrackedSubscriptionPackages {
    data class TrackedService(
        val code: String,
        val serviceName: String,
        val aliases: Set<String> = emptySet(),
        val packageNames: Set<String> = emptySet(),
    )

    data class TrackedBundle(
        val code: String,
        val bundleName: String,
        val aliases: Set<String> = emptySet(),
        val serviceCodes: Set<String>,
        val plans: List<TrackedBundlePlan>,
    )

    data class TrackedBundlePlan(
        val planName: String,
        val billingCycle: String,
        val monthlyPrice: Int,
    )

    val registry: List<TrackedService> = listOf(
        TrackedService(
            code = "NETFLIX",
            serviceName = "넷플릭스",
            aliases = setOf("Netflix", "NETFLIX", "넷플릭스"),
            packageNames = setOf("com.netflix.mediaclient"),
        ),
        TrackedService(
            code = "TVING",
            serviceName = "티빙",
            aliases = setOf("TVING", "Tving", "티빙"),
            packageNames = setOf("com.cj.enm.tving"),
        ),
        TrackedService(
            code = "WAVVE",
            serviceName = "웨이브",
            aliases = setOf("Wavve", "WAVVE", "웨이브"),
            packageNames = setOf("com.wavve.android.wavve"),
        ),
        TrackedService(
            code = "WATCHA",
            serviceName = "왓챠",
            aliases = setOf("Watcha", "WATCHA", "왓챠"),
            packageNames = setOf("com.frograms.watcha"),
        ),
        TrackedService(
            code = "COUPANG_PLAY",
            serviceName = "쿠팡플레이",
            aliases = setOf("Coupang Play", "COUPANG_PLAY", "쿠팡플레이"),
            packageNames = setOf("com.coupang.mobile.play"),
        ),
        TrackedService(
            code = "DISNEY_PLUS",
            serviceName = "디즈니플러스",
            aliases = setOf("Disney+", "DISNEY_PLUS", "디즈니플러스", "디즈니+"),
            packageNames = setOf("com.disney.disneyplus"),
        ),
        TrackedService(
            code = "MELON",
            serviceName = "멜론",
            aliases = setOf("Melon", "MELON", "멜론"),
            packageNames = setOf("com.iloen.melon"),
        ),
        TrackedService(
            code = "BUGS",
            serviceName = "벅스",
            aliases = setOf("Bugs", "BUGS", "벅스"),
            packageNames = setOf("com.neowiz.android.bugs"),
        ),
        TrackedService(
            code = "YOUTUBE_MUSIC",
            serviceName = "YouTube Music",
            aliases = setOf("YouTube Music", "YOUTUBE_MUSIC", "유튜브 뮤직", "YouTubeMusic"),
            packageNames = setOf("com.google.android.apps.youtube.music"),
        ),
        TrackedService(
            code = "SPOTIFY",
            serviceName = "Spotify",
            aliases = setOf("Spotify", "SPOTIFY", "스포티파이"),
            packageNames = setOf("com.spotify.music"),
        ),
        TrackedService(
            code = "CHATGPT",
            serviceName = "ChatGPT",
            aliases = setOf("ChatGPT", "CHATGPT", "챗지피티", "챗GPT"),
            packageNames = setOf("com.openai.chatgpt"),
        ),
        TrackedService(
            code = "GEMINI",
            serviceName = "Gemini",
            aliases = setOf("Gemini", "GEMINI", "제미나이", "Google Gemini"),
            packageNames = setOf("com.google.android.apps.bard"),
        ),
        TrackedService(
            code = "CLAUDE",
            serviceName = "Claude",
            aliases = setOf("Claude", "CLAUDE", "클로드"),
            packageNames = setOf("com.anthropic.claude"),
        ),
    )

    val bundleRegistry: List<TrackedBundle> = listOf(
        TrackedBundle(
            code = "TVING_WAVVE",
            bundleName = "티빙 + 웨이브 더블",
            aliases = setOf("더블 이용권", "티빙 웨이브", "웨이브 티빙", "더블 스탠다드", "더블 프리미엄", "더블 베이직", "더블 슬림"),
            serviceCodes = setOf("TVING", "WAVVE"),
            plans = listOf(
                TrackedBundlePlan("더블 광고형 스탠다드", "MONTHLY", 7000),
                TrackedBundlePlan("더블 슬림", "MONTHLY", 9500),
                TrackedBundlePlan("더블 베이직", "MONTHLY", 13500),
                TrackedBundlePlan("더블 스탠다드", "MONTHLY", 15000),
                TrackedBundlePlan("더블 프리미엄", "MONTHLY", 19500),
            ),
        ),
        TrackedBundle(
            code = "DISNEY_PLUS_TVING",
            bundleName = "디즈니+ 티빙 번들",
            aliases = setOf("디즈니 티빙", "티빙 디즈니", "더블 디즈니", "디즈니+ 티빙 번들"),
            serviceCodes = setOf("TVING", "DISNEY_PLUS"),
            plans = listOf(
                TrackedBundlePlan("디즈니+ 티빙 번들", "MONTHLY", 18000),
            ),
        ),
        TrackedBundle(
            code = "DISNEY_PLUS_TVING_WAVVE_3_PACK",
            bundleName = "티빙 + 디즈니플러스 + 웨이브 3 PACK",
            aliases = setOf("3 PACK", "티빙 디즈니 웨이브", "디즈니 티빙 웨이브"),
            serviceCodes = setOf("TVING", "DISNEY_PLUS", "WAVVE"),
            plans = listOf(
                TrackedBundlePlan("3 PACK", "MONTHLY", 21500),
            ),
        ),
    )

    val values: Set<String> = registry.flatMap { service -> service.packageNames }.toSet()

    fun findServiceByCode(code: String): TrackedService? {
        return registry.firstOrNull { service -> service.code.equals(code, ignoreCase = true) }
    }

    fun findServiceByName(name: String): TrackedService? {
        return registry.firstOrNull { service ->
            service.serviceName.equals(name, ignoreCase = true) ||
                service.aliases.any { alias -> alias.equals(name, ignoreCase = true) }
        }
    }

    fun findServiceByPackageName(packageName: String): TrackedService? {
        return registry.firstOrNull { service -> packageName in service.packageNames }
    }

    fun toServiceCatalog(loadedAtEpochMs: Long? = null): ServiceCatalog {
        return ServiceCatalog(
            services = registry.map { service ->
                ServiceCatalog.Service(
                    code = service.code,
                    name = service.serviceName,
                    aliases = service.aliases,
                    packageNames = service.packageNames,
                )
            },
            bundles = bundleRegistry.map { bundle ->
                ServiceCatalog.Bundle(
                    code = bundle.code,
                    name = bundle.bundleName,
                    aliases = bundle.aliases,
                    serviceCodes = bundle.serviceCodes,
                    plans = bundle.plans.map { plan ->
                        ServiceCatalog.BundlePlan(
                            planName = plan.planName,
                            billingCycle = plan.billingCycle,
                            monthlyPrice = plan.monthlyPrice,
                        )
                    },
                )
            },
            loadedAtEpochMs = loadedAtEpochMs,
        )
    }
}
