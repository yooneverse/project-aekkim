package com.ssafy.e106.domain.notification.entity;

import java.time.LocalDateTime;

import com.ssafy.e106.domain.auth.entity.User;
import com.ssafy.e106.domain.notification.enums.NotificationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long notificationId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private NotificationType type;

  private Long referenceId;

  @Column(nullable = false, length = 120)
  private String title;

  @Column(nullable = false, length = 500)
  private String body;

  private LocalDateTime sentAt;

  private LocalDateTime readAt;

  @Builder
  public Notification(
      User user,
      NotificationType type,
      Long referenceId,
      String title,
      String body,
      LocalDateTime sentAt,
      LocalDateTime readAt) {
    this.user = user;
    this.type = type;
    this.referenceId = referenceId;
    this.title = title;
    this.body = body;
    this.sentAt = sentAt;
    this.readAt = readAt;
  }

  public void markAsRead(LocalDateTime readAt) {
    if (this.readAt == null) {
      this.readAt = readAt;
    }
  }

  public void markAsSent(LocalDateTime sentAt) {
    if (this.sentAt == null) {
      this.sentAt = sentAt;
    }
  }
}
