package com.ssafy.e106.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ssafy.e106.core.ui.theme.LocalSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onNavigateBack: () -> Unit,
) {
    val spacing = LocalSpacing.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "개인정보 처리방침",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "뒤로 가기",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = spacing.screenHorizontalPadding,
                    end = spacing.screenHorizontalPadding,
                    top = spacing.space5,
                    bottom = spacing.space8 + spacing.space8,
                ),
        ) {
            PolicyTitle()
            PolicyPreamble()
            SectionDivider()
            Section1ProcessingPurpose()
            SectionDivider()
            Section2ProcessedItems()
            SectionDivider()
            Section3RetentionPeriod()
            SectionDivider()
            Section4ThirdPartyProvision()
            SectionDivider()
            Section5Entrustment()
            SectionDivider()
            Section6OverseasTransfer()
            SectionDivider()
            Section7Destruction()
            SectionDivider()
            Section8DataSubjectRights()
            SectionDivider()
            Section9OnDeviceAi()
            SectionDivider()
            Section10UsageData()
            SectionDivider()
            Section11PushNotification()
            SectionDivider()
            Section12SecurityMeasures()
            SectionDivider()
            Section13PseudonymizedData()
            SectionDivider()
            Section14PrivacyOfficer()
            SectionDivider()
            Section15Remedies()
            SectionDivider()
            Section16PolicyChanges()
        }
    }
}

/** 섹션 사이 구분선 — 위아래 손가락 하나 정도 여백 */
@Composable
private fun SectionDivider() {
    val spacing = LocalSpacing.current
    Spacer(modifier = Modifier.height(spacing.space7))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
    Spacer(modifier = Modifier.height(spacing.space7))
}

@Composable
private fun SectionHeading(text: String) {
    val spacing = LocalSpacing.current
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = spacing.space3),
    )
}

@Composable
private fun SubHeading(text: String) {
    val spacing = LocalSpacing.current
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = spacing.space4, bottom = spacing.space2),
    )
}

@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun BulletItem(text: String) {
    val spacing = LocalSpacing.current
    Text(
        text = "  \u2022  $text",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = spacing.space2, top = spacing.space1),
    )
}

@Composable
private fun NumberedItem(number: Int, text: String) {
    val spacing = LocalSpacing.current
    Text(
        text = "$number. $text",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = spacing.space2, top = spacing.space1),
    )
}

// ─────────────────────── Title & Preamble ───────────────────────

@Composable
private fun PolicyTitle() {
    val spacing = LocalSpacing.current
    Text(
        text = "애낌(AEKKIM) 개인정보 처리방침",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = spacing.space4),
    )
}

