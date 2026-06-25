package com.ssafy.e106.domain.subscriptionusage.entity;

import java.time.LocalDate;

import com.ssafy.e106.domain.subscription.entity.Service;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
    name = "subscription_usage_daily",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_subscription_usage_daily_user_service_date",
            columnNames = {"user_id", "service_id", "usage_date"})
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionUsageDaily extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long usageId;

  @Column(nullable = false)
  private Long userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "service_id", nullable = false)
  private Service service;

  @Column(nullable = false)
  private LocalDate usageDate;

  @Column(nullable = false)
  private Integer usedMinutes;

  @Builder
  public SubscriptionUsageDaily(
      Long userId,
      Service service,
      LocalDate usageDate,
      Integer usedMinutes) {
    this.userId = userId;
    this.service = service;
    this.usageDate = usageDate;
    this.usedMinutes = usedMinutes;
  }

  public void updateUsedMinutes(Integer usedMinutes) {
    this.usedMinutes = usedMinutes;
  }
}
