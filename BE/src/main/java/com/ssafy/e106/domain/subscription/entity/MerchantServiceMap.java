package com.ssafy.e106.domain.subscription.entity;

import jakarta.persistence.*;

import com.ssafy.e106.global.common.BaseEntity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "merchant_service_map",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_merchant_raw", columnNames = "merchantRaw")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MerchantServiceMap extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long merchantServiceMapId;

    @Column(nullable = false, unique = true, length = 200)
    private String merchantRaw;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(nullable = false)
    private Integer hitCount;

    @Builder
    public MerchantServiceMap(String merchantRaw, Service service, Integer hitCount) {
        this.merchantRaw = merchantRaw;
        this.service = service;
        this.hitCount = hitCount;
    }

    public void incrementHitCount() {
        this.hitCount++;
    }

    public void confirm(Service service) {
        this.service = service;
        this.hitCount++;
    }
}
