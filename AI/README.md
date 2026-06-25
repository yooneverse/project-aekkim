# 애낌 (Aekkim) AI 작업 요약

## 내가 맡은 영역

| 영역 | 역할 | 핵심 산출물 |
|------|------|------------|
| **AI/scraper** | OTT·음악·AI 서비스 요금제/프로모션 자동 수집 파이프라인 | Python 스크래퍼 + Gemini LLM 정규화 + MySQL 적재 |
| **FE/ai** | Android On-Device 구독 자동 탐지 AI | FastText + XGBoost 순수 Kotlin 구현 (앱 내 오프라인 추론) |

---

# 1. AI/scraper — 데이터 수집 파이프라인

## 설계 핵심

**문제**: 10개 이상 서비스의 요금제 페이지가 각각 다른 구조(SPA, SSR, RSC payload 등)를 가짐. CSS 셀렉터 하드코딩은 사이트 리뉴얼마다 깨짐.

**해결**: 스크래퍼는 원본 데이터를 "대략적으로" 파싱하고, **Gemini 2.0 Flash LLM이 의미 단위로 정규화**하는 2단계 구조를 채택.

```
스크래퍼 (비정형 파싱) → LLM 정규화 (표준 JSON) → Pydantic 검증 → MySQL UPSERT
                                                     ↓ (실패 시)
                                                   dlq/ 보관
```

- LLM 출력은 `Pydantic` 스키마로 강제 검증 → 환각/형식 오류 차단
- 번들 상품(`services ≥ 2`)과 단일 서비스를 Python 레벨에서 분리 후 각각 다른 프롬프트로 LLM 호출 → 정확도 향상
- 실패 원본은 Dead Letter Queue(`dlq/`)에 보존 → 수동 재처리 가능

## 폴더 구조

```
AI/scraper/
├── .env                          # GMS API Key, DB 접속 정보
├── base.py                       # 추상 기반 클래스 (OTTScraper, MusicScraper, AIScraper 등)
├── scheduler.py                  # APScheduler 엔트리포인트 (일/주/월 스케줄)
│
├── AI/                           # ChatGPT, Claude, Gemini 요금제 스크래퍼
├── ott/                          # Netflix, Tving, Wavve, Disney+, Watcha
├── music/                        # 멜론, 벅스
├── card/                         # Card-Gorilla 신용카드 혜택
├── promotion/                    # 서비스별 프로모션 + 네이버 멤버십
│
├── db/client.py                  # MySQL UPSERT (Service, ServicePlan, Promotion)
│
├── llm/
│   ├── normalizer.py             # Gemini 2.0 Flash 정규화 핵심 모듈
│   ├── schemas.py                # Pydantic 검증 스키마
│   └── prompts/                  # 용도별 시스템 프롬프트 4종
│
├── test/                         # 통합 테스트
└── result/dlq/                   # LLM 실패 원본 보관
```

## 실행

```bash
python scheduler.py plans        # 요금제 수집 (일간)
python scheduler.py promotions   # 프로모션 수집 (주간)
python scheduler.py all          # 전체 즉시 실행
python scheduler.py              # 스케줄러 데몬 모드
```

---

# 2. FE/ai — On-Device 구독 탐지 AI

## 설계 과정과 핵심 결정

### 1차 시도: On-Device LLM (Gemma3 270M)

초기에는 Google LiteRT 위에 Gemma3 270M LLM을 올려 자연어 프롬프트로 구독 여부를 판정하는 방식을 사용.

**발생한 문제:**
- 모델 크기 ~270MB → 앱 번들 불가, 별도 다운로드 필수
- 추론 속도 수 초~수십 초 → UX 부적합
- JSON 출력 파싱 불안정 (LLM 환각으로 깨진 JSON 반환)

### 2차 시도: FastText + XGBoost 경량 파이프라인

LLM을 걷어내고 **FastText(텍스트 분류) + XGBoost(종합 판정)** 2단 파이프라인으로 전면 교체.

| 비교 | Gemma3 LLM | FastText + XGBoost |
|------|-----------|-------------------|
| 모델 크기 | ~270MB (다운로드) | ~21MB (앱 번들) |
| 추론 속도 | 수 초~수십 초 | 수 ms |
| 오프라인 | 불가 (다운로드 필요) | 완전 오프라인 |
| 외부 의존성 | LiteRT SDK | **없음** (순수 Kotlin) |

