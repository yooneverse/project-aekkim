package com.ssafy.e106.feature.settings.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ssafy.e106.core.ui.theme.LocalSpacing

@Composable
fun ServiceTermsScreen(
    onNavigateBack: () -> Unit,
) {
    LegalDocumentScaffold(
        title = "서비스 이용약관",
        onNavigateBack = onNavigateBack,
    ) {
        item {
            LegalBodyText("시행일자: 2026년 3월 23일")
            Spacer(modifier = Modifier.height(LocalSpacing.current.space2))
            LegalBodyText("본 약관은 AEKKIM 서비스 이용과 관련하여 회사와 이용자 사이의 권리, 의무 및 책임사항을 정합니다.")
        }

        item { LegalSectionDivider() }
        item {
            LegalSectionHeading("1. 서비스 내용")
            LegalBodyText("회사는 이용자의 OTT 등 구독형 서비스 이용 현황을 정리하고, 결제정보와 앱 이용정보를 바탕으로 구독 여부를 분석하며, 프로모션 추천, 체크인 알림, 해지 검토 보조정보를 제공합니다.")
            Spacer(modifier = Modifier.height(LocalSpacing.current.space1))
            LegalBulletItem("구글 또는 카카오 로그인 기반 계정 생성 및 인증")
            LegalBulletItem("OTT 서비스 목록, 요금제, 구독 내역 조회")
            LegalBulletItem("결제내역과 서비스 매핑 정보를 바탕으로 한 구독 추정")
            LegalBulletItem("Android UsageStats 권한을 활용한 OTT 앱 사용시간 분석")
            LegalBulletItem("수동 구독 등록, 수정, 삭제")
            LegalBulletItem("프로모션 추천 및 알림함 제공")
        }

        item { LegalSectionDivider() }
        item {
            LegalSectionHeading("2. 이용계약")
            LegalBodyText("이용계약은 이용자가 외부 로그인 수단을 통해 가입을 신청하고, 이용약관 및 개인정보 처리 관련 고지와 동의 사항에 동의한 뒤 회사가 이를 승낙함으로써 성립합니다.")
            Spacer(modifier = Modifier.height(LocalSpacing.current.space1))
            LegalBodyText("회사는 타인 정보 도용, 허위 정보 입력, 서비스 운영 방해, 법령 위반 목적 이용 등의 경우 가입을 제한하거나 사후 해지할 수 있습니다.")
        }

        item { LegalSectionDivider() }
        item {
            LegalSectionHeading("3. 결제내역 기반 분석 및 온디바이스 AI")
            LegalBodyText("서비스의 핵심 기능은 결제내역과 앱 사용정보를 바탕으로 구독 여부를 분석하는 것입니다.")
            Spacer(modifier = Modifier.height(LocalSpacing.current.space1))
            LegalBulletItem("결제내역 원문과 앱 사용정보 원문은 원칙적으로 이용자의 기기에서 우선 처리됩니다.")
            LegalBulletItem("회사는 서비스 제공에 필요한 범위에서 결제내역 정보를 서버 저장소에 저장·조회할 수 있으며, Android UsageStats 원문은 서버에 저장하지 않습니다.")
            LegalBulletItem("서버에는 서비스 제공에 필요한 결제내역 정보와 분석 결과 중심 정보만 반영될 수 있습니다.")
            LegalBulletItem("분석 정확도가 낮거나 매핑이 불명확한 경우 이용자에게 수동 확인을 요청할 수 있습니다.")
        }

        item { LegalSectionDivider() }
        item {
            LegalSectionHeading("4. 권한 및 동의")
            LegalBodyText("회사는 서비스 제공을 위해 이용약관 동의, 개인정보 처리방침 동의, 결제내역 기반 구독분석 및 온디바이스 AI 처리 동의, Android UsageStats 접근 권한, 푸시 알림 권한 등을 요청할 수 있습니다.")
            Spacer(modifier = Modifier.height(LocalSpacing.current.space1))
            LegalBodyText("필수 권한 또는 동의가 없는 경우 자동 분석, 체크인 보조, 이용 여부 판단 등 핵심 기능의 전부 또는 일부를 사용할 수 없습니다.")
        }

        item { LegalSectionDivider() }
        item {
            LegalSectionHeading("5. 이용자의 의무")
            LegalBulletItem("타인의 계정 또는 인증정보를 도용해서는 안 됩니다.")
            LegalBulletItem("허위 정보를 입력하거나 분석 결과를 왜곡할 목적으로 반복적으로 오입력해서는 안 됩니다.")
            LegalBulletItem("서비스의 정상 운영을 방해하거나 법령, 공공질서, 선량한 풍속에 반하는 행위를 해서는 안 됩니다.")
        }

        item { LegalSectionDivider() }
        item {
            LegalSectionHeading("6. 책임 제한")
            LegalBodyText("회사는 천재지변, 통신 장애, 외부 인증사업자 또는 외부 플랫폼 장애 등 회사의 합리적 통제 범위를 벗어난 사유로 인한 서비스 중단에 대하여 책임을 지지 않습니다.")
            Spacer(modifier = Modifier.height(LocalSpacing.current.space1))
            LegalBodyText("프로모션, 해지 가이드, 고객센터 정보는 참고 정보이며 실제 조건과 절차는 각 사업자의 정책에 따라 달라질 수 있습니다. AI 분석 결과는 의사결정을 보조하기 위한 참고 정보이고, 최종 판단은 이용자가 직접 하여야 합니다.")
        }

        item { LegalSectionDivider() }
        item {
            LegalSectionHeading("7. 유료서비스")
            LegalBodyText("회사가 별도로 유료서비스임을 고지하지 않는 한, AEKKIM은 이용자를 대신하여 OTT 구독을 직접 결제하거나 해지하는 서비스가 아니라 구독 현황 분석과 관리 보조 기능을 제공하는 서비스입니다.")
        }

        item { LegalSectionDivider() }
        item {
            LegalSectionHeading("8. 회사 정보")
            LegalNumberedItem(1, "상호: 마을회관")
            LegalNumberedItem(2, "대표자: 이동장")
            LegalNumberedItem(3, "대표 이메일: support@example.com")
            LegalNumberedItem(4, "대표 전화번호: 051-6666-0106")
            LegalNumberedItem(5, "고객센터 운영 시간: 평일 09:00 - 18:00")
        }
    }
}
