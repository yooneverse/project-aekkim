# 안드로이드 UsageStats A-Z 구현 가이드 (초보자용)

> 작성일: 2026-03-06  
> 대상: Android 개발 입문자  
> 목표: 특정 앱의 최근 7일/30일 총 사용시간 + 마지막 사용일을 수집하고 서비스/프론트에 표시

## 0. 이 문서에서 해결하는 것

이 문서는 아래를 처음부터 끝까지 다룹니다.

1. UsageStats 권한을 왜/어떻게 요청하는지
2. 설정 화면으로 보낸 뒤 앱으로 돌아와 권한 상태를 재확인하는 방법
3. 권한 거절/철회 시 UX와 예외 처리
4. 특정 앱(packageName)의 최근 7일/30일 사용시간과 마지막 사용일 계산
5. API로 서버에 전달하거나, 프론트에서 바로 표시하는 데이터 모델
6. 테스트(허용/거절/철회/OEM 이슈/시간대 변경) 시나리오

---

## 1. 먼저 이해해야 할 핵심 개념

### 1-1. UsageStats는 일반 런타임 권한이 아니다

- 권한명: `android.permission.PACKAGE_USAGE_STATS`
- 이 권한은 알림 권한처럼 앱 내부 팝업으로 허용받는 타입이 아니다.
- 사용자를 기기 설정의 "사용 기록 접근" 화면으로 보내서 직접 켜게 해야 한다.

### 1-2. 권한 확인 방식이 다르다

- `checkSelfPermission()`만으로는 오판할 수 있다.
- `AppOpsManager.OPSTR_GET_USAGE_STATS`의 mode를 확인해야 한다.
- API 29+에서는 `unsafeCheckOpNoThrow()` 경로를 함께 고려한다.

### 1-3. 데이터가 "없다"의 의미가 3가지다

UsageStats 조회 결과가 빈 값일 때 아래를 구분해야 한다.

1. 권한이 없음
2. 권한은 있는데 아직 수집 히스토리가 거의 없음(신규 기기 등)
3. OEM 정책/환경 제약으로 수집이 제한됨

프론트는 이 3개를 같은 에러로 처리하면 안 된다.

---

## 2. 요구사항 정리 (이번 기능 기준)

사용자가 선택한 특정 OTT 앱(예: Netflix) 기준으로 아래를 보여준다.

- 최근 7일 총 사용시간(ms)
- 최근 30일 총 사용시간(ms)
- 마지막 사용일(lastTimeUsed)

입력값:

- 대상 앱 패키지명 목록(예: `com.netflix.mediaclient`)
- 조회 시점의 디바이스 시간/시간대

출력값(예시):

```json
{
  "packageName": "com.netflix.mediaclient",
  "usage7dMs": 5400000,
  "usage30dMs": 18200000,
  "lastUsedEpochMs": 1767643200000,
  "permissionGranted": true,
  "reason": null,
  "queriedAtEpochMs": 1767686400000,
  "timezone": "Asia/Seoul"
}
```

---

## 3. 전체 구현 계획 (처음부터 실행 순서)

1. Manifest/SDK 조건 점검
2. 권한 상태 매니저 구현(AppOps 기반)
3. 온보딩/설정 화면에 "권한 요청 안내 + 설정 이동" UX 추가
4. `onResume()`에서 복귀 후 재검사
5. UsageStats 수집기 구현(7일/30일 + lastUsed)
6. 앱 패키지 필터링(서비스 대상 앱만)
7. UI 상태 모델(허용됨/거절/데이터없음/오류) 정의
8. API 계약(서버 연동 시) 확정
9. 테스트 매트릭스 실행
10. 운영 체크(개인정보/Play 심사 대응)

---

## 4. 사전 준비

### 4-1. Manifest

`AndroidManifest.xml`에 아래 권한 선언:

```xml
<uses-permission
    android:name="android.permission.PACKAGE_USAGE_STATS"
    tools:ignore="ProtectedPermissions" />
```

### 4-2. 최소 SDK

- UsageStatsManager는 API 21부터 가능
- 현재 프로젝트는 `minSdk = 26`이므로 사용 가능

