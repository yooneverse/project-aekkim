package com.ssafy.e106.domain.promotion.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.e106.domain.promotion.entity.PromotionServiceMapping;

public interface PromotionServiceMappingRepository extends JpaRepository<PromotionServiceMapping, Long> {

    @EntityGraph(attributePaths = {"service"})
    List<PromotionServiceMapping> findByPromotionPromotionId(Long promotionId);

    @EntityGraph(attributePaths = {"service", "promotion"})
    List<PromotionServiceMapping> findByPromotionPromotionIdIn(Collection<Long> promotionIds);

    @EntityGraph(attributePaths = {"service", "promotion"})
    List<PromotionServiceMapping> findByServiceServiceId(Long serviceId);

    boolean existsByService_ServiceId(Long serviceId);

    void deleteAllByPromotion_PromotionId(Long promotionId);
}
