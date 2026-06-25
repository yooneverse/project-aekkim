package com.ssafy.e106.domain.auth.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.e106.domain.auth.entity.User;
import com.ssafy.e106.domain.auth.enums.AuthProvider;
import com.ssafy.e106.domain.auth.repository.UserRepository;
import com.ssafy.e106.global.exception.BusinessException;
import com.ssafy.e106.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OAuthService {

  private final UserRepository userRepository;

  @Transactional
  public UpsertOAuthUserResult upsertOAuthUser(
      AuthProvider provider,
      String providerUserId,
      String email,
      String displayName,
      String profileImageUrl) {
    LocalDateTime now = LocalDateTime.now();
    String normalizedEmail = normalizeEmail(email);
    String resolvedDisplayName = resolveDisplayName(displayName);

    return userRepository
        .findByProviderAndProviderUserId(provider, providerUserId)
        .map(existingUser -> {
          existingUser.updateOAuthProfile(
              normalizedEmail,
              resolvedDisplayName,
              profileImageUrl,
              now);
          return new UpsertOAuthUserResult(existingUser, false);
        })
        .orElseGet(() -> {
          validateCrossProviderSignup(provider, normalizedEmail);

          User createdUser = userRepository.save(User.builder()
              .provider(provider)
              .providerUserId(providerUserId)
              .email(normalizedEmail)
              .displayName(resolvedDisplayName)
              .profileImageUrl(profileImageUrl)
              .checkinAlertEnabled(true)
              .promoAlertEnabled(true)
              .optionalConsentAgreed(false)
              .connectedAt(now)
              .lastLoginAt(now)
              .build());

          return new UpsertOAuthUserResult(createdUser, true);
        });
  }

  public record UpsertOAuthUserResult(User user, boolean isNewUser) {
  }

  private void validateCrossProviderSignup(AuthProvider provider, String email) {
    if (email == null) {
      return;
    }

    Optional<User> existingUser = userRepository.findByEmail(email);
    if (existingUser.isPresent() && existingUser.get().getProvider() != provider) {
      throw new BusinessException(ErrorCode.AUTH_PROVIDER_CONFLICT);
    }
  }

  private String normalizeEmail(String email) {
    if (email == null || email.isBlank()) {
      return null;
    }
    return email.trim().toLowerCase();
  }

  private String resolveDisplayName(String displayName) {
    if (displayName == null || displayName.isBlank()) {
      return "사용자";
    }
    return displayName.trim();
  }
}
