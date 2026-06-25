# 모바일 앱 컴포넌트 명세서 v1 (Light Mode 기준)

> 본 문서는 `디자인 시스템 정의서 v2 (White-Base 개정판)`을 바탕으로, FE 구현에 직접 사용할 수 있도록 **컴포넌트 단위 규격**만 별도로 분리한 명세서이다.  
> 목적은 버튼, 입력폼, 카드, 리스트, 탭바, 모달 등 공통 UI를 **variant / size / state 기준으로 일관되게 구현**하는 데 있다.

## 0. 문서 운영 상태

### 현재 구현 기준

- 현재 구현 기준 문서는 `2026-03-28_FE_디자인시스템_구현기준_및_문서정합성.md`다.
- 실제 `AppButton`은 `Primary/Secondary/Ghost` 3종만 제공하고 소셜 로그인은 별도 컴포넌트로 분리돼 있다.
- 실제 `AppCard`는 기본 border가 있으며 `Default/Elevated` 2종만 운영한다.
- 실제 `AppBottomTabBar`는 selected tint만 바뀌고 active indicator는 없다.

### 권장 기준

- 이 문서 본문은 공용 컴포넌트가 맞춰갈 목표 규격 문서다.
- 이하 본문은 현재 구현 상세가 아니라 권장 컴포넌트 규격으로 읽는다.

### 후속 수정 필요 항목

- 소셜 로그인 버튼의 공용 컴포넌트 체계 편입 여부 정리
- 탭바 active indicator 추가
- 보조 텍스트 색 단계 분리
- 체크인 응답 버튼 등 화면 단위 예외 컴포넌트 정리
- 권한 다이얼로그와 프로모션 hero 예외 스타일 축소

---

## 1. 문서 개요

### 1-1. 적용 범위

- 플랫폼: 모바일 앱
- 기준 모드: **Light Mode only**
- 폰트: **Pretendard**
- 기준 문서: `디자인 시스템 정의서 v2 (White-Base 개정판)`
- 구현 기준: 공통 컴포넌트 우선, 화면별 예외 최소화

### 1-2. 공통 토큰 요약

#### Color

| 토큰 | 값 | 용도 |
|---|---|---|
| Background | `#FFFFFF` | 앱 전체 기본 배경 |
| Surface | `#FFFFFF` | 카드, 입력폼, 시트 배경 |
| Surface Muted | `#F8F8F8` | 비활성 입력, 구분 영역 |
| Primary | `#F5A623` | CTA, 선택 상태, 강조 수치 |
| Primary Pressed | `#E99A14` | Primary 눌림 상태 |
| Text Primary | `#111111` | 본문, 제목 |
| Text Secondary | `#8E8E93` | 보조 설명, placeholder |
| Border | `#EAEAEA` | 구분선, 기본 보더 |
| Disabled | `#D9D9D9` | 비활성 요소, 미선택 체크 아이콘 |
| Success | `#22C55E` | 완료, 성공 상태 |

#### 계층 구분 원칙

- 배경 계층은 색상 채움이 아닌 **보더, 반경, 그림자, 간격, 타이포그래피**로 만든다.
- 색상(Primary, Success)은 CTA, 선택 상태, 진행 표시, 강조 수치에만 쓴다.
- 전체 카드 배경을 Primary 색으로 채우는 것은 금지한다.

#### Radius

- `radius-sm`: `8px`
- `radius-md`: `12px`
- `radius-lg`: `16px`
- `radius-xl`: `20px`
- `radius-pill`: `999px`

#### Spacing

- `space-1`: `4px`
- `space-2`: `8px`
- `space-3`: `12px`
- `space-4`: `16px`
- `space-5`: `20px`
- `space-6`: `24px`

#### Shadow

- `shadow-sm`: `0 2px 8px rgba(17, 17, 17, 0.06)`
- `shadow-md`: `0 6px 20px rgba(17, 17, 17, 0.08)`
- `shadow-lg`: `0 10px 28px rgba(17, 17, 17, 0.12)`

---

## 2. 공통 상태 규칙

### 2-1. 기본 상태

- Default
- Pressed
- Disabled
- Selected
- Loading
- Focused
- Error
- Success

### 2-2. 상태 적용 원칙