### 4-3. 패키지명 매핑 준비

서비스에서 추적할 앱의 packageName을 서버/앱 내 상수로 관리한다.

예:

```kotlin
object TrackedOttPackages {
    val values = setOf(
        "com.netflix.mediaclient",
        "com.google.android.youtube",
        "com.coupang.mobile.play"
    )
}
```

---

## 5. 권한 요청 UX 구현 (가장 중요)

## 5-1. 사용자에게 먼저 설명한다 (바로 설정으로 보내지 말기)

권장 문구 예시:

- 제목: "앱 사용 기록 접근 권한이 필요해요"
- 본문: "선택한 OTT 앱의 최근 사용시간(7일/30일)을 확인해 더 정확한 안내를 제공하기 위해 필요합니다. 원치 않으면 건너뛸 수 있어요."
- 버튼1: "설정으로 이동"
- 버튼2: "지금은 안 할게요"

핵심:

- 강제처럼 보이지 않게 선택권 제공
- 거절해도 앱 기본 기능은 동작하게 설계

### 5-2. 설정 화면으로 이동

```kotlin
fun openUsageAccessSettings(context: Context) {
    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
```

### 5-3. 앱 복귀 시점 처리

사용자가 설정에서 토글을 바꾸고 돌아왔을 때 자동 콜백이 보장되지 않으므로,
`onResume()`에서 항상 재확인한다.

```kotlin
override fun onResume() {
    super.onResume()
    viewModel.refreshUsagePermissionState()
}
```

추가 주의:

- `ActivityResult`의 `resultCode`는 신뢰하지 말고, 복귀 시 `isGranted()`를 다시 호출한다.
- 제조사 ROM에 따라 설정 화면 경로/동작이 달라질 수 있으므로 `onResume()` 재검사는 필수다.

### 5-4. 권한 상태 머신(권장)

```text
UNKNOWN -> (앱 시작) -> GRANTED | DENIED
DENIED -> (설정 이동 후 허용) -> GRANTED
GRANTED -> (사용자 철회) -> REVOKED -> DENIED 처리
GRANTED -> (권한은 있으나 데이터 없음) -> EMPTY_DATA
```

권장 UI 매핑:

- `GRANTED`: 사용 통계 카드/차트 표시
- `DENIED`: 권한 안내 + 설정 이동 CTA
- `REVOKED`: "권한이 해제되었어요" 안내 + 재요청 CTA
- `EMPTY_DATA`: "아직 수집된 사용 기록이 없어요" 안내

---

## 6. 권한 확인 로직 구현

```kotlin
class UsagePermissionChecker(
    private val context: Context
) {
    fun isGranted(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val uid = Process.myUid()
        val pkg = context.packageName

        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, pkg)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, pkg)
        }

        if (mode == AppOpsManager.MODE_ALLOWED) return true

        // 일부 기기에서는 MODE_DEFAULT가 떨어질 수 있어 2차 확인
        if (mode == AppOpsManager.MODE_DEFAULT) {
            return context.checkCallingOrSelfPermission(
                android.Manifest.permission.PACKAGE_USAGE_STATS
            ) == PackageManager.PERMISSION_GRANTED
        }

        return false
    }
}
```

권한 상태 enum 권장:

```kotlin
enum class UsagePermissionState {
    GRANTED,
    DENIED,
    UNKNOWN
}
```

---

## 7. 7일/30일/마지막 사용일 수집 로직

### 7-1. 시간 범위 계산 (시간대 주의)

하루 경계는 단순 `now - 24h`로 계산하지 말고, 디바이스 시간대를 기준으로 자정 앵커를 사용한다.

```kotlin
fun startOfDayMillis(zoneId: ZoneId = ZoneId.systemDefault()): Long {
    return LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant().toEpochMilli()
}

fun daysAgoMillis(days: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long {
    return LocalDate.now(zoneId)
        .minusDays(days)
        .atStartOfDay(zoneId)
        .toInstant()
        .toEpochMilli()
}
```

### 7-2. UsageStatsManager 조회

