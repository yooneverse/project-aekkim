package com.ssafy.e106.domain.subscription.entity;

import com.ssafy.e106.global.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "bundles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bundle extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long bundleId;

  @Column(nullable = false, unique = true, length = 80)
  private String code;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(nullable = false, length = 100)
  private String planName;

  @Column(nullable = false, length = 20)
  private String billingCycle;

  @Column(nullable = false)
  private Integer monthlyPrice;

  private Integer originalPrice;

  @Column(length = 500)
  private String logoUrl;

  @Column(length = 500)
  private String sourceUrl;

  @Builder
  public Bundle(
      String code,
      String name,
      String planName,
      String billingCycle,
      Integer monthlyPrice,
      Integer originalPrice,
      String logoUrl,
      String sourceUrl) {
    this.code = code;
    this.name = name;
    this.planName = planName;
    this.billingCycle = billingCycle;
    this.monthlyPrice = monthlyPrice;
    this.originalPrice = originalPrice;
    this.logoUrl = logoUrl;
    this.sourceUrl = sourceUrl;
  }

  public void update(
      String name,
      String planName,
      String billingCycle,
      Integer monthlyPrice,
      Integer originalPrice,
      String logoUrl,
      String sourceUrl) {
    this.name = name;
    this.planName = planName;
    this.billingCycle = billingCycle;
    this.monthlyPrice = monthlyPrice;
    this.originalPrice = originalPrice;
    this.logoUrl = logoUrl;
    this.sourceUrl = sourceUrl;
  }
}
