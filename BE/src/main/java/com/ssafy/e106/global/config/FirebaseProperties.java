package com.ssafy.e106.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "firebase")
public class FirebaseProperties {

  private String serviceAccountJsonBase64;
}
