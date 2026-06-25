package com.ssafy.e106.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

  private String secret;
  private String issuer;
  private long accessTokenTtl;
  private long refreshTokenTtl = 1_209_600L;
}
