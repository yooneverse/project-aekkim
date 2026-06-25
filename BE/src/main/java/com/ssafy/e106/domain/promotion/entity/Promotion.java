package com.ssafy.e106.domain.promotion.entity;

import java.time.LocalDateTime;

import com.ssafy.e106.domain.promotion.enums.PromotionType;
import com.ssafy.e106.global.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "promotions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Promotion extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long promotionId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PromotionType promotionType;

  @Column(nullable = false, length = 150)
  private String title;

  @Column(length = 500)
  private String summary;

  private Integer originalPrice;

  private Integer discountPrice;

  @Column(length = 20)
  private String billingCycle;

  @Column(nullable = false)
  private LocalDateTime startsAt;

  @Column(nullable = false)
  private LocalDateTime endsAt;

  @Column(length = 500)
  private String sourceUrl;

  @Column(length = 500)
  private String imageUrl;

  @Builder
  public Promotion(
      PromotionType promotionType,
      String title,
      String summary,
      Integer originalPrice,
      Integer discountPrice,
      String billingCycle,
      LocalDateTime startsAt,
      LocalDateTime endsAt,
      String sourceUrl,
      String imageUrl) {
    this.promotionType = promotionType;
    this.title = title;
    this.summary = summary;
    this.originalPrice = originalPrice;
    this.discountPrice = discountPrice;
    this.billingCycle = billingCycle;
    this.startsAt = startsAt;
    this.endsAt = endsAt;
    this.sourceUrl = sourceUrl;
    this.imageUrl = imageUrl;
  }

  public void update(
      PromotionType promotionType,
      String title,
      String summary,
      Integer originalPrice,
      Integer discountPrice,
      String billingCycle,
      LocalDateTime startsAt,
      LocalDateTime endsAt,
      String sourceUrl,
      String imageUrl) {
    this.promotionType = promotionType;
    this.title = title;
    this.summary = summary;
    this.originalPrice = originalPrice;
    this.discountPrice = discountPrice;
    this.billingCycle = billingCycle;
    this.startsAt = startsAt;
    this.endsAt = endsAt;
    this.sourceUrl = sourceUrl;
    this.imageUrl = imageUrl;
  }
}
