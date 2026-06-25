package com.ssafy.e106.domain.promotion.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.ssafy.e106.domain.promotion.entity.Promotion;
import com.ssafy.e106.domain.promotion.enums.PromotionType;
import com.ssafy.e106.domain.subscription.entity.Bundle;
import com.ssafy.e106.domain.subscription.entity.CheckinRecord;
import com.ssafy.e106.domain.subscription.entity.Subscription;

@Component
public class PromotionRecommendationRule {

  private static final int MAX_SAVING_SCORE = 40;
  private static final int SAVING_SCORE_DIVISOR = 500;
  private static final int MATCHING_RATIO_SCORE_WEIGHT = 30;
  private static final int MATCHING_COUNT_BONUS_PER_SERVICE = 5;
  private static final int MAX_MATCHING_COUNT_BONUS = 10;
  private static final int MAX_REASON_COUNT = 3;
  private static final String YEARLY_BILLING_CYCLE = "YEARLY";

  public int calculateScore(
      Promotion promotion,
      Set<Long> promotionServiceIds,
      Map<Long, Subscription> subscriptionsByServiceId,
      Map<Long, List<CheckinRecord>> checkinsByServiceId,
      java.time.LocalDateTime now) {
    return calculateEvaluation(
        promotion,
        promotionServiceIds,
        subscriptionsByServiceId,
        checkinsByServiceId,
        now).score();
  }

  public PromotionRecommendationEvaluation calculateEvaluation(
      Promotion promotion,
      Set<Long> promotionServiceIds,
      Map<Long, Subscription> subscriptionsByServiceId,
      Map<Long, List<CheckinRecord>> checkinsByServiceId,
      java.time.LocalDateTime now) {
    int savingScore = calculateSavingScore(calculateMonthlySavingAmount(promotion));
    int matchingScore = calculateMatchingScore(promotionServiceIds, subscriptionsByServiceId.keySet());
    int totalScore = savingScore + matchingScore;

    List<String> reasonLabels = buildPromotionReasonLabels(
        promotion,
        savingScore,
        matchingScore);

    return new PromotionRecommendationEvaluation(totalScore, reasonLabels);
  }

  public PromotionRecommendationEvaluation calculateBundleEvaluation(
      Bundle bundle,
      Set<Long> bundleServiceIds,
      Map<Long, Subscription> subscriptionsByServiceId,
      Map<Long, List<CheckinRecord>> checkinsByServiceId) {
    int savingScore = calculateSavingScore(calculateMonthlySavingAmount(bundle));
    int matchingScore = calculateMatchingScore(bundleServiceIds, subscriptionsByServiceId.keySet());
    int totalScore = savingScore + matchingScore;

    List<String> reasonLabels = buildBundleReasonLabels(
        bundle,
        savingScore,
        matchingScore);

    return new PromotionRecommendationEvaluation(totalScore, reasonLabels);
  }

  public Integer calculateMonthlySavingAmount(Promotion promotion) {
    if (promotion.getOriginalPrice() == null || promotion.getDiscountPrice() == null) {
      return null;
    }
    int savingAmount = promotion.getOriginalPrice() - promotion.getDiscountPrice();
    if (savingAmount <= 0) {
      return null;
    }
    if (YEARLY_BILLING_CYCLE.equalsIgnoreCase(promotion.getBillingCycle())) {
      return savingAmount / 12;
    }
    return savingAmount;
  }

  public Integer calculateMonthlySavingAmount(Bundle bundle) {
    if (bundle.getOriginalPrice() == null || bundle.getMonthlyPrice() == null) {
      return null;
    }
    return bundle.getOriginalPrice() - bundle.getMonthlyPrice();
  }

  private int calculateSavingScore(Integer monthlySavingAmount) {
    if (monthlySavingAmount == null || monthlySavingAmount <= 0) {
      return 0;
    }
    return Math.min(MAX_SAVING_SCORE, monthlySavingAmount / SAVING_SCORE_DIVISOR);
  }

  private int calculateMatchingScore(Set<Long> candidateServiceIds, Set<Long> userServiceIds) {
    if (candidateServiceIds.isEmpty()) {
      return 0;
    }

    long overlapCount = candidateServiceIds.stream()
        .filter(userServiceIds::contains)
        .count();
    int ratioScore = (int) Math.floor(
        (double) MATCHING_RATIO_SCORE_WEIGHT * overlapCount / candidateServiceIds.size());
    int countBonus = (int) Math.min(MAX_MATCHING_COUNT_BONUS, overlapCount * MATCHING_COUNT_BONUS_PER_SERVICE);
    return ratioScore + countBonus;
  }

  private List<String> buildPromotionReasonLabels(
      Promotion promotion,
      int savingScore,
      int matchingScore) {
    List<String> reasons = new ArrayList<>();

    addBaseReasonLabels(reasons, savingScore, matchingScore);

    if (promotion.getPromotionType() == PromotionType.CARD_BENEFIT && reasons.isEmpty()) {
      reasons.add("결제에 연결해서 바로 확인할 수 있어요");
    }

    return limitReasonCount(reasons);
  }

  private List<String> buildBundleReasonLabels(
      Bundle bundle,
      int savingScore,
      int matchingScore) {
    List<String> reasons = new ArrayList<>();

    addBaseReasonLabels(reasons, savingScore, matchingScore);

    if (reasons.isEmpty()) {
      reasons.add(bundle.getPlanName() + " 조합을 한 번에 관리할 수 있어요");
    }

    return limitReasonCount(reasons);
  }

  private void addBaseReasonLabels(
      List<String> reasons,
      int savingScore,
      int matchingScore) {
    if (savingScore >= 20) {
      reasons.add("절약 효과가 큰 편이에요");
    } else if (savingScore > 0) {
      reasons.add("비용을 아낄 수 있어요");
    }

    if (matchingScore >= MATCHING_RATIO_SCORE_WEIGHT + MATCHING_COUNT_BONUS_PER_SERVICE) {
      reasons.add("지금 쓰는 서비스와 바로 연결돼요");
    } else if (matchingScore > 0) {
      reasons.add("현재 이용 중인 서비스와 관련 있어요");
    }
  }

  private List<String> limitReasonCount(List<String> reasons) {
    if (reasons.size() > MAX_REASON_COUNT) {
      return reasons.subList(0, MAX_REASON_COUNT);
    }
    return reasons;
  }
}
