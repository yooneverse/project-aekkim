package com.ssafy.e106.domain.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.e106.domain.auth.entity.User;
import com.ssafy.e106.domain.auth.repository.UserRepository;
import com.ssafy.e106.domain.auth.service.TokenService;
import com.ssafy.e106.domain.notification.repository.FcmTokenRepository;
import com.ssafy.e106.domain.notification.repository.NotificationRepository;
import com.ssafy.e106.domain.subscription.repository.CheckinRecordRepository;
import com.ssafy.e106.domain.subscription.repository.SubscriptionRepository;
import com.ssafy.e106.domain.user.dto.request.UpdateNotificationSettingsRequest;
import com.ssafy.e106.domain.user.dto.request.UpdateOptionalConsentRequest;
import com.ssafy.e106.domain.user.dto.response.NotificationSettingsResponse;
import com.ssafy.e106.domain.user.dto.response.UserMeResponse;
import com.ssafy.e106.domain.user.dto.response.UserOptionalConsentResponse;
import com.ssafy.e106.global.exception.BusinessException;
import com.ssafy.e106.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final TokenService tokenService;
  private final CheckinRecordRepository checkinRecordRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final NotificationRepository notificationRepository;
  private final FcmTokenRepository fcmTokenRepository;

  @Transactional(readOnly = true)
  public UserMeResponse getMyInfo(Long userId) {
    return UserMeResponse.from(getUser(userId));
  }

  @Transactional
  public void deleteMyAccount(Long userId) {
    log.info("Deleting user account. userId={}", userId);
    User user = getUser(userId);

    checkinRecordRepository.deleteAllByUser_UserId(userId);
    subscriptionRepository.deleteAllByUserId(userId);
    notificationRepository.deleteAllByUser_UserId(userId);
    fcmTokenRepository.deleteAllByUser_UserId(userId);

    tokenService.logout(userId);
    userRepository.delete(user);
    log.info("Deleted user account. userId={}", userId);
  }

  @Transactional
  public NotificationSettingsResponse updateNotificationSettings(
      Long userId,
      UpdateNotificationSettingsRequest request) {
    User user = getUser(userId);
    user.updateAlertSetting(request.checkinAlertEnabled(), request.promoAlertEnabled());
    return NotificationSettingsResponse.from(user);
  }

  @Transactional
  public UserOptionalConsentResponse updateOptionalConsent(
      Long userId,
      UpdateOptionalConsentRequest request) {
    User user = getUser(userId);
    user.updateOptionalConsent(request.optionalConsentAgreed());
    return UserOptionalConsentResponse.from(user);
  }

  private User getUser(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));
  }
}
