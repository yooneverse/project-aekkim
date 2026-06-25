# Spring Batch 도입 계획

## 1. 작성 목적

- 프로젝트에 Spring Batch를 도입하는 이유와 적용 범위를 정리한다.
- Scheduler와 Spring Batch의 역할 차이를 명확히 정리한다.
- 1차 도입 대상인 알림 대상 계산 배치의 구현 방향을 문서화한다.

## 2. 왜 Spring Batch를 도입하는가

우리 프로젝트에는 사용자의 직접 요청과 무관하게 주기적으로 처리해야 하는 작업이 있다.

- 결제일 D-3 대상 사용자 판단
- 체크인/프로모션 알림 대상 계산 및 발송

이 작업들은 단순 API 처리보다 다음 특성이 강하다.

- 전체 사용자 대상 대량 처리
- 주기 실행 필요
- 실패/재시도 관리 필요
- 앱 진입 시점이 아니라 사전 계산이 더 적합

따라서 단순 Scheduler만으로 처리하기보다, 대량 데이터 처리에 특화된 Spring Batch를 함께 도입하는 것이 더 적절하다.

## 3. Scheduler와 Spring Batch의 역할 차이

### 3.1 Scheduler

Scheduler는 작업의 실행 시점을 정한다.

예:

- 매일 04:00 CHECKIN 대상 선정 실행 / 04:10 PROMO 대상 선정 실행
- 매일 09:00 CHECKIN 알림 발송 실행 / 09:10 PROMO 알림 발송 실행

즉 Scheduler의 책임은 "언제 실행할지"이다.

### 3.2 Spring Batch

Spring Batch는 실제 작업을 안정적으로 수행한다.

예:

- 많은 데이터를 일정 단위로 나누어 처리
- 실행 이력 관리
- 실패/재시도 처리
- 대량 데이터 읽기/가공/저장

즉 Spring Batch의 책임은 "대량 데이터를 어떻게 안정적으로 처리할지"이다.

### 3.3 결론

이 프로젝트에서는 다음 구조를 사용한다.

- Scheduler가 정해진 시간에 Job을 실행한다.
- Spring Batch가 실제 데이터를 읽고, 가공하고, 저장한다.

## 4. 프로젝트에서 Spring Batch를 적용할 위치

### 4.1 1차 도입 대상

#### 결제일 D-3 체크인 판단 배치

- 목적: 결제일이 3일 남은 사용자 중 체크인 발송 대상을 계산한다.
- 실행 시점: 매일 새벽

#### 체크인 알림 발송 배치

- 목적: 계산된 대상에게 실제 체크인 알림을 발송한다.
- 실행 시점: 매일 09:00

#### 프로모션 알림 대상 계산/발송 배치

- 목적: 추천 점수 기준을 통과한 프로모션 중 미발송 top 1을 선정해 알림 발송 대상으로 만든다.
- 기준:
  - 활성 프로모션만 대상
  - 사용자 구독과 매칭되는 프로모션만 대상
  - 이미 보낸 프로모션은 제외

### 4.2 이후 확장 가능 대상

#### 결제내역 적재/정제 배치

- 목적: 백엔드 결제내역 저장소를 주기적으로 정리하고, `GET /api/v1/payment-history`로 제공할 데이터를 정제한다.
- 실행 시점: 추후 결정
- 성격: 대량 데이터 읽기 + 가공 + 저장

## 5. 알림 배치 적용 범위

현재 프로젝트에서 배치로 다룰 알림 타입은 아래 2개다.

- `CHECKIN`
- `PROMO`

`CHURN_REVIEW`는 현재 1차 범위에서 제외한다.

### 5.1 CHECKIN 배치

CHECKIN은 결제일 D-3 기준으로 동작한다.

사용량 원본 데이터는 개인정보 이슈로 백엔드에 저장하지 않는다.
대신 프론트가 최근 30일 UsageStats를 기준으로 저사용 여부만 계산해 백엔드에 전달한다.

