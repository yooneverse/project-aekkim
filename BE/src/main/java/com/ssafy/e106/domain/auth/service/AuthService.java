package com.ssafy.e106.domain.auth.service;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.ssafy.e106.domain.auth.entity.User;
import com.ssafy.e106.domain.auth.enums.AuthProvider;
import com.ssafy.e106.global.exception.BusinessException;
import com.ssafy.e106.global.exception.ErrorCode;
import com.ssafy.e106.global.security.GoogleProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final OAuthService oAuthService;
  private final TokenService tokenService;
  private final GoogleProperties googleProperties;

  private final RestClient kakaoRestClient = RestClient.builder()
      .baseUrl("https://kapi.kakao.com")
      .build();

  private final RestClient googleRestClient = RestClient.builder()
      .baseUrl("https://www.googleapis.com")
      .build();

  @Transactional
  public LoginResult loginWithKakao(String kakaoAccessToken) {
    KakaoProfile profile = fetchKakaoProfile(kakaoAccessToken);

    OAuthService.UpsertOAuthUserResult upsertResult = oAuthService.upsertOAuthUser(
        AuthProvider.KAKAO,
        profile.providerUserId(),
        profile.email(),
        profile.displayName(),
        profile.profileImageUrl());

    return new LoginResult(
        tokenService.issueTokenPair(upsertResult.user().getUserId()),
        upsertResult.user(),
        upsertResult.isNewUser());
  }

  @Transactional
  public LoginResult loginWithGoogle(String googleIdToken) {
    GoogleProfile profile = fetchGoogleProfile(googleIdToken);

    OAuthService.UpsertOAuthUserResult upsertResult = oAuthService.upsertOAuthUser(
        AuthProvider.GOOGLE,
        profile.providerUserId(),
        profile.email(),
        profile.displayName(),
        profile.profileImageUrl());

    return new LoginResult(
        tokenService.issueTokenPair(upsertResult.user().getUserId()),
        upsertResult.user(),
        upsertResult.isNewUser());
  }

  @Transactional
  public TokenService.TokenPair refresh(String refreshToken) {
    return tokenService.refresh(refreshToken);
  }

  @Transactional
  public void logout(Long userId) {
    tokenService.logout(userId);
  }

  @SuppressWarnings("unchecked")
  private KakaoProfile fetchKakaoProfile(String kakaoAccessToken) {
    try {
      Map<String, Object> body = kakaoRestClient.get()
          .uri("/v2/user/me")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
          .retrieve()
          .body(Map.class);

      if (body == null || body.get("id") == null) {
        throw new BusinessException(ErrorCode.AUTH_KAKAO_TOKEN_INVALID);
      }

      String providerUserId = String.valueOf(body.get("id"));
      String email = null;
      String displayName = null;
      String profileImageUrl = null;

      Object kakaoAccountObj = body.get("kakao_account");
      if (kakaoAccountObj instanceof Map<?, ?> kakaoAccount) {
        Object emailObj = kakaoAccount.get("email");
        email = emailObj == null ? null : String.valueOf(emailObj);

        Object profileObj = kakaoAccount.get("profile");
        if (profileObj instanceof Map<?, ?> profileMap) {
          Object nicknameObj = profileMap.get("nickname");
          displayName = nicknameObj == null ? null : String.valueOf(nicknameObj);
          Object profileImageUrlObj = profileMap.get("profile_image_url");
          profileImageUrl = profileImageUrlObj == null ? null : String.valueOf(profileImageUrlObj);
        }
      }

      if (email == null || email.isBlank()) {
        throw new BusinessException(ErrorCode.AUTH_KAKAO_TOKEN_INVALID);
      }

      return new KakaoProfile(providerUserId, email, displayName, profileImageUrl);
    } catch (RestClientException e) {
      throw new BusinessException(ErrorCode.AUTH_KAKAO_TOKEN_INVALID);
    }
  }

  @SuppressWarnings("unchecked")
  private GoogleProfile fetchGoogleProfile(String googleIdToken) {
    try {
      Map<String, Object> body = googleRestClient.get()
          .uri(uriBuilder -> uriBuilder
              .path("/oauth2/v3/tokeninfo")
              .queryParam("id_token", googleIdToken)
              .build())
          .retrieve()
          .body(Map.class);

      if (body == null || body.get("sub") == null) {
        throw new BusinessException(ErrorCode.AUTH_GOOGLE_TOKEN_INVALID);
      }

      String audience = body.get("aud") == null ? null : String.valueOf(body.get("aud"));
      if (audience == null || !audience.equals(googleProperties.getClientId())) {
        throw new BusinessException(ErrorCode.AUTH_GOOGLE_TOKEN_INVALID);
      }

      String providerUserId = String.valueOf(body.get("sub"));
      String email = body.get("email") == null ? null : String.valueOf(body.get("email"));
      String displayName = body.get("name") == null ? null : String.valueOf(body.get("name"));
      String profileImageUrl = body.get("picture") == null ? null : String.valueOf(body.get("picture"));

      return new GoogleProfile(providerUserId, email, displayName, profileImageUrl);
    } catch (RestClientException e) {
      throw new BusinessException(ErrorCode.AUTH_GOOGLE_TOKEN_INVALID);
    }
  }

  public record LoginResult(TokenService.TokenPair tokenPair, User user, boolean isNewUser) {
  }

  private record KakaoProfile(
      String providerUserId,
      String email,
      String displayName,
      String profileImageUrl) {
  }

  private record GoogleProfile(
      String providerUserId,
      String email,
      String displayName,
      String profileImageUrl) {
  }
}
