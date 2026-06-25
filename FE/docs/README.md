# FE 문서 가이드

> 마지막 업데이트: 2026-03-29

프로젝트 마무리 기준으로 `FE/docs`는 FE 전용 문서의 단일 보관 위치다.
운영, 유지보수, 인수인계에 직접 필요한 문서는 `maintenance/`에 두고,
작업 이력, 과거 계획서, 설정 참고자료는 `archive/` 아래로 분리한다.
루트 `docs/`에 있던 FE 전용 문서도 같은 기준으로 `FE/docs`로 편입한다.

## 1. 현재 활성 문서 위치

활성 문서는 모두 `maintenance/` 아래에 둔다.

- `maintenance/2026-03-29_FE_운영_유지보수_인수인계_가이드.md`
- `maintenance/2026-03-29_FE_운영_체크리스트_및_트러블슈팅.md`
- `maintenance/2026-03-28_FE_디자인시스템_구현기준_및_문서정합성.md`
- `maintenance/2026-03-21_FE_체크인_배치_연동_가이드.md`
- `maintenance/2026-03-21_FE_프로모션_배치_연동_가이드.md`
- `maintenance/2026-03-19_FE_화면명세서_기반_전체_체크리스트.md`
- `maintenance/2026-03-12_FE_fdesign_css_definition__v2.md`
- `maintenance/2026-03-12_FE_mobile_component_spec_v1.md`

## 2. 문서 사용 순서

1. 실행 환경 복구, 외부 연동 확인, 인수인계 시작:
   `2026-03-29_FE_운영_유지보수_인수인계_가이드.md`
2. 장애 대응, 스모크 테스트, 회귀 점검:
   `2026-03-29_FE_운영_체크리스트_및_트러블슈팅.md`
3. 알림 배치와 FE 노출 흐름 확인:
   `2026-03-21_FE_체크인_배치_연동_가이드.md`,
   `2026-03-21_FE_프로모션_배치_연동_가이드.md`
4. 현재 코드 기준 UI/디자인/공용 컴포넌트 확인:
   `2026-03-28_FE_디자인시스템_구현기준_및_문서정합성.md`
5. 화면별 구현 상태와 QA 범위 확인:
   `2026-03-19_FE_화면명세서_기반_전체_체크리스트.md`
6. 목표 디자인 토큰/컴포넌트 규격 확인:
   `2026-03-12_FE_fdesign_css_definition__v2.md`,
   `2026-03-12_FE_mobile_component_spec_v1.md`

## 3. archive 폴더 역할

- `archive/planning-history`
  - 주간 계획, 발표 준비, 우선순위, 태스크 백로그 같은 일회성 실행 문서
- `archive/change-history`
  - 화면 수정, 버그 수정, 특정 이슈 해결 기록
- `archive/completed-stories`
  - 완료된 스토리 단위 계획서
- `archive/design-drafts`
  - 설계 초안, 실험성 디자인 문서
- `archive/legacy-reference`
  - 현재 기준과 어긋나는 구 문서 또는 참고용 문서
- `archive/onboarding`
  - 신규 인원 학습용, Android/환경설정 참고 문서
- `archive/reference-only`
  - 보존 목적의 참고 문서

## 4. 이번에 루트 docs에서 편입한 FE 문서

- `maintenance`
  - `2026-03-21_FE_체크인_배치_연동_가이드.md`
  - `2026-03-21_FE_프로모션_배치_연동_가이드.md`
- `archive/change-history`
  - `2026-03-17_FE_약관_및_동의화면_정비.md`
  - `2026-03-23_FE_MVP_UI_수정사항_정리.md`
  - `2026-03-24_로그인_최초1회_약관동의_이슈정리.md`
- `archive/onboarding`
  - `2026-03-16_공용_디버그_키스토어.md`

## 5. 운영 원칙

- 활성 문서는 운영, 유지보수, 인수인계에 직접 쓰는 문서만 유지한다.
- 특정 작업 회차에서만 쓰는 문서는 작업 종료 즉시 `archive/`로 이동한다.
- 현재 구현 설명은 `maintenance/2026-03-28_FE_디자인시스템_구현기준_및_문서정합성.md`를 우선 기준으로 본다.
- 회귀 QA 범위는 `maintenance/2026-03-19_FE_화면명세서_기반_전체_체크리스트.md`를 기준으로 본다.
- 루트 `docs/`에는 서비스 공통 문서만 남기고, FE 전용 문서는 `FE/docs/`에서 관리한다.