@Composable
private fun PolicyPreamble() {
    Column(verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.space3)) {
        BodyText(
            "애낌(AEKKIM)(이하 \"회사\")은 「개인정보 보호법」 등 관계 법령을 준수하며, " +
                "이용자의 개인정보를 적법하고 안전하게 처리하고 있습니다. " +
                "회사는 이용자의 권익을 보호하고 개인정보와 관련한 고충을 원활하게 처리하기 위하여 " +
                "다음과 같이 개인정보 처리방침을 수립\u00B7공개합니다.",
        )
        Text(
            text = "시행일자: 2026년 3월 16일",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ─────────────────────── Sections ───────────────────────

@Composable
private fun Section1ProcessingPurpose() {
    Column {
        SectionHeading("1. 개인정보의 처리 목적")
        BodyText("회사는 다음의 목적 범위에서 개인정보를 처리합니다.")
        Spacer(modifier = Modifier.height(LocalSpacing.current.space2))
        NumberedItem(1, "회원가입 및 본인 식별, 로그인, 회원관리")
        NumberedItem(2, "구독 서비스 등록, 조회, 수정, 알림 등 기본 서비스 제공")
        NumberedItem(3, "금융 연동 정보를 활용한 개인 맞춤형 구독 분석 및 정리")
        NumberedItem(4, "온디바이스 AI를 활용한 구독 분류, 저이용 판단, 해지 검토 보조")
        NumberedItem(5, "결제예정일, 체크인, 이용 상태 등 서비스 알림 제공")
        NumberedItem(6, "고객 문의 응대, 불만 처리, 분쟁 대응")
        NumberedItem(7, "서비스 안정성 확보, 보안 점검, 장애 및 오류 대응")
        Spacer(modifier = Modifier.height(LocalSpacing.current.space4))
        BodyText(
            "회사는 이용자별 개인화 서비스 제공 및 안정적인 서비스 운영에 필요한 범위를 넘어, " +
                "개인정보를 광고, 외부 마케팅, 데이터 판매, 범용 통계 구축 또는 기타 별도의 사업 목적으로 " +
                "수집\u00B7이용하지 않습니다.",
        )
    }
}

@Composable
private fun Section2ProcessedItems() {
    Column {
        SectionHeading("2. 처리하는 개인정보 항목")
        BodyText("회사는 다음의 개인정보를 처리할 수 있습니다.")

        SubHeading("가. 회원가입 및 로그인")
        BulletItem("소셜 로그인 제공자 식별정보")
        BulletItem("이메일 주소")
        BulletItem("닉네임 또는 프로필명")
        BulletItem("프로필 이미지 URL(제공되는 경우)")
        BulletItem("가입일시")
        BulletItem("최근 로그인 일시")

        SubHeading("나. 이용자가 직접 입력하는 정보")
        BulletItem("구독 서비스명")
        BulletItem("요금제 정보")
        BulletItem("결제일")
        BulletItem("메모")
        BulletItem("체크인 응답 이력")
        BulletItem("알림 설정 정보")

        SubHeading("다. 결제내역 조회 시 앱에 전달되는 정보")
        BulletItem("결제내역 정보(가맹점명, 금액, 결제일, 카테고리)")
        Spacer(modifier = Modifier.height(LocalSpacing.current.space2))
        BodyText(
            "위 정보는 구독 분석 기능 제공을 위하여 회사의 결제내역 조회 API를 통해 앱에 전달되며, " +
                "회사는 서비스 제공에 필요한 범위에서 해당 정보를 결제내역 저장소에 저장·조회할 수 있습니다.",
        )

        SubHeading("라. 온디바이스 AI 분석 결과")
        BulletItem("구독 여부")
        BulletItem("서비스명")
        BulletItem("결제 금액")
        BulletItem("결제일")
        BulletItem("결제주기")
        BulletItem("저이용 판단값")
        BulletItem("개인화 분석 결과")
        Spacer(modifier = Modifier.height(LocalSpacing.current.space2))
        BodyText("회사는 서비스 제공에 필요한 최소한의 분석 결과에 한하여 서버에 저장할 수 있습니다.")

        SubHeading("마. 앱 사용량 기반 기능 이용 시")
        BulletItem("Android UsageStatsManager를 통해 조회되는 OTT 앱 사용시간 정보")
        Spacer(modifier = Modifier.height(LocalSpacing.current.space2))
        BodyText("위 정보는 이용자의 기기 내에서만 처리되며, 회사 서버로 전송되거나 저장되지 않습니다.")

        SubHeading("바. 알림 서비스 이용 시")
        BulletItem("FCM 디바이스 토큰")
        BulletItem("알림 발송 및 수신 이력")

        SubHeading("사. 서비스 이용 과정에서 자동으로 생성되는 정보")
        BulletItem("접속 로그")
        BulletItem("기기 정보")
        BulletItem("앱 버전")
        BulletItem("운영체제 정보")
        BulletItem("오류 및 장애 기록")
        BulletItem("보안 대응에 필요한 최소한의 이용기록")
        Spacer(modifier = Modifier.height(LocalSpacing.current.space4))
        BodyText(
            "회사는 주민등록번호, 여권번호, 운전면허번호 등 법령상 별도 근거가 필요한 " +
                "고유식별정보를 원칙적으로 수집하지 않습니다.",
        )
    }
}

@Composable
private fun Section3RetentionPeriod() {
    Column {
        SectionHeading("3. 개인정보의 처리 및 보유기간")
        BodyText("회사는 법령에 따른 보유기간 또는 이용자로부터 동의받은 기간 내에서 개인정보를 처리\u00B7보유합니다.")

        SubHeading("가. 회원정보")
        BulletItem("회원 탈퇴 시까지")

        SubHeading("나. 결제내역 정보, 수동 등록 구독 정보, 체크인 이력, 개인화 분석 결과")
        BulletItem("회원 탈퇴 시 또는 이용자가 해당 정보를 삭제할 때까지")

        SubHeading("다. 알림 설정 및 알림 이력")
        BulletItem("회원 탈퇴 시까지")
        Spacer(modifier = Modifier.height(LocalSpacing.current.space2))
        BodyText("다만, 분쟁 방지 및 서비스 운영상 필요한 범위에서 일정 기간 별도 보관할 수 있습니다.")

        SubHeading("라. 고객 문의 내역")
        BulletItem("문의 처리 완료 후 3년")

        SubHeading("마. 법령상 보존이 필요한 정보")
        BodyText(
            "회사는 관계 법령에 따라 보존이 필요한 경우 해당 법령에서 정한 기간 동안 " +
                "개인정보를 분리하여 보관합니다.",
        )

        SubHeading("바. 서버에 저장하지 않는 정보")
        BodyText("회사는 다음 정보를 서버에 저장하지 않습니다.")
        BulletItem("앱 사용량 원본 데이터")
    }
}

@Composable
private fun Section4ThirdPartyProvision() {
    Column {
        SectionHeading("4. 개인정보의 제3자 제공")
        BodyText("회사는 이용자의 개인정보를 외부에 제공하지 않습니다. 다만, 다음의 경우에는 예외로 합니다.")
        Spacer(modifier = Modifier.height(LocalSpacing.current.space2))
        NumberedItem(1, "이용자가 사전에 별도로 동의한 경우")
        NumberedItem(2, "법령에 특별한 규정이 있는 경우")
        NumberedItem(3, "이용자 또는 제3자의 급박한 생명, 신체, 재산상 이익을 위하여 명백히 필요한 경우")
        NumberedItem(4, "수사기관 등 관계기관이 법령에 따른 절차와 방법에 따라 요청한 경우")
        Spacer(modifier = Modifier.height(LocalSpacing.current.space4))
        BodyText(
            "회사는 이용자의 개인정보를 광고 타기팅, 외부 제휴 마케팅, 데이터 거래 " +
                "또는 개인화 목적과 무관한 제3자 제공에 이용하지 않습니다.",
        )
    }
}

@Composable
private fun Section5Entrustment() {
    Column {
        SectionHeading("5. 개인정보 처리의 위탁")
        BodyText(
            "회사는 원활한 서비스 제공을 위하여 일부 업무를 외부에 위탁할 수 있습니다. " +
                "위탁이 이루어지는 경우 회사는 관계 법령에 따라 위탁계약을 체결하고, " +
                "수탁자가 개인정보를 안전하게 처리하도록 관리\u00B7감독합니다.",
        )
        Spacer(modifier = Modifier.height(LocalSpacing.current.space3))
        BodyText("예시:")
        BulletItem("클라우드 인프라 운영")
        BulletItem("푸시 알림 발송 시스템 운영")
        BulletItem("고객 문의 처리 시스템 운영")
        Spacer(modifier = Modifier.height(LocalSpacing.current.space4))
        BodyText("실제 수탁자 및 위탁업무의 내용이 확정되는 경우 본 방침을 통하여 공개합니다.")
    }
}

@Composable
private fun Section6OverseasTransfer() {
    Column {
        SectionHeading("6. 개인정보의 국외 이전")
        BodyText(
            "회사는 원칙적으로 이용자의 개인정보를 국외로 이전하지 않도록 설계합니다. " +
                "다만, 클라우드 서비스, 푸시 알림 서비스 또는 외부 API 사용 등으로 개인정보의 국외 이전이 " +
                "발생하는 경우에는 관계 법령에 따라 이전받는 자, 이전 국가, 이전 항목, 이전 목적, " +
                "보유 및 이용기간 등을 공개하거나 필요한 절차를 이행합니다.",
        )
    }
}

@Composable
private fun Section7Destruction() {
    Column {
        SectionHeading("7. 개인정보의 파기 절차 및 방법")
        BodyText("회사는 개인정보의 보유기간이 경과하거나 처리 목적이 달성된 경우 지체 없이 해당 개인정보를 파기합니다.")
        Spacer(modifier = Modifier.height(LocalSpacing.current.space2))
        NumberedItem(1, "전자적 파일 형태의 정보는 복구 또는 재생이 불가능한 방법으로 삭제합니다.")
        NumberedItem(2, "종이 문서 형태의 정보는 분쇄 또는 소각합니다.")
        NumberedItem(3, "법령에 따라 별도로 보관하여야 하는 정보는 다른 개인정보와 분리하여 안전하게 보관합니다.")
    }
}

@Composable
private fun Section8DataSubjectRights() {
    Column {
        SectionHeading("8. 정보주체의 권리 및 행사방법")
        BodyText("이용자는 회사에 대하여 언제든지 다음 각 호의 권리를 행사할 수 있습니다.")
        Spacer(modifier = Modifier.height(LocalSpacing.current.space2))
        NumberedItem(1, "개인정보 열람 요구")
        NumberedItem(2, "개인정보 정정\u00B7삭제 요구")
        NumberedItem(3, "개인정보 처리정지 요구")
        NumberedItem(4, "동의 철회 요구")
        NumberedItem(5, "회원 탈퇴 요구")
        NumberedItem(6, "자동화된 처리 결과에 대한 설명 요구 또는 이의 제기")
        Spacer(modifier = Modifier.height(LocalSpacing.current.space4))
        BodyText(
            "이용자는 앱 내 설정 화면, 고객센터 또는 이메일을 통하여 권리를 행사할 수 있으며, " +
                "회사는 관계 법령에 따라 지체 없이 필요한 조치를 하겠습니다.",
        )
        Spacer(modifier = Modifier.height(LocalSpacing.current.space3))
        ContactInfo()
    }
}

@Composable
private fun ContactInfo() {
    val spacing = LocalSpacing.current
    val annotated = buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append("이메일: ") }
        append("privacy@aekkim.com\n")
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append("고객센터: ") }
        append("051-6666-6666")
    }
    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = spacing.space2),
    )
}

