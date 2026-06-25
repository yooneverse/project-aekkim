package com.ssafy.e106.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "finance")
public class FinanceProperties {

  private String apiKey;
  private String baseUrl;
  private String institutionCode;
  private String fintechAppNo;
}
