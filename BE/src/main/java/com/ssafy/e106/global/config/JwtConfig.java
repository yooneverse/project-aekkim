package com.ssafy.e106.global.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.ssafy.e106.global.security.FinanceProperties;
import com.ssafy.e106.global.security.GoogleProperties;
import com.ssafy.e106.global.security.JwtProperties;

/**
 * JWT 인코더/디코더 Bean 등록.
 * - JwtDecoder: 요청의 Bearer 토큰을 검증 (Spring Security가 자동 호출)
 * - JwtEncoder: 토큰 생성 시 사용 (dev 발급 엔드포인트 등)
 */
@Configuration
@EnableConfigurationProperties({JwtProperties.class, GoogleProperties.class, FinanceProperties.class})
public class JwtConfig {

  /**
   * application.yml의 jwt.secret 값으로 SecretKey 생성.
   * HS256은 최소 256비트(32바이트) 키가 필요함.
   */
  private SecretKey secretKey(JwtProperties props) {
    byte[] keyBytes = props.getSecret().getBytes(StandardCharsets.UTF_8);
    return new SecretKeySpec(keyBytes, "HmacSHA256");
  }

  /**
   * 들어온 토큰을 검증하는 디코더.
   * Spring Security가 Bearer 토큰을 만나면 이 Bean을 자동으로 호출해서
   * 서명 검증 + 만료 체크를 수행함.
   */
  @Bean
  public JwtDecoder jwtDecoder(JwtProperties props) {
    return NimbusJwtDecoder.withSecretKey(secretKey(props))
        .macAlgorithm(MacAlgorithm.HS256)
        .build();
  }

  /**
   * 토큰을 생성하는 인코더.
   * dev 발급 엔드포인트에서 JwtEncoder.encode(params)로 토큰을 만들 때 사용.
   */
  @Bean
  public JwtEncoder jwtEncoder(JwtProperties props) {
    OctetSequenceKey jwk = new OctetSequenceKey.Builder(secretKey(props))
        .build();
    return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
  }
}
