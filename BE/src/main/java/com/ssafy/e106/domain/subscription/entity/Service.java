package com.ssafy.e106.domain.subscription.entity;

import com.ssafy.e106.domain.subscription.enums.ServiceCategory;
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
@Table(name = "services")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Service extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long serviceId;

  @Column(nullable = false, unique = true, length = 50)
  private String code;

  @Column(nullable = false, length = 100)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ServiceCategory category;

  @Column(length = 500)
  private String logoUrl;

  @Column(length = 500)
  private String cancelGuideUrl;

  @Column(length = 30)
  private String customerServicePhone;

  @Column(length = 255)
  private String contactEmail;

  @Builder
  public Service(
      String code,
      String name,
      ServiceCategory category,
      String logoUrl,
      String cancelGuideUrl,
      String customerServicePhone,
      String contactEmail) {
    this.code = code;
    this.name = name;
    this.category = category;
    this.logoUrl = logoUrl;
    this.cancelGuideUrl = cancelGuideUrl;
    this.customerServicePhone = customerServicePhone;
    this.contactEmail = contactEmail;
  }

  public void update(
      String code,
      String name,
      ServiceCategory category,
      String logoUrl,
      String cancelGuideUrl,
      String customerServicePhone,
      String contactEmail) {
    this.code = code;
    this.name = name;
    this.category = category;
    this.logoUrl = logoUrl;
    this.cancelGuideUrl = cancelGuideUrl;
    this.customerServicePhone = customerServicePhone;
    this.contactEmail = contactEmail;
  }
}