- 상태는 색상만으로 표현하지 않는다.
- Pressed는 색 변화 또는 그림자 감소를 함께 사용한다.
- Disabled는 클릭 불가 여부가 시각적으로 바로 드러나야 한다.
- Selected는 색상 외에 weight, icon, check 등 보조 신호를 허용한다.
- Error는 보더, helper text, 아이콘 중 최소 2개 조합을 권장한다.

---

## 3. Button

### 컴포넌트명

`AppButton`

### 용도

화면의 주요 액션(Primary), 보조 액션(Secondary), 텍스트성 액션(Ghost/Text), 소셜 로그인(Social)을 일관되게 제공한다.

### 구조

- 아이콘(선택) + 레이블 텍스트 + 아이콘(선택)
- 아이콘과 텍스트 간격: `8px`
- 내부 정렬: 수평/수직 중앙
- 전체 폭 버튼 기본 권장: `width: 100%`
- 인라인 버튼은 콘텐츠 길이에 맞춰 hug width 허용

### Size

| Size | Height | Horizontal Padding | Radius | Text Style | Icon Size |
|---|---:|---:|---:|---|---:|
| sm | 40px | 14px | 12px | label-md | 16px |
| md | 48px | 16px | 16px | label-md | 18px |
| lg | 52px | 20px | 16px | label-md | 20px |

### 스타일 규칙

#### Primary

- 배경: `#F5A623` (오렌지 채움)
- 텍스트: `#FFFFFF`
- 보더: 없음
- 역할: 화면당 주요 CTA 1개

#### Secondary

- 배경: `#FFFFFF`
- 텍스트: `#111111`
- 보더: `1px solid #EAEAEA`
- 역할: 대체 행동, 보조 행동, 확인 이전 단계

#### Ghost / Text

- 배경: transparent
- 텍스트: `#111111` (강조) 또는 `#8E8E93` (약한 액션)
- 보더: 없음
- 역할: 닫기, 건너뛰기, 더 보기, 텍스트 액션

#### Social (브랜드 전용, 일반 시스템과 분리)

| 플랫폼 | 배경 | 텍스트 | 보더 |
|---|---|---|---|
| Google | `#FFFFFF` | `#111111` | `1px solid #EAEAEA` |
| Kakao | `#FEE500` | `#111111` | 없음 |
| Naver | `#03C75A` | `#FFFFFF` | 없음 |

### 상태

#### Primary

| State | Background | Text | Border | Shadow |
|---|---:|---:|---:|---|
| Default | `#F5A623` | `#FFFFFF` | 없음 | shadow-sm |
| Pressed | `#E99A14` | `#FFFFFF` | 없음 | 약화 |
| Disabled | `#D9D9D9` | `#FFFFFF` | 없음 | 없음 |
| Loading | `#F5A623` | `#FFFFFF` | 없음 | shadow-sm |

#### Secondary

| State | Background | Text | Border | Shadow |
|---|---:|---:|---:|---|
| Default | `#FFFFFF` | `#111111` | `1px solid #EAEAEA` | 없음 |
| Pressed | `#F8F8F8` | `#111111` | `1px solid #D9D9D9` | 없음 |
| Disabled | `#F8F8F8` | `#8E8E93` | `1px solid #EAEAEA` | 없음 |

#### Ghost / Text

| State | Background | Text | Border |
|---|---:|---:|---:|
| Default | transparent | `#111111` | 없음 |
| Pressed | `#F8F8F8` | `#111111` | 없음 |
| Disabled | transparent | `#8E8E93` | 없음 |

### 컬러 사용 규칙

- Primary 버튼만 오렌지 배경을 사용한다.
- Secondary는 흰 배경 + 연한 보더 + 어두운 텍스트 조합을 유지한다.
- Ghost는 배경 없이 텍스트 강조만으로 역할을 전달한다.
- Social 버튼은 각 플랫폼 브랜드 컬러를 그대로 따르며, 일반 버튼 시스템과 혼용하지 않는다.

### 금지 패턴

- 버튼마다 다른 height/radius 사용
- Primary와 Secondary를 동일 우선순위처럼 병렬 배치
- Ghost 버튼에 채우기 색 또는 과도한 보더 추가
- Secondary 버튼을 어두운 단색 배경(다크 필)으로 구현
- 아이콘만 있는 버튼을 44px 미만 터치 영역으로 구현
- Social 버튼에 일반 시스템 컬러 적용

