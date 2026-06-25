package com.ssafy.e106.domain.subscription.entity;

import java.time.LocalDate;

import com.ssafy.e106.domain.subscription.enums.SubscriptionType;
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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "subscriptions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long subscriptionId;

  @Column(nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SubscriptionType subscriptionType = SubscriptionType.SINGLE;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "service_id")
  private Service service;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "service_plan_id")
  private ServicePlan servicePlan;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "bundle_id")
  private Bundle bundle;

  private LocalDate nextBillingDate;

  @Column(nullable = false)
  private boolean lowUsageDetected = false;

  @Column(length = 7)
  private String lowUsageCycleYm;

  @Builder
  public Subscription(
      Long userId,
      SubscriptionType subscriptionType,
      Service service,
      ServicePlan servicePlan,
      Bundle bundle,
      LocalDate nextBillingDate) {
    this.userId = userId;
    this.subscriptionType = subscriptionType;
    this.service = service;
    this.servicePlan = servicePlan;
    this.bundle = bundle;
    this.nextBillingDate = nextBillingDate;
  }

  public void update(
      SubscriptionType subscriptionType,
      Service service,
      ServicePlan servicePlan,
      Bundle bundle,
      LocalDate nextBillingDate) {
    this.subscriptionType = subscriptionType;
    this.service = service;
    this.servicePlan = servicePlan;
    this.bundle = bundle;
    this.nextBillingDate = nextBillingDate;
  }

  public void updateLowUsageStatus(boolean lowUsageDetected, String lowUsageCycleYm) {
    this.lowUsageDetected = lowUsageDetected;
    this.lowUsageCycleYm = lowUsageCycleYm;
  }
}