### 이식 과정에서 만난 문제와 해결

**FastText 이식:**
- 공식 Android JNI 래퍼 미배포 → **순수 Kotlin으로 .bin 바이너리 파서 직접 구현**
- TFLite 변환 시도 → 서브워드 n-gram 재현 불가 (오차 0.4 이상) → .bin 직접 파싱으로 전환
- 바이너리 파싱 오정렬 52바이트 버그 발견·수정 (Args/Dict 필드 크기 불일치)
- 기본 bucket=2M → 모델 762MB → bucket=50000으로 재학습 → **19MB로 97.5% 감소**, 성능 동일

**XGBoost 이식:**
- `xgboost4j-android` Maven 아티팩트 미존재 → **순수 Kotlin으로 JSON 트리 파서 직접 구현**
- ONNX 변환 시도 → 피처명 호환 오류 → XGBoost native JSON 직접 로드로 전환
- 피처 26개 → 21개 축소 (Android에서 계산 불가한 5개 제거)

**모델 정확도 문제:**
- 카페·편의점이 `REVIEW_NEEDED`로 오분류 → 학습 데이터의 레이블 불균형이 원인
- candidateScore 1.0~2.0 구간에서 `REVIEW_NEEDED`가 72.7% → 모델이 이 구간을 무조건 REVIEW로 학습
- 해결: 레이블 규칙 재정의 + MINIMUM_CANDIDATE_SCORE 1.0→1.5 상향 + 후처리 필터 추가

### 최종 파이프라인 인터페이스 설계

AI 엔진만 교체하고 **`resolve()` 시그니처와 반환 타입은 유지** → 나머지 FE 코드 변경 없음.

```kotlin
// 이 인터페이스는 LLM 시절부터 동일 — 엔진 내부만 교체됨
suspend fun resolve(reviewHints: List<BatchLookupReviewHint>): List<CandidateResolution>
```

LiteRT 관련 코드(`LiteRtEngine.kt`, `ModelDownloader.kt`)는 롤백 대비로 비활성화 상태 보존.

## 폴더 구조

```
com/ssafy/e106/
├── ai/                                      # AI 엔진 코어
│   ├── AiInterface.kt                       #   파이프라인 초기화 & 엔진 싱글턴
│   ├── AiModelAssetManager.kt               #   assets → 내부 저장소 복사/캐싱
│   ├── FastTextEngine.kt                    #   순수 Kotlin FastText (.bin 파싱 + 추론)
│   ├── XgboostEngine.kt                     #   순수 Kotlin XGBoost (JSON 파싱 + 추론)
│   ├── LiteRtEngine.kt                      #   [비활성화] Gemma3 LLM (롤백용 보존)
│   └── ModelDownloader.kt                   #   [비활성화] LiteRT 다운로더
│
└── feature/analysis/                        # 분석 파이프라인
    ├── AnalysisPipelineOrchestrator.kt      #   8단계 파이프라인 조율
    ├── OnDeviceAiResolver.kt                #   FastText + XGBoost + 후처리 필터
    ├── RuleBasedCandidateResolver.kt        #   규칙 기반 1차 필터
    ├── SubscriptionCandidateExtractor.kt    #   후보 추출 & 스코어링
    ├── PaymentRecordNormalizer.kt           #   가맹점명 정규화
    ├── UsageStatsCollector.kt               #   앱 사용 통계 수집
    └── model/                               #   데이터 모델 (후보, 판정 결과, 결제 레코드 등)
```

## 추론 흐름 요약

```
가맹점명 텍스트
    │
    ▼
FastTextEngine: 서브워드 n-gram → 임베딩 → softmax → 구독 확률 (f0)
    │
    ▼
XgboostEngine: f0 + 20개 피처 → 855 트리 탐색 → 3-class softmax
    │
    ▼
후처리 필터: 오프라인 패널티 / 신뢰도 임계값 / false positive 방지
    │
    ▼
CandidateResolution: CONFIRMED_SUBSCRIPTION / CONFIRMED_NON_SUBSCRIPTION / REVIEW_NEEDED
```

## 디버깅

```bash
adb logcat -s AEKKIM_AI_ASSET:D AEKKIM_AI_FT:D AEKKIM_AI_XGB:D AEKKIM_AI_RESOLVER:I
```