### 사용 예시

```
[Primary]   [오렌지 배경 / 흰 텍스트] "저축 시작하기"
[Secondary] [흰 배경 / 연보더 / 검정 텍스트] "나중에 하기"
[Ghost]     [투명 / 검정 텍스트] "건너뛰기"
[Social]    [흰 배경 / 연보더] Google 로고 + "Google로 계속하기"
[Social]    [카카오 노랑 배경] 카카오 로고 + "카카오로 계속하기"
```

---

## 4. Input Field

### 컴포넌트명

`AppTextField`

### 용도

텍스트 입력, 검색, 선택 진입, 폼 입력을 통일된 구조로 제공한다.

### 구조

- Label (선택) → Field → Helper Text (선택)
- Label ↔ Field 간격: `8px`
- Field ↔ Helper 간격: `6px`
- Leading icon: 좌측 `16px` 기준 정렬
- Trailing icon (clear/visibility): 우측 `16px` 기준 정렬

### 스타일 규칙

| Item | Value |
|---|---:|
| Height | 52px |
| Horizontal Padding | 16px |
| Radius | 16px |
| Background | `#FFFFFF` |
| Border | `1px solid #EAEAEA` |
| Text | `#111111` |
| Placeholder | `#8E8E93` |
| Shadow | 없음 |

### 상태

| State | Background | Border | Text | Helper |
|---|---:|---:|---:|---:|
| Default | `#FFFFFF` | `#EAEAEA` | `#111111` | `#8E8E93` |
| Focused | `#FFFFFF` | `#111111` | `#111111` | `#8E8E93` |
| Disabled | `#F8F8F8` | `#EAEAEA` | `#8E8E93` | `#8E8E93` |
| Error | `#FFFFFF` | 에러 컬러 (별도 확장 예정) | `#111111` | 에러 컬러 (별도 확장 예정) |

### 컬러 사용 규칙

- 기본 배경은 항상 `#FFFFFF`를 유지한다.
- Disabled 상태에서만 `#F8F8F8`로 배경을 낮춘다.
- Focused 상태는 보더 색을 `#111111`로 강화하는 것으로 표현한다.
- Primary 색은 입력 필드 배경이나 보더에 사용하지 않는다.

### 금지 패턴

- Focused 상태에 Primary 오렌지 보더 사용
- 입력 필드 배경을 크림/베이지 계열로 채움
- 에러 상태를 보더 변경만으로 표현 (helper text 없이)

### 사용 예시

```
[Default]  흰 배경 / 연보더 / placeholder "이름을 입력하세요"
[Focused]  흰 배경 / 진한 보더 / 커서 활성
[Disabled] 연회색 배경 / 연보더 / 회색 텍스트
[Error]    흰 배경 / 에러 보더 / helper "올바른 형식으로 입력해주세요"
```

---

## 5. Card

### 컴포넌트명

`AppCard`

### 용도

정보 묶음, 상태 요약, CTA 포함 블록을 안정적으로 구성한다.

### 구조

- 카드 컨테이너 (배경 + 반경 + 그림자 또는 보더)
- 내부: 제목 영역 + 본문 영역 + 메타/액션 영역

### 스타일 규칙

| Item | Value |
|---|---:|
| Background | `#FFFFFF` |
| Radius | 20px |
| Padding | 16px |
| Border | 없음 또는 `1px solid #EAEAEA` |
| Shadow | shadow-md |

#### 내부 간격

| Area | Value |
|---|---:|
| Title ↔ Body | 8px |
| Body ↔ Meta | 12px |
| Section ↔ Section | 16px |
| Card ↔ Card | 16px |

### 상태

- Default: 흰 배경 + shadow-md
- Hover/Pressed: shadow 약화 또는 보더 강화
- Disabled: 배경 `#F8F8F8`, 콘텐츠 opacity 낮춤

### 컬러 사용 규칙

- 카드 배경은 `#FFFFFF`를 기본으로 한다.
- 강조가 약한 정보 카드는 border-only 스타일 허용 (shadow 없음).
- 강조 카드만 shadow를 강하게 사용하고, 같은 화면에서 shadow 단계는 2개 이하로 제한한다.
- Primary 색은 카드 배경에 사용하지 않는다. 카드 내부의 수치, 아이콘, 칩, 진행 바에만 허용한다.

