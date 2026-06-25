# 모바일 앱 디자인 시스템 정의서 v2 (Light Mode 기준)

> **문서 목적**: 모바일 앱 FE 구현 직전 단계에서 사용하는 디자인 시스템/스타일 정의 문서다.
> 화면별 스타일 편차를 줄이고, 공통 컴포넌트를 일관된 규칙으로 구현할 수 있도록
> **토큰, 상태, 컴포넌트 규격**을 명확히 정의한다.
> 감상용 문서가 아닌 **개발용 규격서**다.

## 0. 문서 운영 상태

### 현재 구현 기준

- 현재 구현 기준 문서는 `2026-03-28_FE_디자인시스템_구현기준_및_문서정합성.md`다.
- 현재 코드에서는 `Pretendard`가 아직 적용되지 않았고 `Type.kt`는 `FontFamily.Default`를 사용한다.
- 현재 코드에서는 `TextSecondary(#8E8E93)`가 `onSurfaceVariant`로 넓게 쓰이고, 하단 탭바 active indicator도 없다.
- 프로모션/추천/권한 다이얼로그에는 이 문서보다 강한 gradient/tint 예외 스타일이 남아 있다.

### 권장 기준

- 이 문서 본문은 FE가 맞춰갈 목표 디자인 토큰/스타일 권장 기준이다.
- 이하 본문은 현재 구현 설명이 아니라 권장 규격으로 읽는다.

### 후속 수정 필요 항목

- Pretendard 적용
- `TextSecondary` 재정의와 보조 텍스트 대비 상향
- 탭바 active indicator 추가와 라벨 통일
- 프로모션/추천/상세 gradient 예외 톤다운
- 권한 다이얼로그 gradient/tint 및 dismiss 정책 정리

---

## 1. 문서 목적

- 플랫폼: 모바일 앱
- 기준 모드: **Light Mode only**
- 폰트: **Pretendard**
- 구현 목적: 바이브 코딩 / 공통 컴포넌트 설계 / 화면 스타일 일관화
- 적용 범위: 토큰 정의 → 상태 규칙 → 컴포넌트 규격 → 화면 운영 규칙

---

## 2. 디자인 원칙

### 2-1. White Base

- 앱의 기본 배경은 **순수 흰색(`#FFFFFF`)** 이다.
- 크림/아이보리/웜 배경(`#FFF8EE`, `#FAF6F0`, `#EDE9E4` 등)은 기본 배경으로 사용하지 않는다.
- 배경 계층 구분이 필요할 때는 `#F8F8F8`(surface-muted)을 사용한다. 색조가 있는 배경 틴트는 기본 패턴이 아니다.

### 2-2. 최소 컬러 사용

- 컬러는 의미가 있을 때만 쓴다. 장식 목적의 컬러 사용은 금지한다.
- Primary(`#F5A623`)는 CTA 버튼, 진행 상태, 선택 강조, 강조 숫자에만 사용한다.
- Success(`#22C55E`)는 성공/완료 상태 표시에만 사용한다.
- 한 화면에서 Primary 계열 색상이 3곳 이상 동시에 보이면 과용이다.

### 2-3. Border / Spacing / Typography 우선

- 영역 구분은 배경 틴트보다 **border, spacing, radius, 타이포그래피 계층**으로 먼저 해결한다.
- 카드와 섹션은 흰 배경 위에서 border와 shadow로 분리한다.
- 색으로 구분하기 전에 여백과 선으로 구분할 수 있는지 먼저 검토한다.

### 2-4. Point Color 제한

- Primary 오렌지는 포인트 컬러다. 배경 채우기, 섹션 틴트, 일반 텍스트 강조에 쓰지 않는다.
- 허용 용도: Primary 버튼 fill, 진행 중 상태 표시, 선택된 탭/칩, 강조 숫자/금액.
- 금지 용도: 섹션 배경 틴트, 카드 배경 fill, 일반 아이콘 기본 색상, 보조 텍스트 강조.

### 2-5. 컴포넌트 운영 원칙