예:

- `subscriptions.low_usage_detected`
- `subscriptions.low_usage_cycle_ym`

즉 백엔드는 원본 사용시간이 아니라, "이번 달 저사용 상태인지"만 저장하고 배치 판단에 사용한다.

#### CHECKIN 대상 판단 배치

- 목적: 오늘 기준 결제일이 3일 남은 구독 중 체크인 발송이 필요한 사용자를 선정한다.
- 실행 시점: 매일 새벽
- 선정 기준:
  - `daysUntilBilling == 3`
  - 구독이 활성 상태
  - `low_usage_detected == true`
  - `low_usage_cycle_ym == 현재월`
  - 체크인 알림 수신 동의 상태
  - 이미 이번 사이클 체크인을 완료하지 않은 사용자

#### CHECKIN 알림 발송 배치

- 목적: 새벽에 계산된 CHECKIN 대상에게 실제 FCM을 발송한다.
- 실행 시점: 매일 09:00
- 발송 방식:
  - `notifications.sent_at is null` 인 CHECKIN 후보만 읽는다.
  - 같은 사용자, 같은 구독, 같은 사이클 기준의 CHECKIN 후보만 실제 발송 대상으로 사용한다.
  - 발송 성공 시에만 `sent_at` 을 기록하고, 실제 발송된 알림만 사용자 알림 목록에 노출한다.
- 실패 처리:
  - 발송 배치는 `chunk(1)` 기준으로 처리한다.
  - 일시적 FCM 오류는 retry 대상이다.
  - 토큰 없음, 무효 토큰 등 재시도 의미가 없는 오류는 skip 대상으로 처리한다.
  - 중간 실패가 나도 앞에서 성공한 발송의 `sent_at` 은 롤백되지 않도록 설계한다.

즉 CHECKIN은 아래 2단계로 나눈다.

1. 새벽: 판단
2. 오전 9시: 실제 발송

### 5.2 PROMO 배치

PROMO는 특정 결제일 기준이 아니라, 활성 프로모션과 사용자 구독 조합을 기준으로 선정한다.

PROMO도 동일하게 프론트가 최근 30일 기준 저사용 여부만 백엔드에 기록하고, 배치는 이 값을 후보 필터에 사용한다.

#### PROMO 대상 판단 배치

- 목적: 사용자별로 오늘 보낼 프로모션 알림 후보를 선정한다.
- 실행 시점: 새벽
- 선정 기준:
  - `low_usage_detected == true`
  - `low_usage_cycle_ym == 현재월`
  - 활성 프로모션만 대상
  - 사용자 구독과 서비스 매칭이 되는 프로모션만 대상
  - 추천 점수 기준치를 넘는 프로모션만 대상
  - 이미 같은 사용자에게 발송한 프로모션은 제외
  - 최종적으로 사용자당 상위 1개만 선정

#### PROMO 알림 발송 배치

- 목적: 새벽에 선정된 PROMO 대상에게 실제 FCM을 발송한다.
- 실행 시점: 매일 09:10
- 발송 방식:
  - `notifications.sent_at is null` 인 PROMO 후보만 읽는다.
  - 사용자별 top 1로 선정된 PROMO 후보만 실제 발송 대상으로 사용한다.
  - 발송 성공 시에만 `sent_at` 을 기록하고, 실제 발송된 알림만 사용자 알림 목록에 노출한다.
- 실패 처리:
  - 발송 배치는 `chunk(1)` 기준으로 처리한다.
  - 일시적 FCM 오류는 retry 대상이다.
  - 토큰 없음, 무효 토큰 등 재시도 의미가 없는 오류는 skip 대상으로 처리한다.
  - 중간 실패가 나도 앞에서 성공한 발송의 `sent_at` 은 롤백되지 않도록 설계한다.

### 5.3 PROMO 선정 정책

