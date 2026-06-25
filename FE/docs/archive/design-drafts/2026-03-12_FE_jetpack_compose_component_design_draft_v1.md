# Jetpack Compose 컴포넌트 설계 초안 v1

## 기준
- 플랫폼: Android
- UI 프레임워크: Jetpack Compose
- 모드 기준: Light Mode
- 폰트: Pretendard
- 목적: 디자인 토큰과 컴포넌트 명세를 실제 Compose 구조로 옮기기 위한 구현 직전 설계 초안

---

# 1. 설계 원칙

## 1.1 목표
이 문서는 디자인 시스템 v2와 모바일 컴포넌트 명세서를 기준으로, Android 앱에서 재사용 가능한 Jetpack Compose 구조를 정의하기 위한 초안이다.

핵심 목표는 다음과 같다.

- 화면 기본 배경을 흰색으로 고정한다.
- 카드, 입력폼, 리스트, 시트는 흰 표면 + 연한 보더 중심으로 구성한다.
- Primary 오렌지는 CTA, 선택 상태, 진행 상태, 강조 수치에만 사용한다.
- 상태 의미는 넓은 배경 틴트보다 아이콘, 텍스트, 칩, progress line으로 전달한다.
- 화면 단에서는 조합만 하고, 스타일 결정은 공통 컴포넌트 내부에서 처리한다.

## 1.2 설계 원칙
- 색상값, 폰트값, 간격값은 Theme/Dimens/Tokens에서만 참조한다.
- 컴포넌트는 `Variant + Size + State` 구조를 우선한다.
- one-off UI보다 재사용 가능한 공통 컴포넌트를 먼저 만든다.
- 기본 배경 계층은 `background`/`surface`를 모두 white base로 두고, 구분은 `outline`, `padding`, `shape`, `typography`로 만든다.
- Secondary 액션은 dark solid fill이 아니라 white + border 조합을 기본으로 한다.
- Dark Mode는 본 문서 범위 밖이다.

---

# 2. 권장 패키지 구조

```text
ui/
  theme/
    Color.kt
    Type.kt
    Shape.kt
    Dimens.kt
    Theme.kt

  component/
    button/
      AppButton.kt
      SocialButton.kt
    card/
      AppCard.kt
      AppProgressCard.kt
      AppSavingsCard.kt
    input/
      AppTextField.kt
      SearchField.kt
    feedback/
      AppBadge.kt
      AppSnackbar.kt
      AppEmptyState.kt
      AppErrorState.kt
      AppLoadingState.kt
    selection/
      AppCheckRow.kt
      AppAgreementRow.kt
    navigation/
      AppTopBar.kt
      AppBottomTabBar.kt
      TabItem.kt
    sheet/
      AppBottomSheet.kt
      AppDialog.kt

  model/
    ButtonVariant.kt
    ButtonSize.kt
    BadgeVariant.kt
    CardVariant.kt
    ProgressStatus.kt
    TabDestination.kt

  screen/
    home/
    detail/
    login/
    settings/
```

---

# 3. Theme 설계

## 3.1 Color.kt
라이트 모드 기준 컬러 토큰은 아래처럼 관리한다.

```kotlin
import androidx.compose.ui.graphics.Color

val BgDefault = Color(0xFFFFFFFF)
val SurfaceDefault = Color(0xFFFFFFFF)
val SurfaceMuted = Color(0xFFF8F8F8)

val Primary = Color(0xFFF5A623)
val PrimaryPressed = Color(0xFFE99A14)

val TextPrimary = Color(0xFF111111)
val TextSecondary = Color(0xFF8E8E93)

val BorderDefault = Color(0xFFEAEAEA)
val Success = Color(0xFF22C55E)
val Disabled = Color(0xFFD9D9D9)

val White = Color(0xFFFFFFFF)
```

### Color 운영 메모
- `BgDefault`, `SurfaceDefault`는 둘 다 white base다.
- `SurfaceMuted`는 disabled 입력, pressed secondary, 가벼운 보조 영역에만 제한적으로 쓴다.
- `Primary`는 fill CTA, focus border, progress, selected state, 강조 금액에만 쓴다.
- `Success`는 완료/정상 상태 전용이다.
- `Disabled`는 비활성 아이콘, 비활성 배경, 미선택 체크 아이콘에 사용한다.

## 3.2 Type.kt
Typography는 Pretendard 기준으로 다음 계층을 권장한다.

