package com.ssafy.e106.domain.notification.batch.processor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.ssafy.e106.domain.notification.batch.model.PromoCandidate;
import com.ssafy.e106.domain.notification.batch.model.PromoTarget;
import com.ssafy.e106.domain.notification.enums.NotificationType;
import com.ssafy.e106.domain.notification.repository.NotificationRepository;
import com.ssafy.e106.domain.promotion.dto.response.PromotionRecommendationSummary;
import com.ssafy.e106.domain.promotion.service.PromotionService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PromoTargetProcessor implements ItemProcessor<PromoCandidate, PromoTarget> {

  private static final int RECENT_PROMO_EXCLUSION_DAYS = 30;

  private final PromotionService promotionService;
  private final NotificationRepository notificationRepository;

  @Override
  public PromoTarget process(PromoCandidate item) {
    Set<Long> excludedPromotionIds = new HashSet<>(
        notificationRepository.findDistinctReferenceIdsByUserAndTypeSentAfter(
            item.userId(),
            NotificationType.PROMO,
            LocalDateTime.now().minusDays(RECENT_PROMO_EXCLUSION_DAYS)));
    excludedPromotionIds.addAll(
        notificationRepository.findDistinctReferenceIdsByUserAndType(
            item.userId(),
            NotificationType.PROMO));

    PromotionRecommendationSummary summary = promotionService.findTopRecommendationForPromoBatch(
        item.userId(),
        item.serviceIds(),
        excludedPromotionIds);

    if (summary == null) {
      return null;
    }

    return new PromoTarget(
        item.userId(),
        summary.promotionId(),
        summary.title(),
        summary.headline());
  }
}