- 버튼은 variant, size, state를 모두 명시한다.
- 카드/입력폼/바텀시트/탭바는 동일한 radius 체계를 따른다.
- 임의 수치 사용을 지양하고 spacing token을 우선 적용한다.
- 화면 단위 예외 스타일은 최소화한다.

---

## 3. Foundation Tokens

### 3-1. 필수 팔레트

#### Core Colors

| 역할 | 값 | 설명 |
|---|---:|---|
| Background | `#FFFFFF` | 앱 기본 배경 |
| Surface | `#FFFFFF` | 카드/시트/입력 영역 표면 |
| Surface Muted | `#F8F8F8` | 약한 계층 구분, 비활성 배경 |
| Primary | `#F5A623` | CTA, 진행 상태, 선택 강조 |
| Primary Pressed | `#E99A14` | Primary 터치 상태 |
| Success | `#22C55E` | 성공/완료 상태 |
| Disabled | `#D9D9D9` | 비활성 요소 |

#### Neutral Colors

| 역할 | 값 | 설명 |
|---|---:|---|
| Text Primary | `#111111` | 본문, 타이틀 |
| Text Secondary | `#8E8E93` | 보조 설명, 메타 정보 |
| Border | `#EAEAEA` | 입력 경계, divider, 카드 구분선 |

#### 컬러 사용 제한

- `#F5A623`만 Primary로 본다.
- `#E99A14`는 pressed 상태 전용이다.
- 크림/웜 계열(`#FFF8EE`, `#FAF6F0`, `#EDE9E4`, `#F7B84B`) 은 이 시스템의 기본 팔레트가 아니다.
- 딥 네이비(`#1F2A37`)는 이 시스템의 Secondary 버튼 색상이 아니다.
- 새로운 노랑/오렌지 계열 색상 추가는 지양한다.

### 3-2. Foundation Token CSS

```css
:root {
  /* Background */
  --color-bg: #FFFFFF;
  --color-surface: #FFFFFF;
  --color-surface-muted: #F8F8F8;

  /* Primary */
  --color-primary: #F5A623;
  --color-primary-pressed: #E99A14;

  /* Status */
  --color-success: #22C55E;
  --color-disabled: #D9D9D9;

  /* Text */
  --color-text-primary: #111111;
  --color-text-secondary: #8E8E93;

  /* Border */
  --color-border: #EAEAEA;
}
```

---

## 4. Semantic Tokens

Semantic token은 Foundation token을 역할 기반으로 참조한다. 컴포넌트 구현 시 Foundation 값을 직접 쓰지 않고 Semantic token을 사용한다.

```css
:root {
  /* Background / Surface */
  --bg-default: var(--color-bg);
  --bg-surface: var(--color-surface);
  --bg-muted: var(--color-surface-muted);

  /* Text */
  --text-default: var(--color-text-primary);
  --text-subtle: var(--color-text-secondary);
  --text-on-primary: #FFFFFF;

  /* Border */
  --border-default: var(--color-border);
  --border-strong: var(--color-text-primary);

  /* Action */
  --action-primary: var(--color-primary);
  --action-primary-pressed: var(--color-primary-pressed);

  /* Status */
  --status-success: var(--color-success);
  --status-progress: var(--color-primary);
  --status-disabled: var(--color-disabled);
  /* --status-error: 추후 확장 */
}
```

### Semantic Token 역할 요약

| 토큰 | 역할 |
|---|---|
| `--bg-default` | 화면 기본 배경 |
| `--bg-surface` | 카드, 시트, 입력 표면 |
| `--bg-muted` | 비활성 배경, 약한 계층 구분 |
| `--text-default` | 본문, 제목 |
| `--text-subtle` | 보조 설명, 메타 |
| `--text-on-primary` | Primary 배경 위 텍스트 |
| `--border-default` | 기본 구분선, 입력 경계 |
| `--border-strong` | 포커스, 강조 경계 |
| `--action-primary` | Primary CTA |
| `--action-primary-pressed` | Primary 터치 상태 |
| `--status-success` | 성공/완료 |
| `--status-progress` | 진행 중 |
| `--status-disabled` | 비활성 |

---

## 5. Typography

### Font Family

- `Pretendard`

### Font Weight

