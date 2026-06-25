# 애낌 (AEKKIM) FE Android

`FE/`는 애낌 Android 앱 프론트엔드 모듈이다. 서비스명은 `애낌(AEKKIM)`이고, 앱 식별자는 `com.ssafy.e106`이다.

현재 프로젝트는 Jetpack Compose 기반 단일 `app` 모듈 구조로 운영되며, 구독 분석, 구독 관리, 알림, 체크인, 마이페이지까지 앱 흐름이 한 저장소 안에 정리되어 있다.

## 현재 구현 범위

- 인증/온보딩: Google 로그인, Kakao 로그인, 약관 동의, 서비스 이용약관/개인정보처리방침/마케팅 동의 상세
- 권한/분석: 알림 권한, UsageStats 권한 요청, 결제 후보 조회, 사용 기록 수집, 온디바이스 AI 보조 분석, 구독 확정
- 홈: 대시보드, 구독 상세, 인사이트, 확인 필요 결제 수동 매핑
- 운영 기능: 체크인, 체크인 후속 권유, 해지 가이드, 프로모션 목록/상세, 알림함, 마이페이지, 회원탈퇴
- 연동: FCM 토큰 동기화, 푸시 딥링크(`aekkim://`), Kakao SDK 초기화, 공용 debug keystore

현재 `Naver` 로그인 버튼은 노출되지만 실제 로그인 연동은 되어 있지 않고 안내 토스트만 표시한다.

## 기술 스택

- Kotlin `2.1.0`, AGP `8.8.0`, Gradle `8.14.4`, Java 17
- Jetpack Compose, Material 3, Navigation Compose, Lifecycle ViewModel
- Hilt + KSP
- Retrofit, OkHttp, Kotlinx Serialization
- Firebase Messaging, Firebase Analytics 의존성, Credential Manager(Google), Kakao SDK
- EncryptedSharedPreferences, DataStore
- Coil, SVG asset 로딩
- LiteRT-LM, FastText, XGBoost JSON 추론 기반 온디바이스 분석 파이프라인

## 주요 디렉터리

```text
FE/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── assets/
│       │   │   ├── dashboard/
│       │   │   ├── ml/
│       │   │   ├── onboarding/
│       │   │   └── services/
│       │   ├── java/com/ssafy/e106/
│       │   │   ├── ai/
│       │   │   ├── app/
│       │   │   ├── core/
│       │   │   ├── data/
│       │   │   ├── di/
│       │   │   └── feature/
│       │   └── res/
│       ├── debug/
│       └── test/
├── docs/
├── gradle/
├── signing/
├── Mock-up/
├── prototype/
├── google-services.json.example
└── local_ondevice.properties
```

패키지 역할은 아래 기준으로 보면 된다.

- `app`: `Application`, `MainActivity`, NavHost, 라우팅
- `ai`: 온디바이스 모델 로더와 추론 엔진
- `feature`: 화면, Contract, ViewModel
- `data`: API, DTO, Repository, 로컬 저장소, 인증 인터셉터
- `core`: 공통 UI, theme, result, network 유틸
- `di`: Hilt 모듈

## 실행 환경

필수 전제는 아래와 같다.

- Android Studio 최신 안정 버전
- Android SDK 설치
- JDK 17
- `app/google-services.json` 준비
- `FE/local.properties` 준비

`local_ondevice.properties`는 실기기 테스트용 예시 파일이다. 실제 빌드는 `local.properties`만 읽으므로 필요한 값을 복사해서 사용해야 한다.

### `local.properties` 예시

```properties
sdk.dir=C\:\\Users\\<USER>\\AppData\\Local\\Android\\Sdk
BASE_URL_DEV=http://10.0.2.2:8080/
BASE_URL_EMULATOR=http://10.0.2.2:8080/
BASE_URL_DEVICE=http://192.168.0.10:8080/
BASE_URL_PROD=https://your-prod-api.example.com/
GOOGLE_WEB_CLIENT_ID=your_google_web_client_id_here
KAKAO_NATIVE_APP_KEY=your_kakao_native_key_here
```

환경 키 해석 방식은 다음과 같다.

- 에뮬레이터 debug: `BASE_URL_EMULATOR` 우선, 비어 있으면 `BASE_URL_DEV`
- 실기기 debug: `BASE_URL_DEVICE` 우선, 비어 있으면 `BASE_URL_EMULATOR`
- release: `BASE_URL_PROD`
- URL에 스킴이 없으면 fallback URL이 사용된다.

## 실행 및 빌드

### Android Studio

1. Android Studio에서 `FE/`를 연다.
2. `app/google-services.json`과 `local.properties`를 맞춘다.
3. 빌드 변형을 선택한다.
4. 에뮬레이터 또는 실기기에서 실행한다.

빌드 변형은 아래 3개를 기준으로 본다.

- `debug`: 기본 개발용
- `device`: 실기기 디버그 환경 전용 base URL 분리용
- `release`: 배포용, minify/shrink 적용

### CLI

```bash
# 디버그 앱 설치
.\gradlew.bat installDebug

# 실기기용 device 빌드 APK 생성
.\gradlew.bat assembleDevice

# 릴리즈 APK/AAB
.\gradlew.bat assembleRelease
.\gradlew.bat bundleRelease

# 린트 / 테스트
.\gradlew.bat lintDebug
.\gradlew.bat testDebugUnitTest
```

## 테스트

단위 테스트는 `app/src/test` 아래에 있다. 현재는 대시보드 계산, 인사이트 UI 해석, 프로모션 추천 정책, 서비스 비주얼 매핑, 온디바이스 계약 로직 중심으로 테스트가 들어가 있다.

대표 실행 명령은 아래와 같다.

```bash
.\gradlew.bat testDebugUnitTest
```

## 참고 문서

운영과 유지보수 기준 문서는 `FE/docs`에서 관리한다.

- [FE 문서 가이드](docs/README.md)
- [운영 유지보수 인수인계 가이드](docs/maintenance/2026-03-29_FE_운영_유지보수_인수인계_가이드.md)
- [운영 체크리스트 및 트러블슈팅](docs/maintenance/2026-03-29_FE_운영_체크리스트_및_트러블슈팅.md)
- [화면명세서 기반 전체 체크리스트](docs/maintenance/2026-03-19_FE_화면명세서_기반_전체_체크리스트.md)
- [디자인시스템 구현기준 및 문서정합성](docs/maintenance/2026-03-28_FE_디자인시스템_구현기준_및_문서정합성.md)

루트 `docs/`에는 서비스 공통 문서만 남기고, FE 전용 문서는 `FE/docs/`에서 관리한다.

## 유의사항

- `google-services.json`은 Git에 포함하지 않는다. 실제 사용 경로는 `app/google-services.json`이다.
- debug 서명은 `signing/debug.keystore` 공용 키스토어를 사용한다.
- 앱 시작 시 FCM 토큰 동기화와 알림 채널 생성이 수행된다.
- Kakao 로그인은 `KAKAO_NATIVE_APP_KEY`가 비어 있으면 초기화되지 않는다.
- 딥링크 스킴은 `aekkim://`이다.
