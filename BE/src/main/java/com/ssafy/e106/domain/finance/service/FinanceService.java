package com.ssafy.e106.domain.finance.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.e106.domain.auth.entity.User;
import com.ssafy.e106.domain.auth.repository.UserRepository;
import com.ssafy.e106.domain.finance.dto.request.FinanceCreditCardTransactionCreateRequest;
import com.ssafy.e106.domain.finance.dto.request.FinanceCreditCardTransactionListRequest;
import com.ssafy.e106.domain.finance.dto.request.FinanceDemandDepositAccountCreateRequest;
import com.ssafy.e106.domain.finance.dto.request.FinanceDemandDepositCreateRequest;
import com.ssafy.e106.domain.finance.dto.request.FinanceMemberCreateRequest;
import com.ssafy.e106.domain.finance.dto.request.FinanceMerchantCreateRequest;
import com.ssafy.e106.domain.finance.dto.response.FinanceCardCategoryListResponse;
import com.ssafy.e106.domain.finance.dto.response.FinanceCardIssuerListResponse;
import com.ssafy.e106.domain.finance.dto.response.FinanceCreditCardListResponse;
import com.ssafy.e106.domain.finance.dto.response.FinanceSignUpCreditCardListResponse;
import com.ssafy.e106.domain.finance.dto.response.FinanceCreditCardTransactionCreateResponse;
import com.ssafy.e106.domain.finance.dto.response.FinanceCreditCardTransactionListResponse;
import com.ssafy.e106.domain.finance.dto.response.FinanceDemandDepositAccountCreateResponse;
import com.ssafy.e106.domain.finance.dto.response.FinanceDemandDepositAccountListResponse;
import com.ssafy.e106.domain.finance.dto.response.FinanceDemandDepositCreateResponse;
import com.ssafy.e106.domain.finance.dto.response.FinanceDemandDepositListResponse;
import com.ssafy.e106.domain.finance.dto.response.FinanceMemberCreateResponse;
import com.ssafy.e106.domain.finance.dto.response.FinanceMerchantCreateResponse;
import com.ssafy.e106.global.exception.BusinessException;
import com.ssafy.e106.global.exception.ErrorCode;
import com.ssafy.e106.global.security.FinanceProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FinanceService {

  private static final DateTimeFormatter TRANSMISSION_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd");
  private static final DateTimeFormatter TRANSMISSION_TIME_FORMAT =
      DateTimeFormatter.ofPattern("HHmmss");
  private static final DateTimeFormatter UNIQUE_NO_DATE_TIME_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private final FinanceProperties financeProperties;
  private final ObjectMapper objectMapper;
  private final UserRepository userRepository;

  private RestClient financeRestClient() {
    return RestClient.builder()
        .baseUrl(financeProperties.getBaseUrl())
        .build();
  }

  @Transactional
  public FinanceMemberCreateResponse createMember(Long userId, FinanceMemberCreateRequest request) {
    return requestMember(userId, "/ssafy/api/v1/member", request);
  }

  @Transactional
  public FinanceMemberCreateResponse searchMember(Long userId, FinanceMemberCreateRequest request) {
    return requestMember(userId, "/ssafy/api/v1/member/search", request);
  }

  @Transactional(readOnly = true)
  public FinanceDemandDepositCreateResponse createDemandDeposit(
      Long userId,
      FinanceDemandDepositCreateRequest request) {
    requireUser(userId);

    if (request == null) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }

    try {
      FinanceDemandDepositApiResponse response = financeRestClient().post()
          .uri("/ssafy/api/v1/edu/demandDeposit/createDemandDeposit")
          .contentType(MediaType.APPLICATION_JSON)
          .body(new FinanceDemandDepositApiRequest(
              createFinanceHeader("createDemandDeposit", null),
              request.bankCode(),
              request.accountName(),
              request.accountDescription()))
          .retrieve()
          .body(FinanceDemandDepositApiResponse.class);

      validateDemandDepositResponse(response);

      return new FinanceDemandDepositCreateResponse(
          toCreateHeader(response.header()),
          toCreateRec(response.rec()));
    } catch (RestClientResponseException e) {
      throw mapFinanceException(e);
    } catch (RestClientException e) {
      throw new BusinessException(ErrorCode.FINANCE_MEMBER_ERROR);
    }
  }

  @Transactional(readOnly = true)
  public FinanceDemandDepositListResponse inquireDemandDepositList(Long userId) {
    requireUser(userId);

    try {
      FinanceDemandDepositListApiResponse response = financeRestClient().post()
          .uri("/ssafy/api/v1/edu/demandDeposit/inquireDemandDepositList")
          .contentType(MediaType.APPLICATION_JSON)
          .body(new FinanceHeaderOnlyRequest(createFinanceHeader("inquireDemandDepositList", null)))
          .retrieve()
          .body(FinanceDemandDepositListApiResponse.class);

      if (response == null || response.header() == null || response.rec() == null) {
        throw new BusinessException(ErrorCode.FINANCE_MEMBER_ERROR);
      }

      validateSuccessCode(response.header().responseCode(), response.header().responseMessage());

      return new FinanceDemandDepositListResponse(
          toDepositListHeader(response.header()),
          response.rec().stream().map(this::toDepositListRec).toList());
    } catch (RestClientResponseException e) {
      throw mapFinanceException(e);
    } catch (RestClientException e) {
      throw new BusinessException(ErrorCode.FINANCE_MEMBER_ERROR);
    }
  }

  @Transactional(readOnly = true)
  public FinanceDemandDepositAccountCreateResponse createDemandDepositAccount(
      Long userId,
      FinanceDemandDepositAccountCreateRequest request) {
    User user = requireUser(userId);

    if (request == null) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }

    if (user.getFinanceUserKey() == null || user.getFinanceUserKey().isBlank()) {
      throw new BusinessException(ErrorCode.FINANCE_USER_KEY_INVALID, "금융망 사용자 키가 연결되어 있지 않습니다.");
    }

    try {
      FinanceDemandDepositAccountCreateApiResponse response = financeRestClient().post()
          .uri("/ssafy/api/v1/edu/demandDeDemandDepositAccount")
          .contentType(MediaType.APPLICATION_JSON)
          .body(new FinanceDemandDepositAccountCreateApiRequest(
              createFinanceHeader("createDemandDepositAccount", user.getFinanceUserKey()),
              request.accountTypeUniqueNo()))
          .retrieve()
          .body(FinanceDemandDepositAccountCreateApiResponse.class);

      if (response == null || response.header() == null || response.rec() == null) {
        throw new BusinessException(ErrorCode.FINANCE_MEMBER_ERROR);
      }

      validateSuccessCode(response.header().responseCode(), response.header().responseMessage());

      return new FinanceDemandDepositAccountCreateResponse(
          toAccountCreateHeader(response.header()),
          new FinanceDemandDepositAccountCreateResponse.Rec(
              response.rec().bankCode(),
              response.rec().accountNo(),
              new FinanceDemandDepositAccountCreateResponse.Currency(
                  response.rec().currency().currency(),
                  response.rec().currency().currencyName())));
    } catch (RestClientResponseException e) {
      throw mapFinanceException(e);
    } catch (RestClientException e) {
      throw new BusinessException(ErrorCode.FINANCE_MEMBER_ERROR);
    }
  }

  @Transactional(readOnly = true)
  public FinanceDemandDepositAccountListResponse inquireDemandDepositAccountList(Long userId) {
    User user = requireUser(userId);

    if (user.getFinanceUserKey() == null || user.getFinanceUserKey().isBlank()) {
      throw new BusinessException(ErrorCode.FINANCE_USER_KEY_INVALID, "금융망 사용자 키가 연결되어 있지 않습니다.");
    }

    try {
      FinanceDemandDepositAccountListApiResponse response = financeRestClient().post()
          .uri("/ssafy/api/v1/edu/demandDeposit/inquireDemandDepositAccountList")
          .contentType(MediaType.APPLICATION_JSON)
          .body(new FinanceHeaderOnlyRequest(
              createFinanceHeader("inquireDemandDepositAccountList", user.getFinanceUserKey())))
          .retrieve()
          .body(FinanceDemandDepositAccountListApiResponse.class);

      if (response == null || response.header() == null || response.rec() == null) {
        throw new BusinessException(ErrorCode.FINANCE_MEMBER_ERROR);
      }

      validateSuccessCode(response.header().responseCode(), response.header().responseMessage());

      return new FinanceDemandDepositAccountListResponse(
          toAccountListHeader(response.header()),
          response.rec().stream().map(this::toAccountListRec).toList());
    } catch (RestClientResponseException e) {
      throw mapFinanceException(e);
    } catch (RestClientException e) {
      throw new BusinessException(ErrorCode.FINANCE_MEMBER_ERROR);
    }
  }

  @Transactional(readOnly = true)
  public FinanceCardCategoryListResponse inquireCardCategoryList(Long userId) {
    requireUser(userId);

    try {
      FinanceCardCategoryListApiResponse response = financeRestClient().post()
          .uri("/ssafy/api/v1/edu/creditCard/inquireCategoryList")
          .contentType(MediaType.APPLICATION_JSON)
          .body(new FinanceHeaderOnlyRequest(createFinanceHeader("inquireCategoryList", null)))
          .retrieve()
          .body(FinanceCardCategoryListApiResponse.class);

      if (response == null || response.header() == null || response.rec() == null) {
        throw new BusinessException(ErrorCode.FINANCE_MEMBER_ERROR);
      }

      validateSuccessCode(response.header().responseCode(), response.header().responseMessage());

      return new FinanceCardCategoryListResponse(
          toCardCategoryHeader(response.header()),
          response.rec().stream().map(this::toCardCategoryRec).toList());
    } catch (RestClientResponseException e) {
      throw mapFinanceException(e);
    } catch (RestClientException e) {
      throw new BusinessException(ErrorCode.FINANCE_MEMBER_ERROR);
    }
  }

  @Transactional(readOnly = true)
  public FinanceCardIssuerListResponse inquireCardIssuerList(Long userId) {
    requireUser(userId);

    try {
      FinanceCardIssuerListApiResponse response = financeRestClient().post()
          .uri("/ssafy/api/v1/edu/creditCard/inquireCardIssuerCodesList")
          .contentType(MediaType.APPLICATION_JSON)
          .body(new FinanceHeaderOnlyRequest(createFinanceHeader("inquireCardIssuerCodesList", null)))
          .retrieve()
          .body(FinanceCardIssuerListApiResponse.class);

      if (response == null || response.header() == null || response.rec() == null) {
        throw new BusinessException(ErrorCode.FINANCE_MEMBER_ERROR);
      }

      validateSuccessCode(response.header().responseCode(), response.header().responseMessage());

      return new FinanceCardIssuerListResponse(
          toCardIssuerHeader(response.header()),
          response.rec().stream().map(this::toCardIssuerRec).toList());
    } catch (RestClientResponseException e) {
      throw mapFinanceException(e);
    } catch (RestClientException e) {
      throw new BusinessException(ErrorCode.FINANCE_MEMBER_ERROR);
    }
  }

  @Transactional(readOnly = true)
  public FinanceCreditCardListResponse inquireCreditCardList(Long userId) {
    requireUser(userId);

    try {
      FinanceCreditCardListApiResponse response = financeRestClient().post()
          .uri("/ssafy/api/v1/edu/creditCard/inquireCreditCardList")
          .contentType(MediaType.APPLICATION_JSON)
          .body(new FinanceHeaderOnlyRequest(createFinanceHeader("inquireCreditCardList", null)))
          .retrieve()
          .body(FinanceCreditCardListApiResponse.class);

      if (response == null || response.header() == null || response.rec() == null) {
        throw new BusinessException(ErrorCode.FINANCE_MEMBER_ERROR);
      }

      validateSuccessCode(response.header().responseCode(), response.header().responseMessage());

      return new FinanceCreditCardListResponse(
          toCreditCardHeader(response.header()),
          response.rec().stream().map(this::toCreditCardRec).toList());
    } catch (RestClientResponseException e) {
      throw mapFinanceException(e);
    } catch (RestClientException e) {
      throw new BusinessException(ErrorCode.FINANCE_MEMBER_ERROR);
    }
  }

  @Transactional(readOnly = true)
  public FinanceSignUpCreditCardListResponse inquireSignUpCreditCardList(Long userId) {
    User user = requireUser(userId);

    if (user.getFinanceUserKey() == null || user.getFinanceUserKey().isBlank()) {
      throw new BusinessException(ErrorCode.FINANCE_USER_KEY_INVALID, "금융망 사용자 키가 연결되어 있지 않습니다.");
    }

    try {
      FinanceSignUpCreditCardListApiResponse response = financeRestClient().post()
          .uri("/ssafy/api/v1/edu/creditCard/inquireSignUpCreditCardList")
          .contentType(MediaType.APPLICATION_JSON)
          .body(new FinanceHeaderOnlyRequest(
              createFinanceHeader("inquireSignUpCreditCardList", user.getFinanceUserKey())))
          .retrieve()
          .body(FinanceSignUpCreditCardListApiResponse.class);

      if (response == null || response.header() == null || response.rec() == null) {
        throw new BusinessException(ErrorCode.FINANCE_MEMBER_ERROR);
      }

      validateSuccessCode(response.header().responseCode(), response.header().responseMessage());

      return new FinanceSignUpCreditCardListResponse(
          toSignUpCreditCardHeader(response.header()),
          response.rec().stream().map(this::toSignUpCreditCardRec).toList());
    } catch (RestClientResponseException e) {
      throw mapFinanceException(e);
    } catch (RestClientException e) {
      throw new BusinessException(ErrorCode.FINANCE_MEMBER_ERROR);
    }
  }

  @Transactional(readOnly = true)
  public FinanceMerchantCreateResponse createMerchant(Long userId, FinanceMerchantCreateRequest request) {
    requireUser(userId);

    if (request == null) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }

    try {
      FinanceMerchantCreateApiResponse response = financeRestClient().post()
          .uri("/ssafy/api/v1/edu/creditCard/createMerchant")
          .contentType(MediaType.APPLICATION_JSON)
          .body(new FinanceMerchantCreateApiRequest(
              createFinanceHeader("createMerchant", null),
              request.categoryId(),
              request.merchantName()))
          .retrieve()
          .body(FinanceMerchantCreateApiResponse.class);

      if (response == null || response.header() == null || response.rec() == null) {
        throw new BusinessException(ErrorCode.FINANCE_MEMBER_ERROR);
      }

      validateSuccessCode(response.header().responseCode(), response.header().responseMessage());

      return new FinanceMerchantCreateResponse(
          toMerchantHeader(response.header()),
          response.rec().stream().map(this::toMerchantRec).toList());
    } catch (RestClientResponseException e) {
      throw mapFinanceException(e);
    } catch (RestClientException e) {
      throw new BusinessException(ErrorCode.FINANCE_MEMBER_ERROR);
    }
  }

  @Transactional(readOnly = true)
  public FinanceCreditCardTransactionCreateResponse createCreditCardTransaction(
      Long userId,
      FinanceCreditCardTransactionCreateRequest request) {
    User user = requireUser(userId);

    if (request == null) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }

    if (user.getFinanceUserKey() == null || user.getFinanceUserKey().isBlank()) {
      throw new BusinessException(ErrorCode.FINANCE_USER_KEY_INVALID, "금융망 사용자 키가 연결되어 있지 않습니다.");
    }

    try {
      FinanceCreditCardTransactionCreateApiResponse response = financeRestClient().post()
          .uri("/ssafy/api/v1/edu/creditCard/createCreditCardTransaction")
          .contentType(MediaType.APPLICATION_JSON)
          .body(new FinanceCreditCardTransactionCreateApiRequest(
              createFinanceHeader("createCreditCardTransaction", user.getFinanceUserKey()),
              request.cardNo(),
              request.cvc(),
              request.merchantId(),
              request.paymentBalance()))
          .retrieve()
          .body(FinanceCreditCardTransactionCreateApiResponse.class);

      if (response == null || response.header() == null || response.rec() == null) {
        throw new BusinessException(ErrorCode.FINANCE_MEMBER_ERROR);
      }

      validateSuccessCode(response.header().responseCode(), response.header().responseMessage());

      return new FinanceCreditCardTransactionCreateResponse(
          toCreditCardTransactionHeader(response.header()),
          toCreditCardTransactionRec(response.rec()));
    } catch (RestClientResponseException e) {
      throw mapFinanceException(e);
    } catch (RestClientException e) {
      throw new BusinessException(ErrorCode.FINANCE_MEMBER_ERROR);
    }
  }

  @Transactional(readOnly = true)
  public FinanceCreditCardTransactionListResponse inquireCreditCardTransactionList(
      Long userId,
      FinanceCreditCardTransactionListRequest request) {
    User user = requireUser(userId);

    if (request == null) {
      throw new BusinessException(ErrorCode.INVALID_INPUT);
    }

    if (user.getFinanceUserKey() == null || user.getFinanceUserKey().isBlank()) {
      throw new BusinessException(ErrorCode.FINANCE_USER_KEY_INVALID, "금융망 사용자 키가 연결되어 있지 않습니다.");
    }

    try {
      FinanceCreditCardTransactionListApiResponse response = financeRestClient().post()
          .uri("/ssafy/api/v1/edu/creditCard/inquireCreditCardTransactionList")
          .contentType(MediaType.APPLICATION_JSON)
          .body(new FinanceCreditCardTransactionListApiRequest(
              createFinanceHeader("inquireCreditCardTransactionList", user.getFinanceUserKey()),
              request.cardNo(),
              request.cvc(),
              request.startDate(),
              request.endDate()))
          .retrieve()
          .body(FinanceCreditCardTransactionListApiResponse.class);

      if (response == null || response.header() == null || response.rec() == null) {
        throw new BusinessException(ErrorCode.FINANCE_MEMBER_ERROR);
      }

      validateSuccessCode(response.header().responseCode(), response.header().responseMessage());

      return new FinanceCreditCardTransactionListResponse(
          toCreditCardTransactionListHeader(response.header()),
          toCreditCardTransactionListRec(response.rec()));
    } catch (RestClientResponseException e) {
      throw mapFinanceException(e);
    } catch (RestClientException e) {
      throw new BusinessException(ErrorCode.FINANCE_MEMBER_ERROR);
    }
  }

  private FinanceMemberCreateResponse requestMember(
      Long userId,
      String uri,
      FinanceMemberCreateRequest request) {
    if (request == null) {
      throw new BusinessException(ErrorCode.FINANCE_MEMBER_INVALID_REQUEST);
    }

    User user = requireUser(userId);

    try {
      FinanceMemberApiResponse response = financeRestClient().post()
          .uri(uri)
          .contentType(MediaType.APPLICATION_JSON)
          .body(new FinanceMemberApiRequest(financeProperties.getApiKey(), request.userId()))
          .retrieve()
          .body(FinanceMemberApiResponse.class);

      if (response == null) {
        throw new BusinessException(ErrorCode.FINANCE_MEMBER_ERROR);
      }

      user.updateFinanceUserKey(response.userKey());

      return new FinanceMemberCreateResponse(
          response.userId(),
          response.userName(),
          response.institutionCode(),
          response.userKey(),
          response.created(),
          response.modified());
    } catch (RestClientResponseException e) {
      throw mapFinanceException(e);
    } catch (RestClientException e) {
      throw new BusinessException(ErrorCode.FINANCE_MEMBER_ERROR);
    }
  }

  private User requireUser(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));
  }

  private void validateDemandDepositResponse(FinanceDemandDepositApiResponse response) {
    if (response == null || response.header() == null || response.rec() == null) {
      throw new BusinessException(ErrorCode.FINANCE_MEMBER_ERROR);
    }

    validateSuccessCode(response.header().responseCode(), response.header().responseMessage());
  }

  private void validateSuccessCode(String code, String message) {
    if (!"H0000".equals(code)) {
      throw new BusinessException(mapFinanceErrorCode(code), message);
    }
  }

  private FinanceDemandDepositCreateResponse.Header toCreateHeader(FinanceDemandDepositApiHeader header) {
    return new FinanceDemandDepositCreateResponse.Header(
        header.responseCode(),
        header.responseMessage(),
        header.apiName(),
        header.transmissionDate(),
        header.transmissionTime(),
        header.institutionCode(),
        header.apiKey(),
        header.apiServiceCode(),
        header.institutionTransactionUniqueNo());
  }

  private FinanceDemandDepositListResponse.Header toDepositListHeader(FinanceDemandDepositApiHeader header) {
    return new FinanceDemandDepositListResponse.Header(
        header.responseCode(),
        header.responseMessage(),
        header.apiName(),
        header.transmissionDate(),
        header.transmissionTime(),
        header.institutionCode(),
        header.apiKey(),
        header.apiServiceCode(),
        header.institutionTransactionUniqueNo());
  }

  private FinanceDemandDepositAccountCreateResponse.Header toAccountCreateHeader(
      FinanceDemandDepositApiHeader header) {
    return new FinanceDemandDepositAccountCreateResponse.Header(
        header.responseCode(),
        header.responseMessage(),
        header.apiName(),
        header.transmissionDate(),
        header.transmissionTime(),
        header.institutionCode(),
        header.apiKey(),
        header.apiServiceCode(),
        header.institutionTransactionUniqueNo());
  }

  private FinanceDemandDepositAccountListResponse.Header toAccountListHeader(
      FinanceDemandDepositApiHeader header) {
    return new FinanceDemandDepositAccountListResponse.Header(
        header.responseCode(),
        header.responseMessage(),
        header.apiName(),
        header.transmissionDate(),
        header.transmissionTime(),
        header.institutionCode(),
        header.apiKey(),
        header.apiServiceCode(),
        header.institutionTransactionUniqueNo());
  }

  private FinanceCardCategoryListResponse.Header toCardCategoryHeader(FinanceDemandDepositApiHeader header) {
    return new FinanceCardCategoryListResponse.Header(
        header.responseCode(),
        header.responseMessage(),
        header.apiName(),
        header.transmissionDate(),
        header.transmissionTime(),
        header.institutionCode(),
        header.apiKey(),
        header.apiServiceCode(),
        header.institutionTransactionUniqueNo());
  }

  private FinanceCardIssuerListResponse.Header toCardIssuerHeader(FinanceDemandDepositApiHeader header) {
    return new FinanceCardIssuerListResponse.Header(
        header.responseCode(),
        header.responseMessage(),
        header.apiName(),
        header.transmissionDate(),
        header.transmissionTime(),
        header.institutionCode(),
        header.apiKey(),
        header.apiServiceCode(),
        header.institutionTransactionUniqueNo());
  }

  private FinanceCreditCardListResponse.Header toCreditCardHeader(FinanceDemandDepositApiHeader header) {
    return new FinanceCreditCardListResponse.Header(
        header.responseCode(),
        header.responseMessage(),
        header.apiName(),
        header.transmissionDate(),
        header.transmissionTime(),
        header.institutionCode(),
        header.apiKey(),
        header.apiServiceCode(),
        header.institutionTransactionUniqueNo());
  }

  private FinanceCreditCardTransactionCreateResponse.Header toCreditCardTransactionHeader(
      FinanceDemandDepositApiHeader header) {
    return new FinanceCreditCardTransactionCreateResponse.Header(
        header.responseCode(),
        header.responseMessage(),
        header.apiName(),
        header.transmissionDate(),
        header.transmissionTime(),
        header.institutionCode(),
        header.apiKey(),
        header.apiServiceCode(),
        header.institutionTransactionUniqueNo());
  }

  private FinanceCreditCardTransactionListResponse.Header toCreditCardTransactionListHeader(
      FinanceDemandDepositApiHeader header) {
    return new FinanceCreditCardTransactionListResponse.Header(
        header.responseCode(),
        header.responseMessage(),
        header.apiName(),
        header.transmissionDate(),
        header.transmissionTime(),
        header.institutionCode(),
        header.apiKey(),
        header.apiServiceCode(),
        header.institutionTransactionUniqueNo());
  }

  private FinanceMerchantCreateResponse.Header toMerchantHeader(FinanceDemandDepositApiHeader header) {
    return new FinanceMerchantCreateResponse.Header(
        header.responseCode(),
        header.responseMessage(),
        header.apiName(),
        header.transmissionDate(),
        header.transmissionTime(),
        header.institutionCode(),
        header.apiKey(),
        header.apiServiceCode(),
        header.institutionTransactionUniqueNo());
  }

  private FinanceDemandDepositCreateResponse.Rec toCreateRec(FinanceDemandDepositApiRec rec) {
    return new FinanceDemandDepositCreateResponse.Rec(
        rec.accountTypeUniqueNo(),
        rec.bankCode(),
        rec.bankName(),
        rec.accountTypeCode(),
        rec.accountTypeName(),
        rec.accountName(),
        rec.accountDescription(),
        rec.accountType());
  }

  private FinanceDemandDepositListResponse.Rec toDepositListRec(FinanceDemandDepositApiRec rec) {
    return new FinanceDemandDepositListResponse.Rec(
        rec.accountTypeUniqueNo(),
        rec.bankCode(),
        rec.bankName(),
        rec.accountTypeCode(),
        rec.accountTypeName(),
        rec.accountName(),
        rec.accountDescription(),
        rec.accountType());
  }

  private FinanceDemandDepositAccountListResponse.Rec toAccountListRec(
      FinanceDemandDepositAccountListApiRec rec) {
    return new FinanceDemandDepositAccountListResponse.Rec(
        rec.bankCode(),
        rec.bankName(),
        rec.userName(),
        rec.accountNo(),
        rec.accountName(),
        rec.accountTypeCode(),
        rec.accountTypeName(),
        rec.accountCreatedDate(),
        rec.accountExpiryDate(),
        rec.dailyTransferLimit(),
        rec.oneTimeTransferLimit(),
        rec.accountBalance(),
        rec.lastTransactionDate(),
        rec.currency());
  }

  private FinanceCardCategoryListResponse.Rec toCardCategoryRec(FinanceCardCategoryApiRec rec) {
    return new FinanceCardCategoryListResponse.Rec(
        rec.categoryId(),
        rec.categoryName(),
        rec.categoryDescription());
  }

  private FinanceCardIssuerListResponse.Rec toCardIssuerRec(FinanceCardIssuerApiRec rec) {
    return new FinanceCardIssuerListResponse.Rec(
        rec.cardIssuerCode(),
        rec.cardIssuerName());
  }

  private FinanceCreditCardListResponse.Rec toCreditCardRec(FinanceCreditCardApiRec rec) {
    return new FinanceCreditCardListResponse.Rec(
        rec.cardUniqueNo(),
        rec.cardIssuerCode(),
        rec.cardIssuerName(),
        rec.cardName(),
        rec.cardTypeCode(),
        rec.cardTypeName(),
        rec.baselinePerformance(),
        rec.maxBenefitLimit(),
        rec.cardDescription(),
        rec.cardBenefitsInfo().stream()
            .map(benefit -> new FinanceCreditCardListResponse.CardBenefitsInfo(
                benefit.categoryId(),
                benefit.categoryName(),
                benefit.discountRate()))
            .toList());
  }

  private FinanceSignUpCreditCardListResponse.Header toSignUpCreditCardHeader(
      FinanceDemandDepositApiHeader header) {
    return new FinanceSignUpCreditCardListResponse.Header(
        header.responseCode(),
        header.responseMessage(),
        header.apiName(),
        header.transmissionDate(),
        header.transmissionTime(),
        header.institutionCode(),
        header.apiKey(),
        header.apiServiceCode(),
        header.institutionTransactionUniqueNo());
  }

  private FinanceSignUpCreditCardListResponse.Rec toSignUpCreditCardRec(
      FinanceSignUpCreditCardApiRec rec) {
    return new FinanceSignUpCreditCardListResponse.Rec(
        rec.cardNo(),
        rec.cvc(),
        rec.cardUniqueNo(),
        rec.cardIssuerCode(),
        rec.cardIssuerName(),
        rec.cardName(),
        rec.baselinePerformance(),
        rec.maxBenefitLimit(),
        rec.cardDescription(),
        rec.cardExpiryDate(),
        rec.withdrawalAccountNo(),
        rec.withdrawalDate());
  }

  private FinanceCreditCardTransactionCreateResponse.Rec toCreditCardTransactionRec(
      FinanceCreditCardTransactionApiRec rec) {
    return new FinanceCreditCardTransactionCreateResponse.Rec(
        rec.transactionUniqueNo(),
        rec.categoryId(),
        rec.categoryName(),
        rec.merchantId(),
        rec.merchantName(),
        rec.transactionDate(),
        rec.transactionTime(),
        rec.paymentBalance());
  }

  private FinanceCreditCardTransactionListResponse.Rec toCreditCardTransactionListRec(
      FinanceCreditCardTransactionListApiRec rec) {
    return new FinanceCreditCardTransactionListResponse.Rec(
        rec.cardIssuerCode(),
        rec.cardIssuerName(),
        rec.cardName(),
        rec.cardNo(),
        rec.estimatedBalance(),
        rec.transactionList().stream()
            .map(transaction -> new FinanceCreditCardTransactionListResponse.Transaction(
                transaction.transactionUniqueNo(),
                transaction.categoryId(),
                transaction.categoryName(),
                transaction.merchantId(),
                transaction.merchantName(),
                transaction.transactionDate(),
                transaction.transactionTime(),
                transaction.transactionBalance(),
                transaction.cardStatus(),
                transaction.billStatementsYn(),
                transaction.billStatementsStatus()))
            .toList());
  }

  private FinanceMerchantCreateResponse.Rec toMerchantRec(FinanceMerchantApiRec rec) {
    return new FinanceMerchantCreateResponse.Rec(
        rec.categoryId(),
        rec.categoryName(),
        rec.merchantId(),
        rec.merchantName());
  }

  private BusinessException mapFinanceException(RestClientResponseException e) {
    try {
      FinanceErrorEnvelope error = objectMapper.readValue(
          e.getResponseBodyAsByteArray(),
          FinanceErrorEnvelope.class);
      ErrorCode errorCode = mapFinanceErrorCode(error.code());
      String message = error.message() == null || error.message().isBlank()
          ? errorCode.getMessage()
          : error.message();
      return new BusinessException(errorCode, message);
    } catch (Exception ignored) {
      return new BusinessException(ErrorCode.FINANCE_MEMBER_ERROR);
    }
  }

  private ErrorCode mapFinanceErrorCode(String code) {
    return switch (code) {
      case "E4001" -> ErrorCode.FINANCE_MEMBER_INVALID_REQUEST;
      case "E4002" -> ErrorCode.FINANCE_MEMBER_DUPLICATE_ID;
      case "E4003" -> ErrorCode.FINANCE_MEMBER_NOT_FOUND;
      case "E4004" -> ErrorCode.FINANCE_MEMBER_INVALID_API_KEY;
      case "H1000" -> ErrorCode.FINANCE_HEADER_INVALID;
      case "H1001" -> ErrorCode.FINANCE_API_NAME_INVALID;
      case "H1002" -> ErrorCode.FINANCE_TRANSMISSION_DATE_INVALID;
      case "H1003" -> ErrorCode.FINANCE_TRANSMISSION_TIME_INVALID;
      case "H1004" -> ErrorCode.FINANCE_INSTITUTION_CODE_INVALID;
      case "H1005" -> ErrorCode.FINANCE_FINTECH_APP_NO_INVALID;
      case "H1006" -> ErrorCode.FINANCE_API_SERVICE_CODE_INVALID;
      case "H1007" -> ErrorCode.FINANCE_TRANSACTION_UNIQUE_NO_DUPLICATED;
      case "H1008" -> ErrorCode.FINANCE_API_KEY_INVALID;
      case "H1009" -> ErrorCode.FINANCE_USER_KEY_INVALID;
      case "H1010" -> ErrorCode.FINANCE_TRANSACTION_UNIQUE_NO_INVALID;
      case "A1001" -> ErrorCode.FINANCE_BANK_CODE_INVALID;
      case "A1019" -> ErrorCode.FINANCE_PRODUCT_NOT_FOUND;
      case "A1021" -> ErrorCode.FINANCE_ACCOUNT_NAME_INVALID;
      case "A1023" -> ErrorCode.FINANCE_ACCOUNT_TYPE_UNIQUE_NO_INVALID;
      case "A1031" -> ErrorCode.FINANCE_ACCOUNT_DESCRIPTION_TOO_LONG;
      case "Q1001" -> ErrorCode.FINANCE_MEMBER_INVALID_BODY;
      case "Q1000" -> ErrorCode.FINANCE_MEMBER_ERROR;
      default -> ErrorCode.FINANCE_MEMBER_ERROR;
    };
  }

  private FinanceApiHeader createFinanceHeader(String apiName, String userKey) {
    LocalDateTime now = LocalDateTime.now();
    return new FinanceApiHeader(
        apiName,
        now.format(TRANSMISSION_DATE_FORMAT),
        now.format(TRANSMISSION_TIME_FORMAT),
        financeProperties.getInstitutionCode(),
        financeProperties.getFintechAppNo(),
        apiName,
        generateInstitutionTransactionUniqueNo(now),
        financeProperties.getApiKey(),
        userKey);
  }

  private String generateInstitutionTransactionUniqueNo(LocalDateTime now) {
    return now.format(UNIQUE_NO_DATE_TIME_FORMAT)
        + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
  }

  private record FinanceMemberApiRequest(
      String apiKey,
      String userId) {
  }

  private record FinanceMemberApiResponse(
      String userId,
      String userName,
      String institutionCode,
      String userKey,
      String created,
      String modified) {
  }

  private record FinanceErrorEnvelope(
      @JsonProperty("responseCode")
      String responseCode,
      @JsonProperty("responseMessage")
      String responseMessage,
      @JsonProperty("Header")
      FinanceDemandDepositApiHeader header) {

    private String code() {
      if (header != null && header.responseCode() != null && !header.responseCode().isBlank()) {
        return header.responseCode();
      }
      return responseCode;
    }

    private String message() {
      if (header != null && header.responseMessage() != null && !header.responseMessage().isBlank()) {
        return header.responseMessage();
      }
      return responseMessage;
    }
  }

  private record FinanceApiHeader(
      String apiName,
      String transmissionDate,
      String transmissionTime,
      String institutionCode,
      String fintechAppNo,
      String apiServiceCode,
      String institutionTransactionUniqueNo,
      String apiKey,
      String userKey) {
  }

  private record FinanceHeaderOnlyRequest(
      @JsonProperty("Header")
      FinanceApiHeader header) {
  }

  private record FinanceDemandDepositApiRequest(
      @JsonProperty("Header")
      FinanceApiHeader header,
      String bankCode,
      String accountName,
      String accountDescription) {
  }

  private record FinanceDemandDepositAccountCreateApiRequest(
      @JsonProperty("Header")
      FinanceApiHeader header,
      String accountTypeUniqueNo) {
  }

  private record FinanceCreditCardTransactionCreateApiRequest(
      @JsonProperty("Header")
      FinanceApiHeader header,
      String cardNo,
      String cvc,
      String merchantId,
      String paymentBalance) {
  }

  private record FinanceCreditCardTransactionListApiRequest(
      @JsonProperty("Header")
      FinanceApiHeader header,
      String cardNo,
      String cvc,
      String startDate,
      String endDate) {
  }

  private record FinanceMerchantCreateApiRequest(
      @JsonProperty("Header")
      FinanceApiHeader header,
      String categoryId,
      String merchantName) {
  }

  private record FinanceDemandDepositApiResponse(
      @JsonProperty("Header")
      FinanceDemandDepositApiHeader header,
      @JsonProperty("REC")
      FinanceDemandDepositApiRec rec) {
  }

  private record FinanceDemandDepositListApiResponse(
      @JsonProperty("Header")
      FinanceDemandDepositApiHeader header,
      @JsonProperty("REC")
      List<FinanceDemandDepositApiRec> rec) {
  }

  private record FinanceDemandDepositAccountCreateApiResponse(
      @JsonProperty("Header")
      FinanceDemandDepositApiHeader header,
      @JsonProperty("REC")
      FinanceDemandDepositAccountCreateApiRec rec) {
  }

  private record FinanceDemandDepositAccountListApiResponse(
      @JsonProperty("Header")
      FinanceDemandDepositApiHeader header,
      @JsonProperty("REC")
      List<FinanceDemandDepositAccountListApiRec> rec) {
  }

  private record FinanceCardCategoryListApiResponse(
      @JsonProperty("Header")
      FinanceDemandDepositApiHeader header,
      @JsonProperty("REC")
      List<FinanceCardCategoryApiRec> rec) {
  }

  private record FinanceCardIssuerListApiResponse(
      @JsonProperty("Header")
      FinanceDemandDepositApiHeader header,
      @JsonProperty("REC")
      List<FinanceCardIssuerApiRec> rec) {
  }

  private record FinanceCreditCardListApiResponse(
      @JsonProperty("Header")
      FinanceDemandDepositApiHeader header,
      @JsonProperty("REC")
      List<FinanceCreditCardApiRec> rec) {
  }

  private record FinanceSignUpCreditCardListApiResponse(
      @JsonProperty("Header")
      FinanceDemandDepositApiHeader header,
      @JsonProperty("REC")
      List<FinanceSignUpCreditCardApiRec> rec) {
  }

  private record FinanceCreditCardTransactionCreateApiResponse(
      @JsonProperty("Header")
      FinanceDemandDepositApiHeader header,
      @JsonProperty("REC")
      FinanceCreditCardTransactionApiRec rec) {
  }

  private record FinanceCreditCardTransactionListApiResponse(
      @JsonProperty("Header")
      FinanceDemandDepositApiHeader header,
      @JsonProperty("REC")
      FinanceCreditCardTransactionListApiRec rec) {
  }

  private record FinanceMerchantCreateApiResponse(
      @JsonProperty("Header")
      FinanceDemandDepositApiHeader header,
      @JsonProperty("REC")
      List<FinanceMerchantApiRec> rec) {
  }

  private record FinanceDemandDepositApiHeader(
      String responseCode,
      String responseMessage,
      String apiName,
      String transmissionDate,
      String transmissionTime,
      String institutionCode,
      String apiKey,
      String apiServiceCode,
      String institutionTransactionUniqueNo) {
  }

  private record FinanceDemandDepositApiRec(
      String accountTypeUniqueNo,
      String bankCode,
      String bankName,
      String accountTypeCode,
      String accountTypeName,
      String accountName,
      String accountDescription,
      String accountType) {
  }

  private record FinanceDemandDepositAccountCreateApiRec(
      String bankCode,
      String accountNo,
      FinanceCurrency currency) {
  }

  private record FinanceDemandDepositAccountListApiRec(
      String bankCode,
      String bankName,
      String userName,
      String accountNo,
      String accountName,
      String accountTypeCode,
      String accountTypeName,
      String accountCreatedDate,
      String accountExpiryDate,
      String dailyTransferLimit,
      String oneTimeTransferLimit,
      String accountBalance,
      String lastTransactionDate,
      String currency) {
  }

  private record FinanceCardCategoryApiRec(
      String categoryId,
      String categoryName,
      String categoryDescription) {
  }

  private record FinanceCardIssuerApiRec(
      String cardIssuerCode,
      String cardIssuerName) {
  }

  private record FinanceCreditCardApiRec(
      String cardUniqueNo,
      String cardIssuerCode,
      String cardIssuerName,
      String cardName,
      String cardTypeCode,
      String cardTypeName,
      String baselinePerformance,
      String maxBenefitLimit,
      String cardDescription,
      List<FinanceCreditCardBenefitApiRec> cardBenefitsInfo) {
  }

  private record FinanceSignUpCreditCardApiRec(
      String cardNo,
      String cvc,
      String cardUniqueNo,
      String cardIssuerCode,
      String cardIssuerName,
      String cardName,
      String baselinePerformance,
      String maxBenefitLimit,
      String cardDescription,
      String cardExpiryDate,
      String withdrawalAccountNo,
      String withdrawalDate) {
  }

  private record FinanceCreditCardBenefitApiRec(
      String categoryId,
      String categoryName,
      String discountRate) {
  }

  private record FinanceCreditCardTransactionApiRec(
      String transactionUniqueNo,
      String categoryId,
      String categoryName,
      String merchantId,
      String merchantName,
      String transactionDate,
      String transactionTime,
      String paymentBalance) {
  }

  private record FinanceCreditCardTransactionListApiRec(
      String cardIssuerCode,
      String cardIssuerName,
      String cardName,
      String cardNo,
      String estimatedBalance,
      List<FinanceCreditCardTransactionHistoryApiRec> transactionList) {
  }

  private record FinanceCreditCardTransactionHistoryApiRec(
      String transactionUniqueNo,
      String categoryId,
      String categoryName,
      String merchantId,
      String merchantName,
      String transactionDate,
      String transactionTime,
      String transactionBalance,
      String cardStatus,
      String billStatementsYn,
      String billStatementsStatus) {
  }

  private record FinanceMerchantApiRec(
      String categoryId,
      String categoryName,
      String merchantId,
      String merchantName) {
  }

  private record FinanceCurrency(
      String currency,
      String currencyName) {
  }
}