| 토큰 | 값 |
|---|---:|
| Regular | 400 |
| Medium | 500 |
| SemiBold | 600 |
| Bold | 700 |

### Type Scale

| 토큰 | 크기 | 줄간격 | 굵기 | 용도 |
|---|---:|---:|---:|---|
| display-lg | 32px | 40px | 700 | 온보딩 핵심 카피, 메인 헤드라인 |
| heading-lg | 24px | 32px | 700 | 화면 타이틀, 강한 제목 |
| heading-md | 20px | 28px | 700 | 카드 큰 제목, 섹션 타이틀 |
| title-lg | 18px | 26px | 600 | 리스트 제목, 다이얼로그 제목 |
| title-md | 16px | 24px | 600 | 버튼, 입력 라벨, 작은 카드 제목 |
| body-lg | 16px | 24px | 400 | 본문 기본 |
| body-md | 14px | 22px | 400 | 보조 본문, 설명 문구 |
| label-md | 14px | 20px | 600 | 버튼 텍스트, 탭 라벨 |
| caption | 12px | 18px | 500 | 메타 정보, 주석 |

### Typography Rules

- 본문 기본은 `body-lg` 또는 `body-md`를 사용한다.
- 12px 텍스트는 caption 용도로만 제한한다.
- 버튼 텍스트는 기본적으로 `label-md`를 사용한다.
- 한 화면 내 제목 계층은 최대 3단계까지만 허용한다.

---

## 6. Spacing / Radius / Border / Shadow

### Spacing

#### Base Rule

- 기본 스페이싱 시스템은 **4pt grid**를 사용한다.
- 주요 컴포넌트와 카드 레이아웃은 8pt 단위 우선 사용.

#### Spacing Tokens

| 토큰 | 값 | 용도 |
|---|---:|---|
| space-1 | 4px | 미세 간격 |
| space-2 | 8px | 아이콘-텍스트 간격 |
| space-3 | 12px | 작은 그룹 간격 |
| space-4 | 16px | 기본 내부 여백 |
| space-5 | 20px | 카드 내부 여백/섹션 보조 |
| space-6 | 24px | 섹션 간격 |
| space-7 | 32px | 큰 블록 간격 |
| space-8 | 40px | 큰 여백 |

#### Layout Rules

- 화면 좌우 기본 패딩: `20px`
- 카드 내부 기본 패딩: `16px`
- 섹션 간 기본 간격: `24px`
- 리스트 아이템 간 기본 간격: `12px` 또는 `16px`

### Radius

| 토큰 | 값 | 용도 |
|---|---:|---|
| radius-sm | 8px | 태그, 작은 배지 |
| radius-md | 12px | 입력폼, 작은 카드 |
| radius-lg | 16px | 기본 버튼, 카드 |
| radius-xl | 20px | 강조 카드, 바텀시트 상단 |
| radius-pill | 999px | pill 버튼, 칩 |

### Border

| 토큰 | 값 |
|---|---:|
| border-thin | 1px |
| border-strong | 1.5px |

- 기본 border color는 `--border-default`(`#EAEAEA`)를 사용한다.
- 포커스/강조 border는 `--border-strong`(`#111111`)을 사용한다.
- 색조가 있는 border 색상(`#EDE9E4`, `#1F2A37`)은 이 시스템의 기본 border 값이 아니다.

### Shadow

| 토큰 | 값 | 용도 |
|---|---|---|
| shadow-sm | `0 2px 8px rgba(17, 17, 17, 0.06)` | 입력폼, 작은 카드 |
| shadow-md | `0 6px 20px rgba(17, 17, 17, 0.08)` | 기본 카드, 강조 버튼 |
| shadow-lg | `0 10px 28px rgba(17, 17, 17, 0.12)` | 바텀시트, 플로팅 요소 |

- 그림자는 과하게 중첩하지 않는다.
- 동일 화면에서는 shadow 단계 2개 이하만 사용하는 것을 권장한다.

---

## 7. Component Styling Rules

### 7-1. Button

#### 목적

주요 액션, 보조 액션, 텍스트 액션, 소셜 로그인 액션을 명확한 시각 우선순위로 제공한다.

#### Variant