```kotlin
val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 18.sp
    )
)
```

## 3.3 Shape.kt

```kotlin
val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp)
)
```

## 3.4 Dimens.kt

```kotlin
object AppDimens {
    val screenHorizontal = 20.dp
    val sectionGap = 24.dp
    val itemGap = 12.dp

    val buttonHeightLg = 52.dp
    val buttonHeightMd = 48.dp
    val buttonHeightSm = 40.dp

    val cardRadius = 20.dp
    val fieldRadius = 16.dp
    val chipRadius = 999.dp
}
```

## 3.5 Theme.kt

```kotlin
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = White,
    background = BgDefault,
    onBackground = TextPrimary,
    surface = SurfaceDefault,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = TextSecondary,
    outline = BorderDefault,
    outlineVariant = BorderDefault
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
```

### Theme 운영 메모
- `background`와 `surface`는 모두 white base로 유지한다.
- `surfaceVariant`는 muted gray(`SurfaceMuted`)로 두되, 전체 섹션 배경이 아니라 pressed/disabled/보조 영역에만 사용한다.
- `outline`과 `outlineVariant`를 통해 border 중심 구조를 만든다.
- 본 시스템은 accent를 넓게 깔지 않는다. `primary`는 CTA, focus, selected, progress에만 등장한다.
- Error 컬러는 추후 확장 가능 항목으로 두되, 현재 설계 초안에서는 별도 토큰 확장 전 placeholder 수준으로 유지한다.

---

# 4. 공통 모델 설계

## 4.1 ButtonVariant

```kotlin
enum class ButtonVariant {
    Primary,
    Secondary,
    Ghost,
    SocialGoogle,
    SocialKakao,
    SocialNaver
}
```

## 4.2 ButtonSize

```kotlin
enum class ButtonSize {
    Large,
    Medium,
    Small
}
```

## 4.3 BadgeVariant

```kotlin
enum class BadgeVariant {
    Neutral,
    Progress,
    Success
}
```

## 4.4 CardVariant

```kotlin
enum class CardVariant {
    Default,
    Elevated,
    Progress,
    Savings
}
```

## 4.5 ProgressStatus

```kotlin
enum class ProgressStatus {
    InProgress,
    Completed,
    Inactive
}
```

---

# 5. 버튼 설계 초안

## 5.1 공통 요구사항
AppButton은 아래 조합을 지원한다.

- Variant: Primary / Secondary / Ghost
- Size: Large / Medium / Small
- State: Enabled / Disabled / Loading
- Icon: leading / trailing optional
- Width: `fillMaxWidth()` 또는 `wrapContentWidth()`

## 5.2 권장 시그니처

```kotlin
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    size: ButtonSize = ButtonSize.Large,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null
)
```

## 5.3 구현 규칙
- Primary: `Primary` fill + `White` text. 유일한 strong fill 버튼.
- Secondary: `White` background + `BorderDefault` border + `TextPrimary` text.
- Ghost: transparent background + `TextPrimary` text.
- Disabled: `Disabled` 또는 `SurfaceMuted` 계열로 톤을 낮춘다.
- Loading: 클릭 비활성화 + `CircularProgressIndicator` 표시.

### 권장 색상 해석

```kotlin
private fun resolveButtonColors(variant: ButtonVariant, enabled: Boolean): ButtonColors {
    return when (variant) {
        ButtonVariant.Primary -> ButtonDefaults.buttonColors(
            containerColor = if (enabled) Primary else Disabled,
            contentColor = White,
            disabledContainerColor = Disabled,
            disabledContentColor = White
        )
        ButtonVariant.Secondary -> ButtonDefaults.buttonColors(
            containerColor = if (enabled) White else SurfaceMuted,
            contentColor = if (enabled) TextPrimary else TextSecondary,
            disabledContainerColor = SurfaceMuted,
            disabledContentColor = TextSecondary
        )
        ButtonVariant.Ghost -> ButtonDefaults.textButtonColors(
            contentColor = if (enabled) TextPrimary else TextSecondary,
            disabledContentColor = TextSecondary
        )
        else -> error("Social buttons use SocialButton")
    }
}
```

### 운영 메모
- Secondary를 dark solid fill로 두지 않는다.
- 일반 섹션 안에서 Primary 버튼을 2개 이상 병렬 배치하지 않는다.
- Ghost는 low emphasis action 전용이다.

---

# 6. 소셜 버튼 설계 초안