@Composable
private fun Section9OnDeviceAi() {
    Column {
        SectionHeading("9. 온디바이스 AI 기반 개인정보 처리")
        BodyText("회사는 구독 분석 및 개인화 기능 제공을 위하여 온디바이스 AI 방식을 사용합니다.")

        SubHeading("가. 처리 방식")
        BodyText(
            "결제 관련 정보는 회사의 결제내역 조회 API를 통해 앱에 전달되고, " +
                "기기 내 AI 모델이 이를 분석합니다. 앱 사용량 정보는 이용자의 기기 내에서만 처리됩니다.",
        )

        SubHeading("나. 처리 원칙")
        NumberedItem(1, "결제내역 정보는 서비스 제공에 필요한 범위에서 회사 서버의 결제내역 저장소에 저장·조회될 수 있습니다.")
        NumberedItem(2, "앱 사용량 원본 데이터는 회사 서버로 전송하지 않습니다.")
        NumberedItem(3, "서비스 제공에 필요한 결제내역 정보와 분석 결과만 서버에 반영합니다.")
        NumberedItem(4, "분석이 완료된 앱 내 임시 데이터는 기기 내 처리 영역에서 삭제되도록 설계합니다.")
        NumberedItem(5, "분석 결과는 이용자 본인에 대한 구독 정리, 저이용 판단, 해지 검토, 맞춤 알림 제공 목적에 한하여 이용합니다.")
        Spacer(modifier = Modifier.height(LocalSpacing.current.space4))
        BodyText(
            "회사는 온디바이스 AI 처리 결과를 이용하여 이용자 일반에 대한 광고용 프로파일을 생성하거나, " +
                "외부 판매용 데이터셋 또는 범용 빅데이터 자산으로 축적\u00B7활용하지 않습니다.",
        )

        SubHeading("다. 자동화된 처리에 관한 안내")
        BodyText(
            "회사의 분석 결과는 이용자의 의사결정을 보조하기 위한 참고 정보이며, " +
                "구독 유지 또는 해지에 관한 최종 판단은 이용자가 직접 수행합니다.",
        )

        SubHeading("라. 외부 보조 모델 사용 시")
        BodyText(
            "회사는 원칙적으로 온디바이스 방식으로만 분석을 수행합니다. " +
                "다만, 특정 항목의 분류 정확도 보완을 위해 외부 보조 모델이 필요한 경우에는 " +
                "최소한의 정보만 사용하고, 실제 외부 전송이 발생하는 경우 사전에 별도 고지하거나 " +
                "필요한 동의 절차를 이행합니다.",
        )
    }
}