- **Primary**: 강한 fill. 화면당 1개 원칙.
- **Secondary**: 흰 배경 + border. 보조 행동에 사용.
- **Ghost**: 배경 없음 + 텍스트만. 닫기/나중에 하기 등 텍스트성 행동.
- **Social**: 소셜 로그인 전용. 일반 버튼 시스템과 분리.

> Secondary와 Ghost는 어두운 solid fill을 기본으로 사용하지 않는다.

#### Size

| 사이즈 | 높이 | 좌우 패딩 | radius | 텍스트 |
|---|---:|---:|---:|---|
| sm | 40px | 14px | 12px | label-md |
| md | 48px | 16px | 16px | label-md |
| lg | 52px | 20px | 16px | label-md |

#### Primary Button

| 상태 | 배경 | 텍스트 | 보더 | 그림자 |
|---|---:|---:|---:|---|
| Default | `#F5A623` | `#FFFFFF` | 없음 | shadow-sm |
| Pressed | `#E99A14` | `#FFFFFF` | 없음 | 약화 |
| Disabled | `#D9D9D9` | `#FFFFFF` | 없음 | 없음 |
| Loading | `#F5A623` | `#FFFFFF` | 없음 | shadow-sm |

#### Secondary Button

Secondary는 흰 배경 위에 border로 구분한다. 어두운 solid fill은 사용하지 않는다.

| 상태 | 배경 | 텍스트 | 보더 |
|---|---:|---:|---:|
| Default | `#FFFFFF` | `#111111` | `1px solid #EAEAEA` |
| Pressed | `#F8F8F8` | `#111111` | `1px solid #111111` |
| Disabled | `#FFFFFF` | `#8E8E93` | `1px solid #EAEAEA` |

#### Ghost Button

| 상태 | 배경 | 텍스트 | 보더 |
|---|---:|---:|---:|
| Default | transparent | `#111111` | 없음 |
| Pressed | `#F8F8F8` | `#111111` | 없음 |
| Disabled | transparent | `#8E8E93` | 없음 |

#### Social Login Button

소셜 로그인 버튼은 일반 버튼 variant 시스템과 별도로 관리한다.
각 소셜 플랫폼의 브랜드 가이드라인을 따르되, 공통 규격은 아래를 기준으로 한다.

| 상태 | 배경 | 텍스트 | 보더 |
|---|---:|---:|---:|
| Default | `#FFFFFF` | `#111111` | `1px solid #EAEAEA` |
| Pressed | `#F8F8F8` | `#111111` | `1px solid #EAEAEA` |
| Disabled | `#FFFFFF` | `#8E8E93` | `1px solid #EAEAEA` |

#### 버튼 규칙

- 한 화면의 주요 CTA는 Primary 1개를 우선한다.
- 동일 영역에서 Primary 버튼 2개 이상을 병렬 배치하지 않는다.
- Secondary는 보조 행동 또는 대체 행동에 사용한다.
- Ghost는 텍스트성 행동, 닫기, 나중에 하기 등에 사용한다.
- 아이콘이 포함될 경우 텍스트와의 간격은 `8px`로 한다.
- 확인/취소 구조는 Primary + Ghost 조합을 우선한다.

---

### 7-2. Input Field

#### 목적

입력, 검색, 선택 영역에 일관된 폼 스타일을 제공한다.

#### 규격

| 항목 | 값 |
|---|---:|
| 높이 | 52px |
| 좌우 패딩 | 16px |
| radius | 16px |
| border | `1px solid #EAEAEA` |
| 배경 | `#FFFFFF` |
| 텍스트 | `#111111` |
| placeholder | `#8E8E93` |

#### 상태

| 상태 | 배경 | 보더 | 텍스트 |
|---|---:|---:|---:|
| Default | `#FFFFFF` | `#EAEAEA` | `#111111` |
| Focused | `#FFFFFF` | `#111111` | `#111111` |
| Disabled | `#F8F8F8` | `#EAEAEA` | `#8E8E93` |
| Error | `#FFFFFF` | 추후 에러 컬러 확장 | `#111111` |

#### Helper Text

