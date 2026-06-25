package com.ssafy.e106.domain.notification.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.e106.domain.notification.entity.FcmToken;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

  Optional<FcmToken> findByFcmToken(String fcmToken);

  Optional<FcmToken> findFirstByUser_UserIdOrderByCreatedAtDesc(Long userId);

  void deleteAllByUser_UserId(Long userId);
}