@Composable
private fun Section10UsageData() {
    Column {
        SectionHeading("10. 앱 사용량 데이터 처리 및 권한")
        BodyText(
            "회사는 OTT 서비스 저이용 여부를 보다 정확히 안내하기 위하여 " +
                "Android UsageStatsManager 권한을 요청할 수 있습니다. " +
                "해당 권한은 선택 권한이며, 이용자의 명시적인 허용이 있는 경우에만 사용합니다.",
        )

        SubHeading("가. 처리 목적")
        BulletItem("OTT 앱 사용시간 기반 개인화 분석")
        BulletItem("저이용 판단 보조")
        BulletItem("사용량 시각화")
        BulletItem("체크인 보조 기능 제공")

        SubHeading("나. 처리 방식")
        BulletItem("앱 사용량 데이터는 이용자의 기기 내에서만 처리됩니다.")
        BulletItem("회사 서버로 전송되거나 저장되지 않습니다.")
        BulletItem("광고, 외부 마케팅, 일반 통계 구축 목적으로 이용되지 않습니다.")

        SubHeading("다. 권한 철회 및 미허용 시 영향")
        BodyText(
            "이용자는 언제든지 기기 설정에서 해당 권한을 철회할 수 있습니다. " +
                "권한을 허용하지 않거나 철회하는 경우 앱 사용량 기반 기능은 제한될 수 있으나, " +
                "수동 등록 등 다른 기능은 이용할 수 있습니다.",
        )
    }
}

