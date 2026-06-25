package com.ssafy.e106.domain.notification.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.e106.domain.auth.entity.User;
import com.ssafy.e106.domain.auth.repository.UserRepository;
import com.ssafy.e106.domain.notification.dto.request.UpsertFcmTokenRequest;
import com.ssafy.e106.domain.notification.dto.response.FcmTokenResponse;
import com.ssafy.e106.domain.notification.dto.response.NotificationListItemResponse;
import com.ssafy.e106.domain.notification.dto.response.NotificationListResponse;
import com.ssafy.e106.domain.notification.dto.response.NotificationReadResponse;
import com.ssafy.e106.domain.notification.entity.FcmToken;
import com.ssafy.e106.domain.notification.entity.Notification;
import com.ssafy.e106.domain.notification.enums.NotificationType;
import com.ssafy.e106.domain.notification.repository.FcmTokenRepository;
import com.ssafy.e106.domain.notification.repository.NotificationRepository;
import com.ssafy.e106.global.exception.BusinessException;
import com.ssafy.e106.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private final UserRepository userRepository;
  private final FcmTokenRepository fcmTokenRepository;
  private final NotificationRepository notificationRepository;

  @Transactional
  public FcmTokenResponse upsertFcmToken(Long userId, UpsertFcmTokenRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));

    FcmToken fcmToken = fcmTokenRepository.findByFcmToken(request.fcmToken())
        .map(existingToken -> {
          existingToken.updateUser(user);
          return existingToken;
        })
        .orElseGet(() -> FcmToken.builder()
            .user(user)
            .fcmToken(request.fcmToken())
            .build());

    return FcmTokenResponse.from(fcmTokenRepository.save(fcmToken));
  }

  @Transactional(readOnly = true)
  public NotificationListResponse getNotifications(Long userId, boolean unreadOnly, int size) {
    if (size < 1 || size > 100) {
      throw new BusinessException(ErrorCode.INVALID_INPUT, "size는 1 이상 100 이하여야 합니다.");
    }

    userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));

    Pageable pageable = PageRequest.of(0, size);
    List<Notification> notifications = unreadOnly
        ? notificationRepository.findAllByUser_UserIdAndReadAtIsNullAndSentAtIsNotNullOrderBySentAtDescNotificationIdDesc(
            userId,
            pageable)
        : notificationRepository.findAllByUser_UserIdAndSentAtIsNotNullOrderBySentAtDescNotificationIdDesc(
            userId,
            pageable);

    List<NotificationListItemResponse> responses = notifications.stream()
        .map(NotificationListItemResponse::from)
        .toList();

    return new NotificationListResponse(responses);
  }

  @Transactional
  public NotificationReadResponse markAsRead(Long userId, Long notificationId) {
    userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));

    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

    if (!notification.getUser().getUserId().equals(userId)) {
      throw new BusinessException(ErrorCode.NOTIFICATION_FORBIDDEN);
    }

    notification.markAsRead(LocalDateTime.now());
    return NotificationReadResponse.from(notification);
  }

  @Transactional
  public void deleteNotification(Long userId, Long notificationId) {
    userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));

    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

    if (!notification.getUser().getUserId().equals(userId)) {
      throw new BusinessException(ErrorCode.NOTIFICATION_FORBIDDEN);
    }

    if (notification.getSentAt() == null) {
      throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    notificationRepository.delete(notification);
  }

  @Transactional
  public void deleteNotifications(Long userId) {
    userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));

    notificationRepository.deleteAllByUser_UserIdAndSentAtIsNotNull(userId);
  }

  @Transactional
  public Notification createNotification(
      Long userId,
      NotificationType type,
      Long referenceId,
      String title,
      String body,
      LocalDateTime sentAt) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));

    Notification notification = Notification.builder()
        .user(user)
        .type(type)
        .referenceId(referenceId)
        .title(title)
        .body(body)
        .sentAt(sentAt)
        .readAt(null)
        .build();

    return notificationRepository.save(notification);
  }

  @Transactional
  public void markAsSent(Long notificationId, LocalDateTime sentAt) {
    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

    notification.markAsSent(sentAt);
  }
}