### 금지 패턴

- 카드 전체 배경을 Primary 오렌지 또는 크림 계열로 채움
- 카드마다 radius가 다름
- 동일 화면에서 shadow 강도를 임의로 혼합
- 카드 내부에 버튼 3개 이상 배치로 우선순위 모호화

### 사용 예시

```
[Default Card]  흰 배경 / shadow-md / 제목 + 설명 + 보조 액션
[Info Card]     흰 배경 / border-only / 아이콘 + 텍스트 정보
[CTA Card]      흰 배경 / shadow-md / 제목 + Primary 버튼
```

---

## 6. Check / Agreement Row

### 컴포넌트명

`AppCheckRow` / `AppAgreementRow`

### 용도

약관 동의, 선택 항목, 체크리스트 행을 일관된 구조로 제공한다.

### 구조

- 체크 아이콘 (좌측) + 레이블 텍스트 + 보조 액션 (선택, 우측)
- 행 높이: 최소 52px
- 수평 패딩: 16px
- 체크 아이콘 크기: 24px
- 아이콘 ↔ 레이블 간격: 12px

### 스타일 규칙

#### 체크 아이콘

| State | Icon Color | Icon Border |
|---|---|---|
| Unselected | `#D9D9D9` (회색 원형 또는 사각형) | `1px solid #D9D9D9` |
| Selected | `#F5A623` (오렌지 채움 + 흰 체크) | 없음 |

#### 행 배경

| State | Background |
|---|---|
| Unselected | `#FFFFFF` |
| Selected | `#FFFFFF` |

#### 레이블 텍스트

| State | Color | Weight |
|---|---|---|
| Unselected | `#111111` | 400 |
| Selected | `#111111` | 600 |

### 상태

- Unselected: 회색 아이콘 + 일반 텍스트 + 흰 배경
- Selected: 오렌지 아이콘 + 굵은 텍스트 + 흰 배경 (배경 변경 없음)
- Disabled: 회색 아이콘 + 회색 텍스트 + 연회색 배경 (`#F8F8F8`)

### 컬러 사용 규칙

- 선택 상태는 체크 아이콘 색상(오렌지)과 텍스트 weight 변화로만 표현한다.
- 행 배경은 선택 여부와 무관하게 흰색을 유지한다.
- 리스트 구분은 `#D9D9D9` 구분선 또는 간격으로 처리한다.
- 전체 동의 행은 동일 규칙을 따르되, 레이블 weight를 600으로 고정한다.

### 금지 패턴

- 선택 상태에서 행 배경을 오렌지 또는 크림 계열로 채움
- 미선택 상태에서 배경 색으로 구분 시도
- 체크 아이콘 없이 배경 색만으로 선택 상태 표현
- 텍스트와 배경을 동시에 강조하는 이중 강조

### 사용 예시

```
[Unselected] ○ (회색 원) "서비스 이용약관 동의 (필수)"  [보기 >]
[Selected]   ● (오렌지 체크) "서비스 이용약관 동의 (필수)"  [보기 >]
[전체 동의]  ● (오렌지 체크) "전체 동의" (weight 600)
```

---

## 7. Progress / Status Card

### 컴포넌트명

`AppProgressCard` / `AppStatusCard`

### 용도

저축 목표, 챌린지, 미션 등의 진행 상태와 완료 여부를 시각적으로 요약한다.

### 구조

- 카드 컨테이너 (흰 배경)
- 상단: 상태 칩 + 제목
- 중단: 진행 바 또는 수치 정보
- 하단: 메타 정보 (날짜, 남은 기간 등) + 보조 액션 (선택)

### 스타일 규칙

| Item | Value |
|---|---:|
| Background | `#FFFFFF` |
| Radius | 20px |
| Padding | 16px |
| Border | `1px solid #EAEAEA` |
| Shadow | shadow-sm 또는 shadow-md |

#### 상태 칩

| Status | Background | Text | Icon |
|---|---|---|---|
| 진행 중 | `rgba(245, 166, 35, 0.12)` | `#F5A623` | 오렌지 아이콘 |
| 완료 | `rgba(34, 197, 94, 0.12)` | `#22C55E` | 초록 아이콘 |
| 비활성 | `#F8F8F8` | `#8E8E93` | 회색 아이콘 |