## 6.1 목적
로그인 화면 전용 버튼으로, AppButton과 합치지 않고 별도 컴포넌트로 분리한다.

## 6.2 권장 시그니처

```kotlin
@Composable
fun SocialButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    provider: ButtonVariant,
    enabled: Boolean = true
)
```

## 6.3 규칙
- 높이 52dp 고정 권장
- 좌측 아이콘 영역 고정
- 텍스트 중앙 정렬
- Google: white + border
- Kakao: brand yellow fill
- Naver: brand green fill
- 일반 CTA 버튼 규칙과 섞지 않는다.

---

# 7. 카드 설계 초안

## 7.1 AppCard
가장 기본이 되는 공통 카드 래퍼다.

```kotlin
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    variant: CardVariant = CardVariant.Default,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
)
```

## 7.2 카드 규칙
- 기본 배경은 `SurfaceDefault`
- 기본 radius는 20dp
- 기본 border는 1dp `BorderDefault`
- Elevated variant만 약한 shadow를 허용한다.
- 색으로 채운 card variant는 기본 패턴이 아니다.

### 변형 기준
- Default: white + border
- Elevated: white + border + shadow-sm
- Progress: white + border + 상태 칩/진행 바
- Savings: white + border + 강조 수치만 Primary

### 운영 메모
- card 전체 배경을 Primary tint로 채우지 않는다.
- hierarchy는 border, spacing, typography, chip으로 만든다.
- tinted card는 특별한 예외가 아니면 설계 기본형에서 제외한다.

---

# 8. 입력 컴포넌트 설계 초안

## 8.1 AppTextField

```kotlin
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null
)
```

## 8.2 입력폼 규칙
- 높이 52dp 권장
- 배경은 `SurfaceDefault`
- unfocused border는 `BorderDefault`
- focused border는 `Primary`
- disabled 배경은 `SurfaceMuted`
- error는 placeholder 확장 항목으로 둔다.

### 권장 색상 초안

```kotlin
val colors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Primary,
    unfocusedBorderColor = BorderDefault,
    focusedContainerColor = White,
    unfocusedContainerColor = White,
    disabledContainerColor = SurfaceMuted,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    disabledTextColor = TextSecondary,
    focusedPlaceholderColor = TextSecondary,
    unfocusedPlaceholderColor = TextSecondary
)
```

### 운영 메모
- focus를 배경색이 아니라 border 변화로 표현한다.
- Primary 배경 필드나 warm tinted field는 사용하지 않는다.
- Error는 `isError = true` 확장 시점에 border/helper 조합으로 추가한다.

## 8.3 SearchField
AppTextField에서 파생하되, leading search icon과 clear action을 기본 지원하도록 설계한다.

---

# 9. 배지 / 칩 설계 초안

## 9.1 AppBadge

```kotlin
@Composable
fun AppBadge(
    text: String,
    modifier: Modifier = Modifier,
    variant: BadgeVariant = BadgeVariant.Neutral
)
```

## 9.2 규칙
- height 24dp 내외
- horizontal padding 10dp
- radius pill 형태
- Neutral: `SurfaceMuted` + `TextSecondary`
- Progress: `Primary` 12% tint + `Primary`
- Success: `Success` 12% tint + `Success`

---

# 10. 네비게이션 설계 초안

## 10.1 AppTopBar

```kotlin
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
)
```

규칙
- 배경은 `BgDefault` 또는 `SurfaceDefault`
- 하단 divider optional
- 액션 아이콘은 `TextPrimary` 또는 `TextSecondary`

## 10.2 AppBottomTabBar

```kotlin
@Composable
fun AppBottomTabBar(
    current: TabDestination,
    onTabSelected: (TabDestination) -> Unit,
    modifier: Modifier = Modifier
)
```

규칙
- background는 `White`
- top border는 `BorderDefault`
- selected는 `Primary`
- unselected는 `TextSecondary`
- 라벨 + 아이콘 조합 우선

---

# 11. 바텀시트 / 다이얼로그 설계 초안

## 11.1 AppBottomSheet

```kotlin
@Composable
fun AppBottomSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
)
```

규칙
- background는 `White`
- radius top 20dp 권장
- drag handle은 `Disabled`
- CTA가 있으면 하단 고정 영역 고려

## 11.2 AppDialog

```kotlin
@Composable
fun AppDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String? = null,
    onDismiss: (() -> Unit)? = null
)
```