```kotlin
class UsageStatsCollector(
    private val context: Context,
    private val permissionChecker: UsagePermissionChecker
) {
    private val usageStatsManager: UsageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    fun collect(packageName: String): AppUsageSnapshot {
        val now = System.currentTimeMillis()
        val zoneId = ZoneId.systemDefault()

        if (!permissionChecker.isGranted()) {
            return AppUsageSnapshot(
                packageName = packageName,
                usage7dMs = 0L,
                usage30dMs = 0L,
                lastUsedEpochMs = null,
                permissionGranted = false,
                reason = "permission_denied",
                queriedAtEpochMs = now,
                timezone = zoneId.id
            )
        }

        val usage7d = queryForegroundTime(packageName, daysAgoMillis(7, zoneId), now)
        val usage30d = queryForegroundTime(packageName, daysAgoMillis(30, zoneId), now)
        val lastUsed = queryLastUsed(packageName, daysAgoMillis(30, zoneId), now)

        return AppUsageSnapshot(
            packageName = packageName,
            usage7dMs = usage7d,
            usage30dMs = usage30d,
            lastUsedEpochMs = lastUsed,
            permissionGranted = true,
            reason = null,
            queriedAtEpochMs = now,
            timezone = zoneId.id
        )
    }

    private fun queryForegroundTime(packageName: String, start: Long, end: Long): Long {
        val map = usageStatsManager.queryAndAggregateUsageStats(start, end)
        return map[packageName]?.totalTimeInForeground ?: 0L
    }

    private fun queryLastUsed(packageName: String, start: Long, end: Long): Long? {
        val map = usageStatsManager.queryAndAggregateUsageStats(start, end)
        val last = map[packageName]?.lastTimeUsed ?: 0L
        return if (last > 0L) last else null
    }
}
```

데이터 모델:

```kotlin
data class AppUsageSnapshot(
    val packageName: String,
    val usage7dMs: Long,
    val usage30dMs: Long,
    val lastUsedEpochMs: Long?,
    val permissionGranted: Boolean,
    val reason: String?,
    val queriedAtEpochMs: Long,
    val timezone: String
)
```

### 7-3. 대상 앱 여러 개를 한 번에 수집

```kotlin
fun collectAll(packages: Collection<String>): List<AppUsageSnapshot> {
    return packages.map { collect(it) }
}
```

---

## 8. 권한 거절/철회/데이터 없음 처리

### 8-1. 권한 거절(처음)

- 화면에 빈 차트 대신 설명형 Empty UI 표시
- CTA: "설정에서 권한 허용"
- 보조 CTA: "건너뛰기"

### 8-2. 권한 허용 후 철회

- 앱 실행 중이거나 다음 실행에서 `onResume()` 재검사
- 기존 캐시값은 "참고용" 배지 처리 또는 무효화

### 8-3. 권한은 있는데 데이터가 0

`reason = "no_history"` 또는 `"oem_restricted"`로 내려서 프론트가 구분 렌더링하도록 한다.

---

## 9. 프론트 표시용 ViewModel/UI 상태 설계

권장 상태:

```kotlin
data class UsageUiState(
    val isLoading: Boolean = false,
    val permissionGranted: Boolean = false,
    val usageItems: List<AppUsageSnapshot> = emptyList(),
    val reason: String? = null,
    val errorMessage: String? = null,
    val updatedAtEpochMs: Long? = null
)
```

렌더링 규칙:

1. `isLoading == true`: 로딩 UI
2. `permissionGranted == false`: 권한 안내 UI
3. `usageItems.isEmpty() && reason == "no_history"`: 데이터 없음 UI
4. 그 외: 앱별 7일/30일/마지막 사용일 카드 표시

시간 포맷 유틸:

```kotlin
fun formatDuration(ms: Long): String {
    val totalMinutes = ms / 1000 / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "${hours}시간 ${minutes}분"
}
```

---

## 10. API 연동 설계 (서비스 활용)

두 가지 방식이 있다.

1. 앱에서 직접 수집 후 UI에 즉시 표시 + 서버 업로드(추천)
2. 앱에서 수집 후 서버 계산/저장 후 서버 응답으로 UI 구성

