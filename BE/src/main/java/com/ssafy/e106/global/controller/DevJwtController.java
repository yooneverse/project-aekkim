package com.ssafy.e106.global.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.e106.global.common.ApiResponse;
import com.ssafy.e106.global.security.JwtProperties;

import lombok.RequiredArgsConstructor;

/**
 * 개발 환경에서만 사용 가능한 JWT 발급 엔드포인트.
 * - local 프로필에서만 Bean 등록됨
 * - prod에서는 이 컨트롤러 자체가 존재하지 않음 (404)
 */
@Profile("local")
@RestController
@RequiredArgsConstructor
public class DevJwtController {

  private final JwtEncoder jwtEncoder;
  private final JwtProperties jwtProperties;

  /**
   * 테스트용 Access Token 발급.
   * - sub: 토큰 주체 (기본값 "dev-user", 나중에 userId가 들어갈 자리)
   * - iss: 발급자 (application.yml의 jwt.issuer)
   * - iat/exp: 발급시각/만료시각
   * - jti: 토큰 고유 ID (중복 방지)
   */
  @PostMapping("/api/v1/dev/jwt")
  public ResponseEntity<ApiResponse<DevTokenResponse>> issueToken(
      @RequestParam(defaultValue = "1") String sub) {

    Instant now = Instant.now();
    long ttl = jwtProperties.getAccessTokenTtl();

    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(jwtProperties.getIssuer())
        .subject(sub)
        .issuedAt(now)
        .expiresAt(now.plusSeconds(ttl))
        .id(UUID.randomUUID().toString())
        .build();

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    String token = jwtEncoder.encode(
        JwtEncoderParameters.from(header, claims)).getTokenValue();

    return ResponseEntity.ok(
        ApiResponse.ok(new DevTokenResponse(token, ttl)));
  }

  public record DevTokenResponse(String accessToken, long expiresIn) {
  }
}
