package com.ssafy.e106.domain.subscription.entity;

import java.time.LocalDateTime;

import com.ssafy.e106.domain.auth.entity.User;
import com.ssafy.e106.domain.subscription.enums.CheckinResponseType;
import com.ssafy.e106.global.common.BaseEntity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
    name = "checkin_records",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_checkin_records_user_service_cycle_ym",
            columnNames = {"user_id", "service_id", "cycle_ym"})
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CheckinRecord extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long checkinRecordId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "service_id", nullable = false)
  private Service service;

  @Column(nullable = false, length = 7)
  private String cycleYm;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CheckinResponseType response;

  @Column(nullable = false)
  private LocalDateTime respondedAt;

  @Builder
  public CheckinRecord(
      User user,
      Service service,
      String cycleYm,
      CheckinResponseType response,
      LocalDateTime respondedAt) {
    this.user = user;
    this.service = service;
    this.cycleYm = cycleYm;
    this.response = response;
    this.respondedAt = respondedAt;
  }
}
