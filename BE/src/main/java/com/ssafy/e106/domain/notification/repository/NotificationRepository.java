package com.ssafy.e106.domain.notification.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ssafy.e106.domain.notification.entity.Notification;
import com.ssafy.e106.domain.notification.enums.NotificationType;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  boolean existsByUser_UserIdAndTypeAndReferenceIdAndBody(
      Long userId,
      com.ssafy.e106.domain.notification.enums.NotificationType type,
      Long referenceId,
      String body);

  boolean existsByUser_UserIdAndTypeAndReferenceId(
      Long userId,
      NotificationType type,
      Long referenceId);

  @Query("""
      select distinct n.referenceId
      from Notification n
      where n.user.userId = :userId
        and n.type = :type
        and n.sentAt is not null
        and n.sentAt >= :since
        and n.referenceId is not null
      """)
  List<Long> findDistinctReferenceIdsByUserAndTypeSentAfter(
      @Param("userId") Long userId,
      @Param("type") NotificationType type,
      @Param("since") LocalDateTime since);

  @Query("""
      select distinct n.referenceId
      from Notification n
      where n.user.userId = :userId
        and n.type = :type
        and n.referenceId is not null
      """)
  List<Long> findDistinctReferenceIdsByUserAndType(
      @Param("userId") Long userId,
      @Param("type") NotificationType type);

  @EntityGraph(attributePaths = {"user"})
  List<Notification> findAllByTypeAndSentAtIsNullAndNotificationIdGreaterThanOrderByNotificationIdAsc(
      NotificationType type,
      Long notificationId,
      Pageable pageable);

  @EntityGraph(attributePaths = {"user"})
  List<Notification> findAllByTypeAndSentAtIsNullOrderByNotificationIdAsc(
      NotificationType type,
      Pageable pageable);

  List<Notification> findAllByUser_UserIdAndSentAtIsNotNullOrderBySentAtDescNotificationIdDesc(
      Long userId,
      Pageable pageable);

  List<Notification> findAllByUser_UserIdAndReadAtIsNullAndSentAtIsNotNullOrderBySentAtDescNotificationIdDesc(
      Long userId,
      Pageable pageable);

  void deleteAllByUser_UserIdAndSentAtIsNotNull(Long userId);

  void deleteAllByUser_UserId(Long userId);
}