규칙
- white surface + large radius
- 확인/취소 구조는 최대 2개 버튼 유지
- destructive 액션은 추후 error token 확장 시 반영

---

# 12. 상태 UI 설계 초안

## 12.1 EmptyStateView

```kotlin
@Composable
fun EmptyStateView(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
)
```

규칙
- white background 유지
- title은 `TextPrimary`, description은 `TextSecondary`
- 아이콘은 `TextSecondary` 또는 최소한의 `Primary` 포인트 허용
- 배경 전체를 muted/primary tint로 채우지 않는다.

## 12.2 LoadingState
- 전체 화면 로딩
- 섹션 로딩
- 버튼 로딩

세 수준으로 나눠 설계한다.

규칙
- 상태 의미는 spinner, skeleton, progress line으로 전달한다.
- 큰 tinted panel 대신 기존 레이아웃 골격을 유지한다.
- progress indicator는 `Primary` 사용 가능.

## 12.3 ErrorState
- 네트워크 에러
- 권한 거부
- 데이터 없음과는 분리

규칙
- white background + error icon + title + description + retry CTA
- error 색상은 추후 확장 항목으로 두되, 현재 초안은 구조 중심으로 유지한다.
- 상태 의미를 큰 빨간 배경 대신 아이콘/텍스트/버튼으로 전달한다.

## 12.4 ProgressState
- 카드 전체 tint 대신 progress chip, progress line, progress text로 상태를 전달한다.
- InProgress는 `Primary`, Completed는 `Success`, Inactive는 `Disabled`를 사용한다.

---

# 13. 화면 조립 원칙

## 13.1 Screen -> Section -> Component 구조
권장 조립 방식은 다음과 같다.

```text
HomeScreen
 ├─ HomeTopBar
 ├─ SavingsSummarySection
 │   ├─ AppSavingsCard
 │   └─ AppBadge
 ├─ ProgressSection
 │   ├─ AppProgressCard
 │   └─ AppButton
 └─ AppBottomTabBar
```

## 13.2 화면 단 규칙
- Screen은 상태 관리와 섹션 조합에 집중한다.
- 스타일 로직은 Component 내부에서 해결한다.
- 문자열/아이콘/상태만 Screen에서 주입한다.
- 같은 UI가 2번 이상 나오면 공통 컴포넌트 후보로 본다.
- 일반 section background에 Primary fill을 사용하지 않는다.

---

# 14. 프리뷰 작성 원칙

각 컴포넌트는 최소 아래 프리뷰를 가진다.

- Default Preview
- Disabled Preview
- Loading Preview
- Variant Preview

예시

```kotlin
@Preview(showBackground = true)
@Composable
private fun AppButtonPrimaryPreview() {
    AppTheme {
        AppButton(
            text = "시작하기",
            onClick = {},
            variant = ButtonVariant.Primary
        )
    }
}
```

---

# 15. 우선 구현 순서 제안

## 1순위
- Theme.kt
- Color.kt
- Type.kt
- Shape.kt
- Dimens.kt

## 2순위
- AppButton
- SocialButton
- AppCard
- AppTextField

## 3순위
- AppBadge
- AppCheckRow
- AppAgreementRow
- AppProgressCard
- AppSavingsCard
- AppBottomTabBar
- EmptyStateView

## 4순위
- AppBottomSheet
- AppDialog
- AppSnackbar
- AppErrorState
- AppLoadingState

---

# 16. 구현 시 금지사항

- 화면 파일 내부에서 직접 hex color 하드코딩 금지
- 버튼마다 개별 radius/height 임의 변경 금지
- 같은 의미의 텍스트 스타일을 화면마다 새로 정의 금지
- Secondary 버튼을 dark solid fill로 구현 금지
- 카드/입력폼/배지에 warm/cream background 기본 적용 금지
- progress/status를 넓은 배경 tint로 해결하려는 방식 금지
- 강조 수치 카드 전체를 Primary fill로 채우는 방식 금지

---

# 17. 다음 단계 권장 산출물

이 설계 초안 다음 단계로는 아래 문서/코드가 이어지면 좋다.

1. Compose Theme 실제 코드 초안
2. AppButton / AppCard / AppTextField 실제 Kotlin 파일 초안
3. 홈/로그인/설정 화면용 샘플 Screen 조립 코드
4. 디자인 토큰 ↔ Compose 매핑표
5. Preview 체크리스트
