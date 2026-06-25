# 애낌 백엔드

애낌 백엔드는 구독 관리 서비스의 핵심 사용자 흐름을 담당하는 Spring Boot 기반 API 서버입니다.  
소셜 로그인, 구독 관리, 추천 혜택 조회, 체크인 및 알림 자동화, 결제 데이터 관리 기능을 제공합니다.

## 백엔드 역할

| 구분 | 설명 |
| --- | --- |
| 인증 | Google / Kakao 로그인 이후 JWT 발급, 재발급, 로그아웃 처리 |
| 구독 | 서비스 / 요금제 / 번들 조회와 구독 등록, 수정, 삭제 |
| 추천 | 사용 이력과 체크인 기록을 기반으로 추천 프로모션 조회 |
| 알림 | FCM 토큰 관리, 알림함 조회 / 읽음 / 삭제, 체크인 / 프로모션 알림 발송 |
| 결제 / 인사이트 | 결제내역 저장 / 조회, 일별 사용량 흐름과 사용량 리포트 조회 |

## 폴더 구조

```text
BE/
├── src/
│   ├── main/
│   │   ├── java/com/ssafy/e106/
│   │   │   ├── domain/
│   │   │   │   └── {domain}/
│   │   │   │       ├── controller/
│   │   │   │       ├── service/
│   │   │   │       ├── repository/
│   │   │   │       ├── entity/
│   │   │   │       └── dto/
│   │   │   │           ├── request/
│   │   │   │           └── response/
│   │   │   └── global/
│   │   │       ├── common/
│   │   │       ├── config/
│   │   │       ├── controller/
│   │   │       ├── exception/
│   │   │       └── security/
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-local.yml
│   │       └── static/
│   └── test/
│       └── java/com/ssafy/e106/
├── docs/
└── build.gradle
```

| 경로 | 역할 |
| --- | --- |
| `src/main/java/com/ssafy/e106/domain/{domain}/controller` | 도메인별 API 엔드포인트 |
| `src/main/java/com/ssafy/e106/domain/{domain}/service` | 도메인 비즈니스 로직 |
| `src/main/java/com/ssafy/e106/domain/{domain}/repository` | 데이터 접근 계층 |
| `src/main/java/com/ssafy/e106/domain/{domain}/entity` | JPA 엔티티 |
| `src/main/java/com/ssafy/e106/domain/{domain}/dto` | 요청 / 응답 DTO |
| `src/main/java/com/ssafy/e106/global` | 공통 응답, 예외 처리, Security, Batch, 설정 |
| `src/main/resources` | `application.yml`, 프로파일 설정, 정적 리소스 |
| `src/test/java/com/ssafy/e106` | 백엔드 테스트 코드 |
| `docs` | ERD 등 백엔드 관련 문서 |

## 도메인 분리

| 도메인 | 담당 기능 |
| --- | --- |
| `auth` | Google / Kakao 로그인, JWT 발급 / 갱신 / 로그아웃 |
| `user` | 내 정보, 알림 설정, 선택 동의, 회원 탈퇴 |
| `subscription` | 서비스 / 번들 조회, 구독 CRUD, 체크인, 가맹점 매핑 |
| `subscriptionusage` | 일별 사용량 업로드, 사용량 리포트 / 흐름 조회 |
| `promotion` | 추천 프로모션 / 카드 혜택 / 번들 상세 조회 |
| `notification` | FCM 토큰 관리, 알림함 API, 알림 배치 |
| `payment` | 결제내역 저장 / 조회 |
| `admin` | 개발 / 운영 보조용 관리자 API |
| `global` | 공통 응답, 예외 처리, Security, Batch, 설정 |

## 구현 기능

| 영역 | 구현 내용 |
| --- | --- |
| 인증 | 소셜 로그인, JWT access / refresh token 발급, Redis 기반 refresh token 관리 |
| 사용자 | 내 정보 조회, 알림 설정 변경, 선택 동의 변경, 회원 탈퇴 |
| 구독 | 서비스 / 번들 / 요금제 조회, 구독 등록 / 수정 / 삭제, 체크인 이력 관리 |
| 추천 | 사용 이력과 체크인 기록을 반영한 추천 프로모션 조회 및 상세 조회 |
| 알림 | FCM 토큰 등록 / 갱신, 알림함 조회 / 읽음 / 삭제, 알림 발송 이력 관리 |
| 인사이트 | 일별 사용량 업로드, 사용량 리포트 조회, 일별 사용량 흐름 조회 |
| 결제 | 결제내역 저장 / 조회, 결제 기반 구독 흐름 지원 |

