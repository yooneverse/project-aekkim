package com.ssafy.e106.domain.subscription.entity;

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
@Table(name = "service_plans")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServicePlan extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long servicePlanId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "service_id", nullable = false)
  private Service service;

  @Column(nullable = false, length = 100)
  private String planName;

  @Column(nullable = false, length = 20)
  private String billingCycle;

  @Column(nullable = false)
  private Integer monthlyPrice;

  @Builder
  public ServicePlan(
      Service service,
      String planName,
      String billingCycle,
      Integer monthlyPrice) {
    this.service = service;
    this.planName = planName;
    this.billingCycle = billingCycle;
    this.monthlyPrice = monthlyPrice;
  }

  public void update(String planName, String billingCycle, Integer monthlyPrice) {
    this.planName = planName;
    this.billingCycle = billingCycle;
    this.monthlyPrice = monthlyPrice;
  }
}