#### 진행 바

| Item | Value |
|---|---|
| Track Background | `#EAEAEA` |
| 진행 중 Fill | `#F5A623` |
| 완료 Fill | `#22C55E` |
| Height | 6px |
| Radius | 999px |

### 상태

- 진행 중: 오렌지 칩 + 오렌지 진행 바 + 흰 카드 배경
- 완료: 초록 칩 + 초록 진행 바 (100%) + 흰 카드 배경
- 비활성: 회색 칩 + 회색 진행 바 + 연회색 배경 (`#F8F8F8`)

### 컬러 사용 규칙

- 카드 배경은 상태와 무관하게 `#FFFFFF`를 기본으로 한다.
- 상태 표현은 칩, 아이콘, 진행 바, 텍스트 색상으로만 한다.
- 비활성 상태에서만 배경을 `#F8F8F8`로 낮출 수 있다.
- 진행 중 상태에서 카드 전체 배경을 오렌지 계열로 채우지 않는다.

### 금지 패턴

- 진행 중 카드 배경을 오렌지 또는 크림 계열로 채움
- 완료 카드 배경을 초록 계열로 채움
- 상태 칩 없이 배경 색만으로 상태 구분
- 진행 바와 카드 배경을 동시에 같은 색으로 강조

### 사용 예시

```
[진행 중]
┌─────────────────────────────┐
│ [오렌지 칩: 진행 중]  목표 이름  │
│ ████████░░░░░░░░  65%        │
│ 남은 기간: 12일              │
└─────────────────────────────┘

[완료]
┌─────────────────────────────┐
│ [초록 칩: 완료]  목표 이름    │
│ ████████████████  100%       │
│ 2026.02.28 달성              │
└─────────────────────────────┘
```

---

## 8. Savings / Summary Card

### 컴포넌트명

`AppSavingsCard` / `AppSummaryCard`

### 용도

저축 금액, 목표 달성 현황, 잔액 요약 등 핵심 수치를 강조하여 보여준다.

### 구조

- 카드 컨테이너 (흰 배경)
- 레이블 텍스트 (보조)
- 강조 수치 (Primary 색)
- 보조 정보 (날짜, 단위, 변화량 등)
- 보조 액션 (선택)

### 스타일 규칙

| Item | Value |
|---|---:|
| Background | `#FFFFFF` |
| Radius | 20px |
| Padding | 20px |
| Border | `1px solid #EAEAEA` |
| Shadow | shadow-md |

#### 수치 타이포그래피

| Element | Size | Weight | Color |
|---|---|---|---|
| 레이블 | 14px | 400 | `#8E8E93` |
| 강조 수치 | 28px 이상 | 700 | `#F5A623` |
| 단위 | 16px | 500 | `#111111` |
| 보조 수치 | 14px | 400 | `#8E8E93` |

### 상태

- Default: 흰 배경 + 오렌지 강조 수치
- 목표 달성: 초록 강조 수치 (`#22C55E`) + 완료 아이콘
- 데이터 없음: 회색 수치 (`#8E8E93`) + 안내 텍스트

### 컬러 사용 규칙

- 카드 배경은 항상 `#FFFFFF`를 유지한다.
- 강조 수치(금액, 달성률)에만 Primary 오렌지를 사용한다.
- 목표 달성 시 수치 색을 `#22C55E`로 전환하되, 배경은 변경하지 않는다.
- 카드 전체를 오렌지 또는 그라디언트로 채우지 않는다.

### 금지 패턴

- 카드 전체 배경을 오렌지 또는 크림 계열로 채움
- 수치와 배경을 동시에 강조하는 이중 강조
- 레이블과 수치를 같은 색/weight로 처리해 위계 소멸
- 그라디언트 배경으로 분위기 연출

### 사용 예시

```
┌─────────────────────────────┐
│ 이번 달 저축 금액             │
│ 125,000 원  (오렌지, 28px)   │
│ 목표 200,000원 대비 62.5%    │
└─────────────────────────────┘

┌─────────────────────────────┐
│ 총 저축 금액                  │
│ 1,250,000 원  (오렌지, 32px) │
│ 2026.01.01 ~ 현재            │
└─────────────────────────────┘
```

