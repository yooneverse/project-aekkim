package com.ssafy.e106.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "google")
public class GoogleProperties {

  private String clientId;
}
