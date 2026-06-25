package com.ssafy.e106.domain.promotion.entity;

import jakarta.persistence.*;

import com.ssafy.e106.domain.subscription.entity.Service;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "promotion_services",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_promotion_service",
                        columnNames = {"promotion_id", "service_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromotionServiceMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long promotionServiceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Builder
    public PromotionServiceMapping(Promotion promotion, Service service) {
        this.promotion = promotion;
        this.service = service;
    }
}
