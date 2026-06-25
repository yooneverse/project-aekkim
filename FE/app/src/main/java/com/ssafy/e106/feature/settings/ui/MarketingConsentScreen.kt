package com.ssafy.e106.feature.settings.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ssafy.e106.core.ui.theme.LocalSpacing

@Composable
fun MarketingConsentScreen(
    onNavigateBack: () -> Unit,
) {
    LegalDocumentScaffold(
        title = "혜택 및 프로모션 정보 수신 동의",
        onNavigateBack = onNavigateBack,
    ) {
        item {
            LegalBodyText("이 동의는 선택 사항입니다. 동의하시면 회사가 제공하는 혜택, 프로모션, 할인 관련 안내를 앱 내에서 받아볼 수 있습니다.")
        }

        item { LegalSectionDivider() }
        item {
            LegalSectionHeading("1. 처리 목적")
            LegalBulletItem("혜택 및 프로모션 관련 안내 제공")
            LegalBulletItem("구독 절약 가능 혜택, 추천 프로모션, 이벤트 정보 제공")
        }

        item { LegalSectionDivider() }
        item {
            LegalSectionHeading("2. 안내 사항")
            LegalBodyText("이 동의는 기본 서비스 이용에 필수적이지 않습니다. 동의하지 않아도 회원가입과 기본적인 구독 조회, 수동 등록, 약관 확인 기능은 이용할 수 있습니다.")
            Spacer(modifier = Modifier.height(LocalSpacing.current.space1))
            LegalBodyText("실제 알림 수신 여부는 앱의 알림 설정과 기기 권한 설정에 따라 달라질 수 있습니다.")
        }

        item { LegalSectionDivider() }
        item {
            LegalSectionHeading("3. 철회 방법")
            LegalBodyText("이용자는 마이페이지에서 언제든지 본 동의를 철회할 수 있습니다. 철회 이후에는 혜택 및 프로모션 관련 선택 안내 제공이 제한될 수 있습니다.")
        }
    }
}