권장 요청/응답 예시:

```http
POST /api/v1/usage-stats/report
```

```json
{
  "queriedAtEpochMs": 1767686400000,
  "timezone": "Asia/Seoul",
  "items": [
    {
      "packageName": "com.netflix.mediaclient",
      "usage7dMs": 5400000,
      "usage30dMs": 18200000,
      "lastUsedEpochMs": 1767643200000,
      "permissionGranted": true,
      "reason": null
    }
  ]
}
```

응답 예시:

```json
{
  "success": true,
  "data": {
    "saved": true,
    "processedAtEpochMs": 1767686401234
  },
  "message": null
}
```

주의:

- 권한 없음은 HTTP 에러(403)로 보지 말고 정상 응답 + 상태 필드로 내려 프론트가 안내 가능하게 한다.

---

## 11. 테스트 A-Z (실전)

## 11-1. 수동 테스트 시나리오

1. 앱 설치 후 권한 미허용 상태 진입
2. 권한 안내 UI 노출 확인
3. 설정 이동 -> 허용 -> 앱 복귀 -> 데이터 표시 확인
4. 다시 설정에서 권한 차단 -> 앱 복귀 -> 권한 필요 UI 복귀 확인
5. 앱 미설치 패키지 조회 시 0 처리 확인
6. 시간대 변경 후(예: Asia/Seoul -> America/New_York) 집계 경계 이상 여부 확인

### 11-2. ADB 기반 상태 전환

```bash
adb shell appops set <패키지명> android:get_usage_stats allow
adb shell appops set <패키지명> android:get_usage_stats deny
adb shell appops set <패키지명> android:get_usage_stats default
adb shell appops get <패키지명> android:get_usage_stats
```

### 11-3. 테스트 케이스 표

| 케이스 | 입력 | 기대값 |
| --- | --- | --- |
| 권한 허용 + 사용이력 있음 | package=넷플릭스 | usage7d/30d > 0, lastUsed 있음 |
| 권한 거절 | 권한 false | permissionGranted=false, reason=permission_denied |
| 권한 허용 + 신규기기 | 히스토리 없음 | usage=0, reason=no_history |
| 앱 미설치 | 없는 package | usage=0, lastUsed=null |
| 시간대 변경 | timezone 변경 | 날짜 경계 깨지지 않음 |

---

## 12. 운영/보안/심사 체크리스트

- [ ] 왜 이 권한이 필요한지 인앱 설명 제공
- [ ] 거절해도 기본 기능 사용 가능
- [ ] packageName 원본을 서버 로그에 과도하게 남기지 않음
- [ ] 데이터 보존기간 최소화
- [ ] Play 심사 소명 문서에 UsageStats 사용 목적 명시

---

## 13. 초보자용 트러블슈팅

### Q1. 권한 허용했는데 값이 0이에요.

가능성:

- 최근 기간에 해당 앱 사용 이력이 거의 없음
- 제조사 정책으로 제한
- 패키지명이 잘못됨

확인 순서:

1. packageName 재확인
2. 권한 상태 재검사(AppOps)
3. 기간을 30일로 넓혀 재조회

### Q2. 설정 갔다가 돌아오면 화면이 안 바뀌어요.

- `onResume()`에서 권한 재검사/상태 갱신 로직이 없는 경우가 많다.

### Q3. lastTimeUsed가 부정확해 보입니다.

- OS/OEM/최근 앱 전환 패턴에 따라 오차가 있을 수 있다.
- 서비스 정책상 "대략적 마지막 사용시각"으로 안내 문구를 두는 것이 안전하다.

---

## 14. 최종 DoD (Definition of Done)

- [ ] 권한 요청 안내 UI + 설정 이동 동작 구현
- [ ] 복귀 시 재검사(onResume) 구현
- [ ] 특정 앱 7일/30일/마지막 사용일 계산 구현
- [ ] 권한 거절/철회/no_history/oem_restricted UI 분기 완료
- [ ] API 요청/응답 스키마 반영
- [ ] 테스트 매트릭스 전 케이스 통과