PROMO는 아래 순서로 선정한다.

1. 활성 프로모션 전체 조회
2. 사용자 구독과 매칭되는 프로모션만 필터링
3. 추천 점수 계산
4. 점수 높은 순 정렬
5. 이미 발송한 프로모션 제외
6. 기준치를 통과한 후보 중 상위 1개 선정

즉 PROMO는 "오늘 기준 사용자에게 가장 의미 있는 프로모션 1개"만 발송하는 구조로 시작한다.

### 5.4 중복 발송 방지

중복 발송 방지는 Redis를 사용한다.

예:

- `promo:sent:{userId}:{promotionId}`
- `promo:send-lock:{userId}:{promotionId}`

동작 원칙:

- 같은 사용자에게 같은 프로모션을 다시 보내지 않는다.
- 발송 직전에는 `promo:send-lock` 으로 동시 발송을 막는다.
- 일반 프로모션은 Redis TTL을 프로모션 종료일까지로 설정한다.
- 종료일이 과도하게 먼 상시 프로모션은 30일 TTL을 사용한다.
- 상시 프로모션은 30일이 지나면 다시 발송 후보가 될 수 있으며, 실제로 다시 발송된 경우 Redis 키 TTL을 다시 30일로 설정한다.

### 5.5 프론트 연계 방식

프론트는 UsageStats 원본을 백엔드에 전달하지 않는다.
대신 하루 1회, 최근 30일 앱 사용량을 로컬에서 판정한 뒤 "저사용 감지 여부"만 백엔드에 기록한다.

예시 API 방향:

- `POST /api/v1/subscriptions/usage-qualification`

예시 요청:

```json
{
  "items": [
    {
      "subscriptionId": 101,
      "lowUsageDetected": true,
      "cycleYm": "2026-03"
    }
  ]
}
```

이 값은 CHECKIN/PROMO 알림 대상 판단의 입력값이다.

- `lowUsageDetected` / `cycleYm`: 프론트가 하루 1회 계산한 최근 30일 저사용 감지 여부
- `lowUsageDetected == true` 인 항목만 해당 배치의 알림 발송 후보가 된다.
- 최종 알림 발송 여부는 결제일, 활성 프로모션, 수신 동의, Redis 중복 방지 조건을 함께 만족할 때 확정한다.

## 6. Redis 사용 범위

Spring Batch 도입과 함께 Redis는 아래 최소 범위로 사용한다.

### 5.1 필수 사용

- 배치 실행 lock
- 프로모션 알림 중복 방지 키

예:

- `batch:lock:promo-selection`
- `batch:lock:promo-send`
- `batch:lock:checkin-selection`
- `batch:lock:checkin-send`
- `promo:sent:{userId}:{promotionId}`
- `promo:send-lock:{userId}:{promotionId}`

### 5.2 지금은 보류

- 활성 프로모션 캐시
- 마지막 배치 성공 시각 저장
- 변경 감지용 hash 저장

위 항목은 성능 최적화 단계에서 검토한다.

## 7. Spring Batch 핵심 개념

### 6.1 Job

배치 작업 전체 단위

예:

- `checkinTargetSelectionJob`
- `checkinNotificationSendJob`
- `promoTargetSelectionJob`
- `promoNotificationSendJob`

### 6.2 Step

Job 안의 실행 단계

### 6.3 Reader

데이터를 읽는다.

예:

- DB에서 D-3 대상 구독 읽기
- `low_usage_detected=true` + `low_usage_cycle_ym=현재월`인 구독 읽기

### 6.4 Processor

데이터를 가공하거나 판단한다.

예:

- 체크인 대상 여부 계산
- 프로모션 후보 점수 계산

### 6.5 Writer

가공 결과를 저장한다.

예:

- 알림 대상 저장
- 알림 발송 이력 저장

### 6.6 Chunk

데이터를 일정 개수씩 묶어서 처리하는 단위

