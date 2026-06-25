package com.ssafy.e106.domain.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ssafy.e106.domain.auth.entity.User;
import com.ssafy.e106.domain.auth.enums.AuthProvider;
import com.ssafy.e106.domain.auth.repository.UserRepository;
import com.ssafy.e106.domain.notification.entity.Notification;
import com.ssafy.e106.domain.notification.enums.NotificationType;
import com.ssafy.e106.domain.notification.repository.FcmTokenRepository;
import com.ssafy.e106.domain.notification.repository.NotificationRepository;
import com.ssafy.e106.global.exception.BusinessException;
import com.ssafy.e106.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private FcmTokenRepository fcmTokenRepository;

  @Mock
  private NotificationRepository notificationRepository;

  @InjectMocks
  private NotificationService notificationService;

  @Test
  void deleteNotification_success() {
    Long userId = 1L;
    Long notificationId = 10L;
    User user = user(userId);
    Notification notification = sentNotification(notificationId, user, LocalDateTime.now());

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

    notificationService.deleteNotification(userId, notificationId);

    verify(notificationRepository).delete(notification);
  }

  @Test
  void deleteNotification_forbidden_whenOtherUsersNotification() {
    Long userId = 1L;
    Long notificationId = 10L;
    User user = user(userId);
    Notification notification = sentNotification(notificationId, user(2L), LocalDateTime.now());

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

    BusinessException exception = assertThrows(
        BusinessException.class,
        () -> notificationService.deleteNotification(userId, notificationId));

    assertEquals(ErrorCode.NOTIFICATION_FORBIDDEN, exception.getErrorCode());
    verify(notificationRepository, never()).delete(notification);
  }

  @Test
  void deleteNotification_notFound_whenNotificationDoesNotExist() {
    Long userId = 1L;
    Long notificationId = 10L;
    User user = user(userId);

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

    BusinessException exception = assertThrows(
        BusinessException.class,
        () -> notificationService.deleteNotification(userId, notificationId));

    assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, exception.getErrorCode());
  }

  @Test
  void deleteNotification_notFound_whenNotificationNotSentYet() {
    Long userId = 1L;
    Long notificationId = 10L;
    User user = user(userId);
    Notification notification = sentNotification(notificationId, user, null);

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

    BusinessException exception = assertThrows(
        BusinessException.class,
        () -> notificationService.deleteNotification(userId, notificationId));

    assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, exception.getErrorCode());
    verify(notificationRepository, never()).delete(notification);
  }

  @Test
  void deleteNotifications_success() {
    Long userId = 1L;
    User user = user(userId);

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    notificationService.deleteNotifications(userId);

    verify(notificationRepository).deleteAllByUser_UserIdAndSentAtIsNotNull(userId);
  }

  @Test
  void deleteNotifications_userNotFound() {
    Long userId = 1L;

    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    BusinessException exception = assertThrows(
        BusinessException.class,
        () -> notificationService.deleteNotifications(userId));

    assertEquals(ErrorCode.AUTH_USER_NOT_FOUND, exception.getErrorCode());
    verify(notificationRepository, never()).deleteAllByUser_UserIdAndSentAtIsNotNull(userId);
  }

  private User user(Long userId) {
    User user = User.builder()
        .provider(AuthProvider.GOOGLE)
        .providerUserId("provider-" + userId)
        .email("user" + userId + "@example.com")
        .displayName("user" + userId)
        .checkinAlertEnabled(true)
        .promoAlertEnabled(true)
        .optionalConsentAgreed(true)
        .connectedAt(LocalDateTime.now())
        .build();
    ReflectionTestUtils.setField(user, "userId", userId);
    return user;
  }

  private Notification sentNotification(Long notificationId, User user, LocalDateTime sentAt) {
    Notification notification = Notification.builder()
        .user(user)
        .type(NotificationType.PROMO)
        .referenceId(1L)
        .title("title")
        .body("body")
        .sentAt(sentAt)
        .build();
    ReflectionTestUtils.setField(notification, "notificationId", notificationId);
    return notification;
  }
}
