package com.ssafy.e106.domain.subscription.entity;

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
    name = "bundle_services",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_bundle_service",
            columnNames = {"bundle_id", "service_id"})
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BundleServiceMap {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long bundleServiceMapId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "bundle_id", nullable = false)
  private Bundle bundle;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "service_id", nullable = false)
  private Service service;

  @Builder
  public BundleServiceMap(Bundle bundle, Service service) {
    this.bundle = bundle;
    this.service = service;
  }
}