---

## 9. Badge / Chip

### 컴포넌트명

`AppBadge` / `AppChip`

### 용도

상태, 속성, 카테고리 요약을 간결하게 표시한다.

### 구조

- 아이콘 (선택) + 텍스트
- 높이: 24px
- 수평 패딩: 10px
- 반경: 999px
- 텍스트: 12px / 600

### 스타일 규칙

| Type | Background | Text |
|---|---|---|
| Neutral | `#F8F8F8` | `#8E8E93` |
| Primary | `rgba(245, 166, 35, 0.12)` | `#F5A623` |
| Success | `rgba(34, 197, 94, 0.12)` | `#22C55E` |

### 상태

- Default: 위 컬러 규칙 적용
- Selected (Chip): 배경 `rgba(245, 166, 35, 0.12)` + 텍스트 `#F5A623` + 보더 `1px solid #F5A623`
- Disabled: 배경 `#F8F8F8` + 텍스트 `#8E8E93`

### 컬러 사용 규칙

- 단순 표시 Badge는 클릭 상태를 갖지 않는다.
- 선택형 Chip의 Selected 상태는 배경 + 보더 조합으로 표현한다.
- 배경 채움은 항상 반투명(rgba)을 사용해 과도한 강조를 피한다.

### 금지 패턴

- 배지 배경을 불투명 Primary 색으로 채움
- 동일 화면에서 3가지 이상 Badge 타입 혼용

### 사용 예시

```
[Neutral]  회색 배경 "일반"
[Primary]  연오렌지 배경 "진행 중"
[Success]  연초록 배경 "완료"
```

---

## 10. List Item

### 컴포넌트명

`AppListItem`

### 용도

거래 내역, 설정 항목, 메뉴 리스트 등 반복 행 구조를 일관되게 제공한다.

### 구조

- Leading: 아이콘 / 썸네일 / 아바타
- Body: 제목 + 보조 설명
- Trailing: 금액 / 상태 / 화살표 / 토글

### 스타일 규칙

| Item | Value |
|---|---:|
| Min Height | 56px |
| Horizontal Padding | 16px |
| Vertical Padding | 14px |
| Background | `#FFFFFF` |
| Divider | `1px solid #EAEAEA` |

#### 타이포그래피

- Title: `body-lg` (16px / 500)
- Description: `body-md` (14px / 400 / `#8E8E93`)
- Meta: `caption` (12px / 400 / `#8E8E93`)

### 상태

- Default: 흰 배경 + 구분선
- Pressed: 배경 `#F8F8F8`
- Selected: 텍스트 weight 강화 + 선택 아이콘 (배경 변경 없음)
- Disabled: 텍스트 `#8E8E93` + 배경 `#F8F8F8`

### 컬러 사용 규칙

- 행 배경은 기본 `#FFFFFF`를 유지한다.
- 구분은 `#EAEAEA` 구분선 또는 간격으로 처리한다.
- 금액 등 강조 수치는 `#F5A623` 사용 가능.
- 선택 상태는 배경 변경 없이 weight와 아이콘으로 표현한다.

### 금지 패턴

- 행 배경을 크림/베이지 계열로 채움
- trailing 정렬이 행마다 다름
- 제목 3줄 이상 허용

### 사용 예시

```
[Default]
│ 🏦 아이콘 │ 저축 입금          │ +50,000원 (오렌지) │ >
│ 🏦 아이콘 │ 출금               │ -20,000원          │ >
```

---

## 11. Tab Bar

### 컴포넌트명

`AppTabBar`

### 용도

앱 하단 주요 네비게이션을 제공한다.

### 구조

- 아이콘 + 레이블 (수직 정렬)
- 아이콘 ↔ 레이블 간격: 4px
- 항목 수: 3~5개 권장

### 스타일 규칙

| Item | Value |
|---|---:|
| Height | 72px |
| Background | `#FFFFFF` |
| Top Border | `1px solid #EAEAEA` |

### 상태

| State | Icon | Label |
|---|---|---|
| Default | `#8E8E93` | `#8E8E93` |
| Selected | `#F5A623` | `#F5A623` |

### 컬러 사용 규칙

