package com.ssafy.e106.domain.admin.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.ssafy.e106.domain.admin.dto.response.AdminKakaoUserResetResponse;
import com.ssafy.e106.domain.auth.entity.User;
import com.ssafy.e106.domain.auth.enums.AuthProvider;
import com.ssafy.e106.domain.auth.repository.UserRepository;
import com.ssafy.e106.domain.user.service.UserService;
import com.ssafy.e106.global.exception.BusinessException;
import com.ssafy.e106.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminKakaoAccountResetService {

  private final UserRepository userRepository;
  private final UserService userService;

  @Value("${kakao.admin-key:}")
  private String kakaoAdminKey;

  private final RestClient kakaoRestClient = RestClient.builder()
      .baseUrl("https://kapi.kakao.com")
      .build();

  @Transactional
  public AdminKakaoUserResetResponse resetKakaoUser(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));

    if (user.getProvider() != AuthProvider.KAKAO) {
      throw new BusinessException(ErrorCode.INVALID_INPUT, "Kakao user only");
    }

    if (kakaoAdminKey == null || kakaoAdminKey.isBlank()) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Kakao admin key is not configured");
    }

    Long kakaoUserId = parseKakaoUserId(user.getProviderUserId());
    Long unlinkedKakaoUserId = unlinkKakaoUser(kakaoUserId);

    userService.deleteMyAccount(userId);

    return new AdminKakaoUserResetResponse(
        userId,
        user.getProvider().name(),
        user.getProviderUserId(),
        unlinkedKakaoUserId,
        true);
  }

  private Long unlinkKakaoUser(Long kakaoUserId) {
    MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
    requestBody.add("target_id_type", "user_id");
    requestBody.add("target_id", String.valueOf(kakaoUserId));

    try {
      KakaoUnlinkResponse response = kakaoRestClient.post()
          .uri("/v1/user/unlink")
          .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoAdminKey)
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(requestBody)
          .retrieve()
          .body(KakaoUnlinkResponse.class);

      if (response == null || response.id() == null) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Kakao unlink returned empty response");
      }

      log.info("Admin Kakao unlink succeeded. kakaoUserId={}", response.id());
      return response.id();
    } catch (RestClientException exception) {
      log.error("Admin Kakao unlink failed. kakaoUserId={}", kakaoUserId, exception);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Kakao unlink failed");
    }
  }

  private Long parseKakaoUserId(String providerUserId) {
    try {
      return Long.valueOf(providerUserId);
    } catch (NumberFormatException exception) {
      throw new BusinessException(ErrorCode.INVALID_INPUT, "Stored Kakao provider user id is invalid");
    }
  }

  private record KakaoUnlinkResponse(Long id) {
  }
}