- 기본 helper/caption은 `12px`, `#8E8E93`
- 오류 helper는 별도 상태 컬러가 확정되면 추가

---

### 7-3. Card

카드는 흰 배경을 기본으로 한다. 색조가 있는 카드 배경은 기본 패턴이 아니다.
영역 구분은 border, spacing, radius, shadow로 해결한다.

#### 기본 카드

| 항목 | 값 |
|---|---:|
| 배경 | `#FFFFFF` |
| radius | 20px |
| 패딩 | 16px |
| border | `1px solid #EAEAEA` |
| shadow | shadow-sm 또는 없음 |

> 카드 배경에 Primary 틴트나 색조 배경을 채우는 것은 기본 패턴이 아니다.
> 강조가 필요한 경우 border 색상 강화 또는 shadow 단계 상향으로 처리한다.

#### 카드 내 구조

- 제목과 본문 사이: `8px`
- 본문과 보조 정보 사이: `12px`
- 카드와 카드 사이: `16px`

#### 카드 규칙

- 카드 배경은 항상 `#FFFFFF`다.
- 색으로 채운 카드 배경과 색조 섹션 배경은 기본 패턴이 아니다.
- 강조가 필요 없는 카드는 border 없이 shadow-sm만 사용 가능하다.
- 정보 밀도가 높은 카드는 shadow를 줄이고 border를 사용하는 방식도 허용한다.
- 카드 내 버튼은 기본적으로 1개 주행동, 1개 보조행동을 넘지 않도록 권장한다.

---

### 7-4. Badge / Chip

#### Badge

| 항목 | 값 |
|---|---:|
| 높이 | 24px |
| 좌우 패딩 | 10px |
| radius | pill |
| 텍스트 | 12px / 600 |

#### 기본 Badge 색상

| 타입 | 배경 | 텍스트 |
|---|---:|---:|
| Neutral | `#F8F8F8` | `#8E8E93` |
| Primary | `rgba(245, 166, 35, 0.12)` | `#F5A623` |
| Success | `rgba(34, 197, 94, 0.12)` | `#22C55E` |

#### 규칙

- 배지는 상태나 속성 요약에만 사용한다.
- 칩 선택형 UI가 아닌 단순 표시용이면 interaction을 넣지 않는다.

---

### 7-5. List Item

#### 기본 규격

| 항목 | 값 |
|---|---:|
| 최소 높이 | 56px |
| 좌우 패딩 | 16px |
| 상하 패딩 | 14px |
| 배경 | `#FFFFFF` |
| 구분선 | `#EAEAEA` |

#### 구조

- Leading: 아이콘/썸네일
- Body: 제목 + 보조 설명
- Trailing: 금액, 화살표, 토글, 상태값

#### 규칙

- 제목은 최대 1줄 또는 2줄까지 허용
- 보조 설명은 `body-md` 또는 `caption`
- trailing 영역 정렬은 오른쪽 기준 고정

---

### 7-6. Tab Bar

#### 기본 규격

| 항목 | 값 |
|---|---:|
| 높이 | 72px |
| 배경 | `#FFFFFF` |
| 상단 border | `1px solid #EAEAEA` |
| 아이콘/라벨 간격 | 4px |

#### 상태

| 상태 | 아이콘/텍스트 |
|---|---:|
| Default | `#8E8E93` |
| Selected | `#F5A623` |

#### 규칙

- 탭 아이템 수는 3~5개 권장
- Selected는 색상뿐 아니라 weight 변화로도 구분 가능
- 하단 탭의 배경은 항상 `#FFFFFF` 유지

---

### 7-7. Modal / Bottom Sheet

#### Modal

| 항목 | 값 |
|---|---:|
| 배경 | `#FFFFFF` |
| radius | 20px |
| 패딩 | 20px |
| shadow | shadow-lg |

#### Bottom Sheet

| 항목 | 값 |
|---|---:|
| 배경 | `#FFFFFF` |
| 상단 radius | 20px |
| 패딩 | 20px |
| drag handle | 36x4px / `#EAEAEA` |

#### 규칙

- 확인/취소 구조는 Primary + Ghost 조합을 우선한다.
- destructive 계열은 본 문서 범위 밖으로 두고, 추후 상태 컬러 확정 시 추가한다.

