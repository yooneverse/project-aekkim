package com.ssafy.e106.domain.notification.entity;

import com.ssafy.e106.domain.auth.entity.User;
import com.ssafy.e106.global.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "fcm_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmToken extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long fcmTokenId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, unique = true, length = 255)
  private String fcmToken;

  @Builder
  public FcmToken(User user, String fcmToken) {
    this.user = user;
    this.fcmToken = fcmToken;
  }

  public void updateUser(User user) {
    this.user = user;
  }
}