@Composable
private fun Section11PushNotification() {
    Column {
        SectionHeading("11. 푸시 알림")
        BodyText(
            "회사는 결제예정일, 체크인, 서비스 이용 상태 등과 관련한 알림을 제공할 수 있습니다. " +
                "푸시 알림 권한은 선택 권한이며, 이용자는 앱 내 설정 또는 기기 설정을 통해 " +
                "언제든지 변경할 수 있습니다.",
        )
        Spacer(modifier = Modifier.height(LocalSpacing.current.space4))
        BodyText(
            "회사는 이용자의 개인정보를 외부 광고 네트워크 제공, 광고 프로파일 판매 " +
                "또는 광고 최적화 목적에 사용하지 않습니다. 마케팅 목적의 알림이 필요한 경우에는 " +
                "관계 법령에 따라 별도의 동의를 받습니다.",
        )
    }
}

@Composable
private fun Section12SecurityMeasures() {
    Column {
        SectionHeading("12. 개인정보의 안전성 확보조치")
        BodyText("회사는 개인정보의 안전성 확보를 위하여 다음과 같은 조치를 시행하고 있습니다.")
        Spacer(modifier = Modifier.height(LocalSpacing.current.space2))
        NumberedItem(1, "개인정보 접근 권한의 최소화")
        NumberedItem(2, "내부관리계획 수립 및 시행")
        NumberedItem(3, "접근통제 및 인증수단 운영")
        NumberedItem(4, "전송구간 보호 및 저장정보 보호조치")
        NumberedItem(5, "접속기록의 보관 및 위\u00B7변조 방지")
        NumberedItem(6, "보안프로그램 설치 및 운영")
        NumberedItem(7, "개인정보 취급자 교육")
        NumberedItem(8, "장애 및 침해사고 대응체계 운영")
        Spacer(modifier = Modifier.height(LocalSpacing.current.space4))
        BodyText(
            "회사는 특히 앱 사용량 원본 데이터는 서버에 저장하지 않고, 결제내역 및 분석 정보는 " +
                "인증·접근통제로 보호하는 방식으로 개인정보 처리 위험을 최소화하고 있습니다.",
        )
    }
}