---

### 7-8. Toast / Snackbar

| 항목 | 값 |
|---|---:|
| 배경 | `#111111` |
| 텍스트 | `#FFFFFF` |
| radius | 14px |
| 패딩 | 14px 16px |

- 짧은 피드백 전달에 사용한다.
- 1개 액션까지만 허용한다.

---

## 8. State Rules

### 8-1. 공통 상태 정의

| 상태 | 설명 |
|---|---|
| Default | 기본 상태 |
| Pressed | 터치/클릭 중 상태 |
| Disabled | 비활성 상태 |
| Selected | 선택 완료 상태 |
| Loading | 처리 중 상태 |
| Error | 오류 상태 (추후 확장) |
| Success | 성공/완료 상태 |
| Focused | 입력 집중 상태 |
| Progress | 진행 중 상태 |

### 8-2. 상태별 컬러 규칙

| 상태 | 컬러 | 설명 |
|---|---|---|
| Success | `#22C55E` | 완료, 정상, 성공 |
| Progress / In-Progress | `#F5A623` | 진행 중, 처리 중 |
| Disabled | `#D9D9D9` | 비활성, 사용 불가 |
| Error | 추후 확장 | border + helper text 중심으로 설계 |

### 8-3. 상태 표현 규칙

- 상태는 색상만으로 구분하지 않고, 텍스트/아이콘/보더 변화와 함께 표현한다.
- Disabled는 클릭 불가 여부가 명확히 보여야 한다.
- Pressed는 색상 변화 또는 그림자 감소로 표현한다.
- Error는 향후 확장 가능하되, 기본 문서에서는 border + helper text 중심으로 설계한다.
- Success는 반드시 `#22C55E`(green)를 사용한다. Primary 오렌지로 성공 상태를 표현하지 않는다.
- Progress/In-Progress는 `#F5A623`(orange)를 사용한다.

### 8-4. 공통 상태 토큰 가이드

| 항목 | Default | Pressed | Disabled |
|---|---|---|---|
| Primary Action | `#F5A623` | `#E99A14` | `#D9D9D9` |
| Secondary Action | `#FFFFFF` + border | `#F8F8F8` + border | `#FFFFFF` + border |
| Text | `#111111` | 유지 | `#8E8E93` |
| Border | `#EAEAEA` | `#111111` | `#EAEAEA` |

---

## 9. Screen Usage Rules

### 9-1. 공통 금지 사항

- 화면 기본 배경으로 크림/아이보리/웜 배경(`#FFF8EE`, `#FAF6F0` 등)을 사용하지 않는다.
- 섹션 배경에 Primary 틴트를 기본으로 깔지 않는다.
- 색조가 있는 배경으로 화면 계층을 구분하는 것은 기본 패턴이 아니다.
- 배경 구분이 필요하면 `#F8F8F8`(surface-muted)을 사용하고, 그것도 최소화한다.

### 9-2. 홈 화면

- 배경은 `#FFFFFF`
- 주요 카드와 CTA는 흰 배경 위에서 운영
- 첫 화면의 핵심 액션은 Primary 버튼 1개 중심
- 섹션 구분은 spacing과 border로 처리

### 9-3. 리스트 화면

- 리스트 아이템은 균일한 높이/패딩 유지
- 필터/정렬은 Ghost 또는 Secondary(border 스타일) 버튼으로 구분
- 구분선은 `#EAEAEA` 중심
- 리스트 배경은 `#FFFFFF`

### 9-4. 상세 화면

- 섹션 제목은 `heading-md` 또는 `title-lg`
- 정보 그룹은 카드 또는 구분 블록으로 정리
- CTA는 하단 고정 또는 마지막 섹션에 일관되게 배치
- 섹션 배경 틴트 없이 spacing과 border로 구분

### 9-5. 설정/폼 화면

- 입력폼은 52px 높이 통일
- helper text와 validation text 영역을 미리 확보
- 저장/완료는 Primary, 취소/닫기는 Ghost 우선

### 9-6. 빈 상태 / 에러 상태 / 로딩 상태

