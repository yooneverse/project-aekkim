package com.ssafy.e106.global.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

@Configuration
@Profile({"local", "dev", "prod"})
@EnableConfigurationProperties(FirebaseProperties.class)
public class FirebaseConfig {

  @Bean
  public FirebaseApp firebaseApp(FirebaseProperties firebaseProperties) throws IOException {
    if (!FirebaseApp.getApps().isEmpty()) {
      return FirebaseApp.getInstance();
    }

    String serviceAccountJsonBase64 = firebaseProperties.getServiceAccountJsonBase64();
    if (serviceAccountJsonBase64 == null || serviceAccountJsonBase64.isBlank()) {
      throw new IllegalStateException("FIREBASE_SERVICE_ACCOUNT_JSON_BASE64 is required.");
    }

    byte[] serviceAccountJson = Base64.getDecoder().decode(serviceAccountJsonBase64);

    try (InputStream inputStream = new ByteArrayInputStream(serviceAccountJson)) {
      FirebaseOptions options = FirebaseOptions.builder()
          .setCredentials(GoogleCredentials.fromStream(inputStream))
          .build();
      return FirebaseApp.initializeApp(options);
    }
  }

  @Bean
  public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
    return FirebaseMessaging.getInstance(firebaseApp);
  }
}