@Composable
private fun Section13PseudonymizedData() {
    Column {
        SectionHeading("13. 가명정보 처리에 관한 사항")
        BodyText(
            "회사는 현재 통계 작성, 과학적 연구, 공익적 기록보존 등을 위하여 " +
                "가명정보를 처리하고 있지 않습니다. 향후 가명정보를 처리하게 되는 경우에는 " +
                "관계 법령에 따라 별도 기준과 안전조치를 마련하고 본 방침을 통하여 안내하겠습니다.",
        )
    }
}

@Composable
private fun Section14PrivacyOfficer() {
    Column {
        SectionHeading("14. 개인정보 보호책임자")
        BodyText(
            "회사는 개인정보 처리에 관한 업무를 총괄하고, 개인정보 관련 문의, " +
                "불만처리 및 피해구제를 위하여 아래와 같이 개인정보 보호책임자를 지정하고 있습니다.",
        )
        Spacer(modifier = Modifier.height(LocalSpacing.current.space4))
        val annotated = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append("서비스 제공자: ") }
            append("마을회관\n")
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append("이메일: ") }
            append("privacy@aekkim.com\n")
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append("연락처: ") }
            append("010-6666-6666")
        }
        Text(
            text = annotated,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = LocalSpacing.current.space2),
        )
    }
}

@Composable
private fun Section15Remedies() {
    Column {
        SectionHeading("15. 권익침해 구제방법")
        BodyText("이용자는 개인정보 침해와 관련한 신고나 상담이 필요한 경우 아래 기관에 문의할 수 있습니다.")
        Spacer(modifier = Modifier.height(LocalSpacing.current.space2))
        BulletItem("개인정보침해신고센터")
        BulletItem("개인정보분쟁조정위원회")
        BulletItem("대검찰청")
        BulletItem("경찰청 사이버범죄 신고시스템")
        Spacer(modifier = Modifier.height(LocalSpacing.current.space4))
        BodyText(
            "위 기관은 회사와 별개의 기관으로서, 회사의 자체적인 개인정보 불만처리 결과에 " +
                "만족하지 못하거나 보다 자세한 도움이 필요한 경우 이용할 수 있습니다.",
        )
    }
}

@Composable
private fun Section16PolicyChanges() {
    Column {
        SectionHeading("16. 개인정보 처리방침의 변경")
        BodyText(
            "회사는 관계 법령, 서비스 내용 또는 개인정보 처리 방식의 변경이 있는 경우 " +
                "본 개인정보 처리방침을 개정할 수 있습니다. 개인정보 처리방침이 변경되는 경우 " +
                "앱 내 공지사항 또는 기타 적절한 방법을 통하여 사전에 안내하겠습니다.",
        )
    }
}