- Empty: 설명 + 가벼운 보조 CTA
- Error: 문제 설명 + 재시도 CTA
- Loading: skeleton 또는 progress 중심, 레이아웃 점프 최소화

---

## 10. 구현용 Quick Reference

```css
:root {
  /* Background */
  --bg-default: #FFFFFF;
  --bg-surface: #FFFFFF;
  --bg-muted: #F8F8F8;

  /* Text */
  --text-default: #111111;
  --text-subtle: #8E8E93;
  --text-on-primary: #FFFFFF;

  /* Border */
  --border-default: #EAEAEA;
  --border-strong: #111111;

  /* Action */
  --action-primary: #F5A623;
  --action-primary-pressed: #E99A14;

  /* Status */
  --status-success: #22C55E;
  --status-progress: #F5A623;
  --status-disabled: #D9D9D9;

  /* Radius */
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --radius-xl: 20px;
  --radius-pill: 999px;

  /* Spacing */
  --space-1: 4px;
  --space-2: 8px;
  --space-3: 12px;
  --space-4: 16px;
  --space-5: 20px;
  --space-6: 24px;
  --space-7: 32px;
  --space-8: 40px;
}
```

### Compose 매핑 권장 구조

- `Color.kt`: foundation / semantic color
- `Type.kt`: Pretendard typography scale
- `Shape.kt`: radius tokens
- `Dimens.kt`: spacing tokens
- `AppButton.kt`: variant + size + state 기준 공통 버튼
- `AppCard.kt`: 카드 wrapper
- `AppTextField.kt`: 공통 입력폼

---

## 11. 접근성 규칙

### 기본 원칙

- 텍스트 대비는 충분히 확보한다.
- 상태는 색상만으로 구분하지 않는다.
- 클릭 가능한 요소는 터치 타겟을 충분히 확보한다.

### 권장 기준

- 최소 터치 타겟: **44px 이상**
- 본문 최소 권장 글자 크기: **14px**
- 아이콘 버튼은 실제 보이는 아이콘보다 넓은 터치 영역을 가진다.
- Disabled 상태도 읽을 수 있어야 하며, 완전히 사라진 것처럼 보이지 않도록 한다.

### 운영 기준

- 중요 정보는 Primary 색만으로 강조하지 않고 텍스트 weight 또는 위치 계층도 함께 사용한다.
- 긴 문장은 `body-lg` 또는 `body-md`를 사용하고 줄간격을 유지한다.

---

## 12. 금지 패턴

- 화면 기본 배경에 크림/웜 배경 사용 (`#FFF8EE`, `#FAF6F0` 등)
- 섹션 배경에 Primary 틴트 기본 적용
- 색조 배경으로 화면 계층 구분
- Secondary 버튼에 어두운 solid fill 사용
- Primary 계열 색을 화면 곳곳에 과도하게 반복 사용
- 카드마다 서로 다른 radius 사용
- 버튼마다 높이/패딩을 임의 지정
- 작은 텍스트를 본문에 남용
- 상태를 색상만으로 구분
- 라이트 모드 기준 문서에 다크 모드 예시를 혼합
- Success 상태에 오렌지 사용 (반드시 green)
- Progress 상태에 green 사용 (반드시 orange)

---

## 13. 최종 정리

본 v2 문서는 기존 초안을 다음 방향으로 정리한 결과물이다.

- 크림/웜 배경 기반 → **순수 흰색 기반**으로 전환
- 어두운 solid Secondary 버튼 → **흰 배경 + border Secondary**로 전환
- 색조 배경 계층 구분 → **border / spacing / typography 계층 구분**으로 전환
- 평가형 문서 → **개발 규격형 문서**로 재구성
- Light/Dark 혼합 구조 → **Light Mode 단일 기준**으로 통일
- 많은 컬러 운영 → **핵심 포인트 컬러 + 중립색 중심**으로 단순화
- 설명 위주 문서 → **토큰/상태/컴포넌트 규격** 중심으로 구체화

따라서 본 문서는 모바일 앱 FE 구현, 공통 컴포넌트 설계, 디자인 일관성 확보를 위한 **기준 문서 v2**로 사용한다.