- Selected 상태는 아이콘과 레이블 모두 `#F5A623`으로 표현한다.
- 탭바 배경은 항상 `#FFFFFF`를 유지한다.
- 강조색을 쓰더라도 모든 탭에 혼용하지 않는다.

### 금지 패턴

- 탭바 배경을 크림/베이지 계열로 채움
- Selected 탭 배경에 오렌지 채움 사용
- 모든 탭 아이콘에 강조색 적용

### 사용 예시

```
[홈 (선택)] 오렌지 아이콘 + 오렌지 레이블
[저축]      회색 아이콘 + 회색 레이블
[내역]      회색 아이콘 + 회색 레이블
```

---

## 12. Modal / Bottom Sheet

### 컴포넌트명

`AppModal` / `AppBottomSheet`

### 용도

확인, 경고, 선택, 상세 정보 입력을 오버레이 형태로 제공한다.

### 구조

#### Modal

| Item | Value |
|---|---:|
| Background | `#FFFFFF` |
| Radius | 20px |
| Padding | 20px |
| Shadow | shadow-lg |

#### Bottom Sheet

| Item | Value |
|---|---:|
| Background | `#FFFFFF` |
| Top Radius | 20px |
| Padding | 20px |
| Drag Handle | 36x4px / `#D9D9D9` |

### 스타일 규칙

- 제목 ↔ 설명: `8px`
- 설명 ↔ 버튼 그룹: `20px`
- 버튼 그룹은 Primary + Ghost 조합 우선
- Destructive 케이스는 별도 상태 컬러 확정 시 추가

### 상태

- Default: 흰 배경 + shadow-lg
- Destructive: 별도 에러 컬러 적용 (추후 확장)

### 컬러 사용 규칙

- 모달/시트 배경은 항상 `#FFFFFF`를 유지한다.
- 딤 레이어: `rgba(17, 17, 17, 0.5)`
- 내부 구분은 간격과 타이포그래피로 처리한다.

### 금지 패턴

- 모달 배경을 크림/베이지 계열로 채움
- 버튼 그룹에 Primary 2개 이상 배치

### 사용 예시

```
[Modal]
┌─────────────────────────────┐
│ 저축을 시작할까요?            │
│ 매달 50,000원이 자동 저축됩니다 │
│ [취소 (Ghost)]  [시작하기 (Primary)] │
└─────────────────────────────┘
```

---

## 13. Toast / Snackbar

### 컴포넌트명

`AppSnackbar`

### 용도

짧은 피드백 메시지를 일시적으로 표시한다.

### 구조

| Item | Value |
|---|---:|
| Background | `#111111` |
| Text | `#FFFFFF` |
| Radius | 14px |
| Padding | `14px 16px` |

### 상태

- Default: 어두운 배경 + 흰 텍스트
- Success: 배경 `#22C55E` + 흰 텍스트 (선택적 사용)

### 컬러 사용 규칙

- 기본 Toast는 `#111111` 배경을 사용한다.
- 성공 피드백에 한해 `#22C55E` 배경 허용.
- Primary 오렌지 배경 Toast는 사용하지 않는다.

### 금지 패턴

- 오렌지 배경 Toast 사용
- 최대 1개 액션 초과
- 지나치게 긴 문장 삽입

---

## 14. Empty / Error / Loading Block

### 컴포넌트명

`AppEmptyState` / `AppErrorState` / `AppLoadingState`

### 용도

데이터 없음, 오류, 로딩 상태를 일관된 구조로 표시한다.

### 구조

#### Empty State

- 아이콘 또는 일러스트 + 제목 + 설명 + 보조 CTA
- CTA는 Ghost 또는 Primary 중 상황에 따라 선택

#### Error State

- 문제 설명 + 재시도 CTA
- 오류 원인은 짧고 명확하게 표현

#### Loading State

- Skeleton 또는 Progress 우선
- 레이아웃 점프가 생기지 않도록 기존 구조 유지

### 스타일 규칙

- 배경: `#FFFFFF`
- 아이콘/일러스트: 회색 계열 (`#8E8E93`) 또는 Primary 포인트
- 제목: `#111111` / 16px / 600
- 설명: `#8E8E93` / 14px / 400

### 컬러 사용 규칙

- 빈 상태 배경은 `#FFFFFF`를 유지한다.
- 아이콘에 한해 Primary 포인트 색 허용.
- 배경을 크림/베이지 계열로 채우지 않는다.