## 주요 프레임워크와 적용 방식

| 구분 | 사용 기술 | 적용 내용 |
| --- | --- | --- |
| 인증 / 인가 | Spring Security, JWT | 모바일 환경에 맞춰 stateless 인증 구조를 적용하고, 인가가 필요한 API를 분리 |
| 캐시 / 토큰 | Redis | refresh token 저장, 배치 중복 실행 방지, 알림 중복 발송 방지 |
| 배치 | Spring Batch | 체크인 알림과 프로모션 알림의 대상 선정과 발송을 분리해 자동화 |
| 데이터 접근 | Spring Data JPA | 도메인별 엔티티와 리포지터리를 기반으로 데이터 저장 / 조회 처리 |
| 데이터베이스 | MySQL | 사용자, 구독, 결제, 알림, 프로모션 관련 데이터 관리 |
| 알림 | Firebase Admin SDK, FCM | 앱 푸시 알림 전송과 토큰 기반 사용자 기기 매핑 처리 |
| 문서화 | springdoc OpenAPI | Swagger UI 기반 API 문서 확인 지원 |

## 실행 방법

| 단계 | 설명 |
| --- | --- |
| 1 | JDK 17, MySQL, Redis, 루트 `.env` 파일을 준비합니다. |
| 2 | 루트에서 `Copy-Item ..\\.env.example ..\\.env`로 환경 파일을 복사합니다. |
| 3 | 루트에서 `docker compose up -d --build`로 `mysql`, `redis`, `backend`를 함께 실행할 수 있습니다. |
| 4 | 백엔드만 실행할 경우 `.\gradlew.bat bootRun`을 사용합니다. |
| 5 | 빌드와 테스트는 `.\gradlew.bat build`, `.\gradlew.bat test`로 수행합니다. |

## 주요 API

| 도메인 | 메서드 | 경로 | 설명 |
| --- | --- | --- | --- |
| 인증 | `POST` | `/api/v1/auth/login/google` | Google 로그인 |
| 인증 | `POST` | `/api/v1/auth/login/kakao` | Kakao 로그인 |
| 인증 | `POST` | `/api/v1/auth/refresh` | access token 재발급 |
| 사용자 | `GET` | `/api/v1/users/me` | 내 정보 조회 |
| 사용자 | `PATCH` | `/api/v1/users/me/notification-settings` | 알림 설정 변경 |
| 구독 | `GET` | `/api/v1/services` | 서비스 목록 조회 |
| 구독 | `GET` | `/api/v1/bundles` | 번들 목록 조회 |
| 구독 | `POST` | `/api/v1/subscriptions` | 구독 등록 |
| 구독 | `GET` | `/api/v1/subscriptions/{subscriptionId}` | 구독 상세 조회 |
| 추천 | `GET` | `/api/v1/promotions/recommendations` | 추천 프로모션 조회 |
| 추천 | `GET` | `/api/v1/promotions/{promotionId}` | 프로모션 상세 조회 |
| 체크인 | `POST` | `/api/v1/checkins` | 체크인 제출 |
| 알림 | `POST` | `/api/v1/notifications/tokens` | FCM 토큰 등록 / 갱신 |
| 알림 | `GET` | `/api/v1/notifications` | 알림 목록 조회 |
| 알림 | `PATCH` | `/api/v1/notifications/{notificationId}/read` | 알림 읽음 처리 |
| 인사이트 | `GET` | `/api/v1/subscription-usages/report` | 구독 사용량 리포트 조회 |
| 결제 | `GET` | `/api/v1/payment-history` | 결제내역 조회 |

## 참고 문서

| 문서 | 경로 |
| --- | --- |
| API 문서 | [`../docs/api`](../docs/api) |
| DB ERD | [`docs/2026-03-20_DB_ERD.md`](docs/2026-03-20_DB_ERD.md) |
| 설정 파일 | [`src/main/resources/application.yml`](src/main/resources/application.yml) |