예:

- 100건 읽기
- 100건 가공
- 100건 저장

## 8. 기본 코드 구조 예시

```java
@Configuration
@RequiredArgsConstructor
public class CheckinTargetSelectionJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;

  @Bean
  public Job checkinTargetSelectionJob(Step checkinTargetSelectionStep) {
    return new JobBuilder("checkinTargetSelectionJob", jobRepository)
            .start(checkinTargetSelectionStep)
            .build();
  }

  @Bean
  public Step checkinTargetSelectionStep(
          ItemReader<CheckinCandidate> reader,
          ItemProcessor<CheckinCandidate, CheckinTarget> processor,
          ItemWriter<CheckinTarget> writer
  ) {
    return new StepBuilder("checkinTargetSelectionStep", jobRepository)
            .<CheckinCandidate, CheckinTarget>chunk(100, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
  }
}
```

이 코드는 다음 역할을 한다.

- `checkinTargetSelectionJob`이라는 배치 작업을 정의한다.
- Job 안에서 `checkinTargetSelectionStep`을 실행한다.
- Step은 체크인 후보 데이터를 100건씩 읽고, 가공하고, 저장한다.

즉 체크인 대상 선정 배치의 최소 골격이다.

## 9. 프로젝트 패키지 구조 제안

```text
com.ssafy.e106
 ├─ domain
 │   ├─ notification
 │   │   ├─ batch
 │   │   │   ├─ CheckinTargetSelectionJobConfig.java
 │   │   │   ├─ CheckinNotificationSendJobConfig.java
 │   │   │   ├─ PromoTargetSelectionJobConfig.java
 │   │   │   ├─ PromoNotificationSendJobConfig.java
 │   │   │   ├─ reader
 │   │   │   ├─ processor
 │   │   │   └─ writer
 └─ global
     ├─ batch
     │   ├─ config
     │   │   └─ BatchInfrastructureConfig.java
```

원칙:

- 공통 배치 설정은 `global.batch`
- 실제 비즈니스 Job은 각 도메인 아래 배치 패키지에 둔다.

## 10. 1차 구현 범위

이번 1차 도입 범위는 알림 대상 계산 배치로 제한한다.

### 포함

- `spring-boot-starter-batch` 의존성 추가
- 배치 공통 설정 추가
- `checkinTargetSelectionJob` 골격 추가
- `promoTargetSelectionJob` 골격 추가
- `subscriptions.low_usage_detected`, `subscriptions.low_usage_cycle_ym` 기준 반영
- CHECKIN 대상 판단 배치와 PROMO 대상 판단 배치의 Reader / Processor / Writer 구조 정의
- CHECKIN 알림 발송 배치를 매일 09:00에, PROMO 알림 발송 배치를 매일 09:10에 실행하도록 Scheduler 연결

### 제외

- 결제내역 적재/정제 배치
- Redis 고도화 전략
- UsageStats 원본 저장

## 11. 다음 단계

1. `build.gradle`에 Spring Batch 의존성 추가
2. `global.batch` / `domain.notification.batch` 패키지 생성
3. `checkinTargetSelectionJob` 최소 골격 구현
4. `promoTargetSelectionJob` 최소 골격 구현
5. Scheduler 연결

## 12. 결론

Spring Batch는 이 프로젝트에서 Scheduler를 대체하는 기술이 아니라, Scheduler와 함께 사용하는 대량 처리 프레임워크다.

현재 프로젝트에서는 다음 구조가 가장 적절하다.

- CHECKIN 대상 판단: 매일 새벽 배치
- PROMO 대상 판단: 매일 새벽 배치
- CHECKIN 알림 전송: 매일 09:00 배치
- PROMO 알림 전송: 매일 09:10 배치

따라서 첫 도입은 알림 대상 계산 배치를 목표로 작게 시작하고, 이후 필요 시 다른 도메인으로 점진적으로 확장한다.