### 금지 패턴

- 빈 상태 배경을 크림/베이지 계열로 채움
- 에러 상태를 배경 색으로만 표현

---

## 15. Compose 구현 권장 매핑

### 15-1. 파일 구조

- `AppButton.kt`
- `AppTextField.kt`
- `AppCard.kt`
- `AppBadge.kt`
- `AppChip.kt`
- `AppCheckRow.kt`
- `AppAgreementRow.kt`
- `AppProgressCard.kt`
- `AppStatusCard.kt`
- `AppSavingsCard.kt`
- `AppListItem.kt`
- `AppTabBar.kt`
- `AppBottomSheet.kt`
- `AppModal.kt`
- `AppSnackbar.kt`

### 15-2. 권장 파라미터 예시

- Button: `variant`, `size`, `enabled`, `loading`, `leadingIcon`, `trailingIcon`
- TextField: `label`, `placeholder`, `value`, `enabled`, `isError`, `helperText`, `leadingIcon`, `trailingIcon`
- Card: `variant`, `padding`, `onClick`
- CheckRow: `checked`, `label`, `enabled`, `onCheckedChange`
- ProgressCard: `status`, `progress`, `title`, `daysLeft`
- SavingsCard: `label`, `amount`, `targetAmount`, `currency`

---

## 16. QA 체크리스트

### 16-1. 버튼

- height가 size 규칙과 일치하는가
- Primary는 오렌지 배경 + 흰 텍스트인가
- Secondary는 흰 배경 + 연보더 + 어두운 텍스트인가
- Ghost는 배경 없이 텍스트만인가
- Social 버튼이 일반 시스템과 분리되어 있는가
- pressed/disabled 상태가 구현되었는가

### 16-2. 입력폼

- Focused, Disabled, Error 상태가 구분되는가
- helper text 영역이 확보되어 있는가
- placeholder와 입력값이 충분히 구분되는가
- 입력 배경이 흰색인가

### 16-3. 카드/리스트

- 카드 배경이 `#FFFFFF`인가
- radius, padding, divider가 규칙에 맞는가
- 같은 화면에서 카드 스타일이 제각각이지 않은가
- trailing 정렬이 일정한가

### 16-4. 체크/동의 행

- 선택 상태가 아이콘 색(오렌지)과 텍스트 weight로만 표현되는가
- 행 배경이 선택 여부와 무관하게 흰색인가

### 16-5. 진행/상태 카드

- 카드 배경이 상태와 무관하게 흰색인가
- 상태 칩, 진행 바, 아이콘으로 상태를 표현하는가
- 진행 중 = 오렌지, 완료 = 초록, 비활성 = 회색인가

### 16-6. 저축/요약 카드

- 카드 배경이 흰색인가
- 강조 수치에만 오렌지가 사용되는가
- 배경 그라디언트나 오렌지 채움이 없는가

### 16-7. 접근성

- 터치 영역이 44px 이상인가
- 상태가 색상만으로 구분되지 않는가
- 본문 텍스트가 14px 이상인가

---

## 17. 최종 정리

본 명세서는 디자인 시스템 v2 (White-Base 개정판)을 실제 구현 단위로 분리한 **컴포넌트 전용 규격서**이다.

핵심 목적은 다음과 같다.

- 공통 컴포넌트의 시각/상태/크기 규칙 통일
- 화면별 임의 스타일 사용 방지
- FE 개발 속도 향상 및 QA 기준 확보

### 핵심 원칙 요약

1. **배경은 흰색**: 앱 전체, 카드, 입력폼, 시트 모두 `#FFFFFF` 기본
2. **계층은 구조로**: 보더, 반경, 그림자, 간격, 타이포그래피로 위계 생성
3. **색은 의미에만**: Primary 오렌지는 CTA, 선택, 진행, 강조 수치에만 사용
4. **상태는 복합 신호로**: 색상 단독이 아닌 아이콘, weight, 보더 조합으로 표현
5. **소셜은 분리**: 소셜 버튼은 브랜드 컬러를 따르며 일반 시스템과 혼용 금지

따라서 본 문서는 공통 UI 구현, 리뷰, 리팩터링, QA의 기준 문서로 사용한다.
