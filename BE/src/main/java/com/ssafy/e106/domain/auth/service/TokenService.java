package com.ssafy.e106.domain.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.e106.domain.auth.entity.User;
import com.ssafy.e106.domain.auth.repository.UserRepository;
import com.ssafy.e106.global.exception.BusinessException;
import com.ssafy.e106.global.exception.ErrorCode;
import com.ssafy.e106.global.security.JwtProperties;
import com.ssafy.e106.global.security.JwtUserIdResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenService {

  private final JwtEncoder jwtEncoder;
  private final JwtDecoder jwtDecoder;
  private final JwtProperties jwtProperties;
  private final UserRepository userRepository;
  private final StringRedisTemplate stringRedisTemplate;

  @Transactional
  public TokenPair issueTokenPair(Long userId) {
    String subject = String.valueOf(userId);
    String accessToken = issueJwt(subject, jwtProperties.getAccessTokenTtl(), "access");
    String refreshToken = issueJwt(subject, jwtProperties.getRefreshTokenTtl(), "refresh");

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "사용자를 찾을 수 없습니다."));

    stringRedisTemplate.opsForValue().set(
        buildRefreshKey(user.getUserId()),
        refreshToken,
        Duration.ofSeconds(jwtProperties.getRefreshTokenTtl()));

    return new TokenPair(
        accessToken,
        refreshToken,
        "Bearer",
        jwtProperties.getAccessTokenTtl());
  }

  @Transactional
  public TokenPair refresh(String rawRefreshToken) {
    Jwt jwt = decodeRefreshToken(rawRefreshToken);
    if (!"refresh".equals(jwt.getClaimAsString("token_type"))) {
      throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
    }

    Long userId = JwtUserIdResolver.resolve(jwt);
    String refreshKey = buildRefreshKey(userId);
    String savedRefreshToken = stringRedisTemplate.opsForValue().get(refreshKey);

    if (savedRefreshToken == null) {
      throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED);
    }

    if (!savedRefreshToken.equals(rawRefreshToken)) {
      throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
    }

    stringRedisTemplate.delete(refreshKey);
    return issueTokenPair(userId);
  }

  @Transactional
  public void logout(Long userId) {
    stringRedisTemplate.delete(buildRefreshKey(userId));
  }

  private Jwt decodeRefreshToken(String token) {
    try {
      return jwtDecoder.decode(token);
    } catch (JwtException e) {
      throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
    }
  }

  private String issueJwt(String subject, long ttlSeconds, String tokenType) {
    Instant now = Instant.now();
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(jwtProperties.getIssuer())
        .subject(subject)
        .issuedAt(now)
        .expiresAt(now.plusSeconds(ttlSeconds))
        .id(UUID.randomUUID().toString())
        .claim("token_type", tokenType)
        .build();

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  private String buildRefreshKey(Long userId) {
    return "refresh:" + userId;
  }

  public record TokenPair(
      String accessToken,
      String refreshToken,
      String tokenType,
      long expiresIn) {
  }
}
