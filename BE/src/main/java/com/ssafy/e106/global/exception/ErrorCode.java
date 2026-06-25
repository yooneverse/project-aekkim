package com.ssafy.e106.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

  // 400 Bad Request
  INVALID_INPUT("INVALID_INPUT", "잘못된 입력입니다.", HttpStatus.BAD_REQUEST),
  FINANCE_MEMBER_INVALID_REQUEST(
      "E4001",
      "빈 데이터이거나 형식에 맞지 않는 데이터입니다.",
      HttpStatus.BAD_REQUEST),
  FINANCE_MEMBER_INVALID_API_KEY(
      "E4004",
      "존재하지 않는 API KEY입니다.",
      HttpStatus.BAD_REQUEST),
  FINANCE_MEMBER_INVALID_BODY(
      "Q1001",
      "요청 본문의 형식이 잘못되었습니다. JSON 형식 또는 데이터 타입을 확인해 주세요.",
      HttpStatus.BAD_REQUEST),
  FINANCE_HEADER_INVALID("H1000", "HEADER 정보가 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
  FINANCE_API_NAME_INVALID("H1001", "API 이름이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
  FINANCE_TRANSMISSION_DATE_INVALID("H1002", "전송일자 형식이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
  FINANCE_TRANSMISSION_TIME_INVALID("H1003", "전송시각 형식이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
  FINANCE_INSTITUTION_CODE_INVALID("H1004", "기관코드가 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
  FINANCE_FINTECH_APP_NO_INVALID("H1005", "핀테크 앱 일련번호가 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
  FINANCE_API_SERVICE_CODE_INVALID("H1006", "API 서비스코드가 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
  FINANCE_API_KEY_INVALID("H1008", "API KEY가 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
  FINANCE_USER_KEY_INVALID("H1009", "USER KEY가 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
  FINANCE_TRANSACTION_UNIQUE_NO_INVALID(
      "H1010",
      "기관거래고유번호가 유효하지 않습니다.",
      HttpStatus.BAD_REQUEST),
  FINANCE_BANK_CODE_INVALID("A1001", "은행코드가 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
  FINANCE_PRODUCT_NOT_FOUND(
      "A1019",
      "없는 상품입니다. 은행별 상품 조회를 다시 확인해주세요.",
      HttpStatus.NOT_FOUND),
  FINANCE_ACCOUNT_NAME_INVALID("A1021", "상품명이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
  FINANCE_ACCOUNT_TYPE_UNIQUE_NO_INVALID(
      "A1023",
      "상품 고유번호가 유효하지 않습니다.",
      HttpStatus.BAD_REQUEST),
  FINANCE_ACCOUNT_DESCRIPTION_TOO_LONG("A1031", "상품설명 길이가 초과되었습니다.", HttpStatus.BAD_REQUEST),

  // 401 Unauthorized
  UNAUTHORIZED("UNAUTHORIZED", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
  AUTH_GOOGLE_TOKEN_INVALID(
      "AUTH_GOOGLE_TOKEN_INVALID",
      "구글 토큰이 유효하지 않습니다.",
      HttpStatus.UNAUTHORIZED),
  AUTH_KAKAO_TOKEN_INVALID(
      "AUTH_KAKAO_TOKEN_INVALID",
      "카카오 토큰이 유효하지 않습니다.",
      HttpStatus.UNAUTHORIZED),
  AUTH_REFRESH_TOKEN_INVALID(
      "AUTH_REFRESH_TOKEN_INVALID",
      "유효하지 않은 리프레시 토큰입니다.",
      HttpStatus.UNAUTHORIZED),
  AUTH_REFRESH_TOKEN_EXPIRED(
      "AUTH_REFRESH_TOKEN_EXPIRED",
      "리프레시 토큰이 만료되었습니다.",
      HttpStatus.UNAUTHORIZED),

  // 403 Forbidden
  FORBIDDEN("FORBIDDEN", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
  NOTIFICATION_FORBIDDEN(
      "NOTIFICATION_FORBIDDEN",
      "해당 알림에 접근할 수 없습니다.",
      HttpStatus.FORBIDDEN),

  // 404 Not Found
  AUTH_USER_NOT_FOUND(
      "AUTH_USER_NOT_FOUND",
      "사용자를 찾을 수 없습니다.",
      HttpStatus.NOT_FOUND),
  SERVICE_NOT_FOUND(
      "SERVICE_NOT_FOUND",
      "서비스를 찾을 수 없습니다.",
      HttpStatus.NOT_FOUND),
  SERVICE_PLAN_NOT_FOUND(
      "SERVICE_PLAN_NOT_FOUND",
      "요금제를 찾을 수 없습니다.",
      HttpStatus.NOT_FOUND),
  RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  SUBSCRIPTION_NOT_FOUND(
      "SUBSCRIPTION_NOT_FOUND",
      "구독 정보를 찾을 수 없습니다.",
      HttpStatus.NOT_FOUND),
  NOTIFICATION_NOT_FOUND(
      "NOTIFICATION_NOT_FOUND",
      "알림을 찾을 수 없습니다.",
      HttpStatus.NOT_FOUND),
  FCM_TOKEN_NOT_FOUND(
      "FCM_TOKEN_NOT_FOUND",
      "FCM 토큰을 찾을 수 없습니다.",
      HttpStatus.NOT_FOUND),
  PROMOTION_NOT_FOUND(
      "PROMOTION_NOT_FOUND",
      "프로모션을 찾을 수 없습니다.",
      HttpStatus.NOT_FOUND),
  FINANCE_MEMBER_NOT_FOUND(
      "E4003",
      "존재하지 않는 ID입니다.",
      HttpStatus.NOT_FOUND),

  // 409 Conflict
  AUTH_PROVIDER_CONFLICT(
      "AUTH_PROVIDER_CONFLICT",
      "이미 다른 소셜 제공자로 가입된 사용자입니다.",
      HttpStatus.CONFLICT),
  SUBSCRIPTION_ALREADY_EXISTS(
      "SUBSCRIPTION_ALREADY_EXISTS",
      "이미 등록된 구독입니다.",
      HttpStatus.CONFLICT),
  CHECKIN_ALREADY_SUBMITTED(
      "CHECKIN_ALREADY_SUBMITTED",
      "이미 해당 월 체크인을 제출했습니다.",
      HttpStatus.CONFLICT),
  FINANCE_MEMBER_DUPLICATE_ID(
      "E4002",
      "이미 존재하는 ID입니다.",
      HttpStatus.CONFLICT),
  FINANCE_TRANSACTION_UNIQUE_NO_DUPLICATED(
      "H1007",
      "기관거래고유번호가 중복된 값입니다.",
      HttpStatus.CONFLICT),

  // 500 Internal Server Error
  INTERNAL_ERROR("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  FCM_SEND_FAILED("FCM_SEND_FAILED", "푸시 알림 발송에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  FINANCE_MEMBER_ERROR(
      "Q1000",
      "그 이외에 에러 메시지",
      HttpStatus.BAD_GATEWAY);

  private final String code;
  private final String message;
  private final HttpStatus httpStatus;
}
