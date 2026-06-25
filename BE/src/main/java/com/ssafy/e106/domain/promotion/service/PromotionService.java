package com.ssafy.e106.domain.promotion.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.e106.domain.auth.repository.UserRepository;
import com.ssafy.e106.domain.promotion.dto.response.PromotionDetailResponse;
import com.ssafy.e106.domain.promotion.dto.response.PromotionDetailServiceResponse;
import com.ssafy.e106.domain.promotion.dto.response.PromotionRecommendationCategoryListResponse;
import com.ssafy.e106.domain.promotion.dto.response.PromotionRecommendationCategoryResponse;
import com.ssafy.e106.domain.promotion.dto.response.PromotionRecommendationItemResponse;
import com.ssafy.e106.domain.promotion.dto.response.PromotionRecommendationServiceResponse;
import com.ssafy.e106.domain.promotion.dto.response.PromotionRecommendationSummary;
import com.ssafy.e106.domain.promotion.entity.Promotion;
import com.ssafy.e106.domain.promotion.entity.PromotionServiceMapping;
import com.ssafy.e106.domain.promotion.enums.PromotionType;
import com.ssafy.e106.domain.promotion.repository.PromotionRepository;
import com.ssafy.e106.domain.promotion.repository.PromotionServiceMappingRepository;
import com.ssafy.e106.domain.subscription.entity.Bundle;
import com.ssafy.e106.domain.subscription.entity.BundleServiceMap;
import com.ssafy.e106.domain.subscription.entity.CheckinRecord;
import com.ssafy.e106.domain.subscription.entity.Service;
import com.ssafy.e106.domain.subscription.entity.Subscription;
import com.ssafy.e106.domain.subscription.enums.ServiceCategory;
import com.ssafy.e106.domain.subscription.repository.BundleRepository;
import com.ssafy.e106.domain.subscription.repository.BundleServiceMapRepository;
import com.ssafy.e106.domain.subscription.repository.CheckinRecordRepository;
import com.ssafy.e106.domain.subscription.repository.SubscriptionRepository;
import com.ssafy.e106.global.exception.BusinessException;
import com.ssafy.e106.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class PromotionService {

  private static final int LIST_RECOMMENDATION_SCORE_THRESHOLD = 0;
  private static final int SUMMARY_RECOMMENDATION_SCORE_THRESHOLD = 0;
  private static final int DEFAULT_SECTION_SIZE = 3;
  private static final int MAX_SECTION_SIZE = 20;
  private static final int RECENT_CHECKIN_SAMPLE_SIZE = 30;
  private static final LocalDateTime BUNDLE_DEFAULT_ENDS_AT = LocalDateTime.of(2099, 12, 31, 23, 59, 59);
  private static final List<String> CARD_BENEFIT_PRIORITY_KEYWORDS = List.of("할인", "적립", "%", "원");

  private final UserRepository userRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final CheckinRecordRepository checkinRecordRepository;
  private final PromotionRepository promotionRepository;
  private final PromotionServiceMappingRepository promotionServiceMappingRepository;
  private final BundleRepository bundleRepository;
  private final BundleServiceMapRepository bundleServiceMapRepository;
  private final PromotionRecommendationRule promotionRecommendationRule;

  @Transactional(readOnly = true)
  public PromotionRecommendationCategoryListResponse getRecommendedPromotions(
      Long userId,
      String promotionType,
      Integer page,
      Integer size) {
    userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));

    PromotionType normalizedPromotionType = parsePromotionType(promotionType);
    normalizePage(page);
    int normalizedSize = normalizeSize(size);
    LocalDateTime now = LocalDateTime.now();

    List<Subscription> subscriptions = subscriptionRepository.findAllByUserIdOrderBySubscriptionIdDesc(userId);
    if (subscriptions.isEmpty()) {
      return new PromotionRecommendationCategoryListResponse(List.of(), normalizedSize);
    }

    Map<Long, Service> subscribedServicesById = buildSubscribedServicesById(subscriptions);
    if (subscribedServicesById.isEmpty()) {
      return new PromotionRecommendationCategoryListResponse(List.of(), normalizedSize);
    }

    Map<Long, Subscription> subscriptionsByServiceId = buildSubscriptionsByServiceId(subscriptions);
    Map<Long, List<CheckinRecord>> checkinsByServiceId = getRecentCheckinsByServiceId(userId);
    Set<Long> subscribedBundleIds = getSubscribedBundleIds(subscriptions);
    Map<ServiceCategory, Set<Long>> serviceIdsByCategory = subscribedServicesById.values().stream()
        .collect(Collectors.groupingBy(
            Service::getCategory,
            LinkedHashMap::new,
            Collectors.mapping(Service::getServiceId, Collectors.toSet())));

    List<PromotionRecommendationCategoryResponse> categories = new ArrayList<>();
    for (Map.Entry<ServiceCategory, Set<Long>> entry : serviceIdsByCategory.entrySet()) {
      categories.add(buildCategoryResponse(
          entry.getKey(),
          entry.getValue(),
          normalizedPromotionType,
          normalizedSize,
          subscriptionsByServiceId,
          checkinsByServiceId,
          subscribedBundleIds,
          now));
    }

    return new PromotionRecommendationCategoryListResponse(categories, normalizedSize);
  }

  @Transactional(readOnly = true)
  public PromotionDetailResponse getPromotion(Long promotionId) {
    if (promotionId < 0) {
      return getBundlePromotionDetail(Math.negateExact(promotionId));
    }

    Promotion promotion = promotionRepository.findById(promotionId)
        .orElseThrow(() -> new BusinessException(ErrorCode.PROMOTION_NOT_FOUND));

    List<PromotionDetailServiceResponse> services = promotionServiceMappingRepository
        .findByPromotionPromotionId(promotionId)
        .stream()
        .map(PromotionServiceMapping::getService)
        .sorted(Comparator.comparing(Service::getServiceId))
        .map(PromotionDetailServiceResponse::from)
        .toList();

    return PromotionDetailResponse.of(promotion, services);
  }

  @Transactional(readOnly = true)
  public PromotionRecommendationSummary getTopRecommendationForSubscription(
      Long userId,
      Subscription currentSubscription) {
    Set<Long> targetServiceIds = extractTargetServiceIds(currentSubscription);
    if (targetServiceIds.isEmpty()) {
      return null;
    }

    LocalDateTime now = LocalDateTime.now();
    List<Subscription> subscriptions = subscriptionRepository.findAllByUserIdOrderBySubscriptionIdDesc(userId);
    Map<Long, Subscription> subscriptionsByServiceId = buildSubscriptionsByServiceId(subscriptions);
    Map<Long, List<CheckinRecord>> checkinsByServiceId = getRecentCheckinsByServiceId(userId);
    Set<Long> subscribedBundleIds = getSubscribedBundleIds(subscriptions);

    ScoredRecommendationSummary topPromotion = getTopPromotionRecommendationSummary(
        targetServiceIds,
        null,
        subscriptionsByServiceId,
        checkinsByServiceId,
        now);
    ScoredRecommendationSummary topBundle = getTopBundleRecommendationSummary(
        targetServiceIds,
        null,
        subscriptionsByServiceId,
        checkinsByServiceId,
        subscribedBundleIds);

    return pickTopSummary(topPromotion, topBundle);
  }

  @Transactional(readOnly = true)
  public PromotionRecommendationSummary findTopRecommendationForPromoBatch(
      Long userId,
      Set<Long> targetServiceIds,
      Set<Long> excludedPromotionIds) {
    userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));

    if (targetServiceIds == null || targetServiceIds.isEmpty()) {
      return null;
    }

    LocalDateTime now = LocalDateTime.now();
    List<Subscription> subscriptions = subscriptionRepository.findAllByUserIdOrderBySubscriptionIdDesc(userId);
    Map<Long, Subscription> subscriptionsByServiceId = buildSubscriptionsByServiceId(subscriptions);
    Map<Long, List<CheckinRecord>> checkinsByServiceId = getRecentCheckinsByServiceId(userId);
    Set<Long> subscribedBundleIds = getSubscribedBundleIds(subscriptions);

    ScoredRecommendationSummary topPromotion = getTopPromotionRecommendationSummary(
        targetServiceIds,
        excludedPromotionIds,
        subscriptionsByServiceId,
        checkinsByServiceId,
        now);
    ScoredRecommendationSummary topBundle = getTopBundleRecommendationSummary(
        targetServiceIds,
        excludedPromotionIds,
        subscriptionsByServiceId,
        checkinsByServiceId,
        subscribedBundleIds);

    return pickTopSummary(topPromotion, topBundle);
  }

  private PromotionRecommendationCategoryResponse buildCategoryResponse(
      ServiceCategory category,
      Set<Long> serviceIds,
      PromotionType promotionType,
      int size,
      Map<Long, Subscription> subscriptionsByServiceId,
      Map<Long, List<CheckinRecord>> checkinsByServiceId,
      Set<Long> subscribedBundleIds,
      LocalDateTime now) {
    List<PromotionRecommendationItemResponse> bundles = shouldInclude(PromotionType.BUNDLE, promotionType)
        ? getTopBundleRecommendations(
            serviceIds,
            size,
            subscriptionsByServiceId,
            checkinsByServiceId,
            subscribedBundleIds)
        : List.of();
    List<PromotionRecommendationItemResponse> promotions = shouldInclude(PromotionType.PROMO, promotionType)
        ? getTopRecommendationsByType(
            serviceIds,
            PromotionType.PROMO,
            size,
            subscriptionsByServiceId,
            checkinsByServiceId,
            now)
        : List.of();
    List<PromotionRecommendationItemResponse> cardBenefits = shouldInclude(
        PromotionType.CARD_BENEFIT,
        promotionType)
            ? getTopRecommendationsByType(
                serviceIds,
                PromotionType.CARD_BENEFIT,
                size,
                subscriptionsByServiceId,
                checkinsByServiceId,
                now)
            : List.of();

    return new PromotionRecommendationCategoryResponse(
        category.name(),
        bundles,
        promotions,
        cardBenefits);
  }

  private boolean shouldInclude(PromotionType candidateType, PromotionType requestedType) {
    return requestedType == null || requestedType == candidateType;
  }

  private List<PromotionRecommendationItemResponse> getTopBundleRecommendations(
      Set<Long> serviceIds,
      int limit,
      Map<Long, Subscription> subscriptionsByServiceId,
      Map<Long, List<CheckinRecord>> checkinsByServiceId,
      Set<Long> subscribedBundleIds) {
    return getBundleRecommendationCandidates(
        serviceIds,
        subscriptionsByServiceId,
        checkinsByServiceId,
        subscribedBundleIds,
        Set.of())
        .stream()
        .filter(candidate -> candidate.evaluation().score() >= LIST_RECOMMENDATION_SCORE_THRESHOLD)
        .sorted(buildBundleRecommendationComparator())
        .limit(limit)
        .map(candidate -> PromotionRecommendationItemResponse.ofBundle(
            candidate.bundle(),
            candidate.evaluation(),
            candidate.services(),
            candidate.startsAt(),
            BUNDLE_DEFAULT_ENDS_AT))
        .toList();
  }

  private ScoredRecommendationSummary getTopBundleRecommendationSummary(
      Set<Long> targetServiceIds,
      Set<Long> excludedPromotionIds,
      Map<Long, Subscription> subscriptionsByServiceId,
      Map<Long, List<CheckinRecord>> checkinsByServiceId,
      Set<Long> subscribedBundleIds) {
    return getBundleRecommendationCandidates(
        targetServiceIds,
        subscriptionsByServiceId,
        checkinsByServiceId,
        subscribedBundleIds,
        extractExcludedBundleIds(excludedPromotionIds))
        .stream()
        .filter(candidate -> candidate.evaluation().score() >= SUMMARY_RECOMMENDATION_SCORE_THRESHOLD)
        .sorted(buildBundleRecommendationComparator())
        .findFirst()
        .map(candidate -> new ScoredRecommendationSummary(
            candidate.evaluation().score(),
            PromotionRecommendationSummary.ofBundle(
                candidate.bundle(),
                candidate.monthlySavingAmount(),
                candidate.services())))
        .orElse(null);
  }

  private List<BundleRecommendationCandidate> getBundleRecommendationCandidates(
      Set<Long> targetServiceIds,
      Map<Long, Subscription> subscriptionsByServiceId,
      Map<Long, List<CheckinRecord>> checkinsByServiceId,
      Set<Long> subscribedBundleIds,
      Set<Long> excludedBundleIds) {
    if (targetServiceIds == null || targetServiceIds.isEmpty()) {
      return List.of();
    }

    List<Bundle> bundles = bundleRepository.findAllByOrderByBundleIdAsc();
    if (bundles.isEmpty()) {
      return List.of();
    }

    Map<Long, List<BundleServiceMap>> mappingsByBundleId = bundleServiceMapRepository.findByBundle_BundleIdIn(
        bundles.stream().map(Bundle::getBundleId).toList())
        .stream()
        .collect(Collectors.groupingBy(
            mapping -> mapping.getBundle().getBundleId(),
            Collectors.collectingAndThen(Collectors.toList(), this::sortBundleServiceMappings)));

    List<BundleRecommendationCandidate> candidates = new ArrayList<>();
    for (Bundle bundle : bundles) {
      if (subscribedBundleIds.contains(bundle.getBundleId()) || excludedBundleIds.contains(bundle.getBundleId())) {
        continue;
      }

      List<BundleServiceMap> mappings = mappingsByBundleId.getOrDefault(bundle.getBundleId(), List.of());
      Set<Long> bundleServiceIds = mappings.stream()
          .map(mapping -> mapping.getService().getServiceId())
          .collect(Collectors.toSet());
      long overlapCount = bundleServiceIds.stream()
          .filter(targetServiceIds::contains)
          .count();

      if (overlapCount == 0L) {
        continue;
      }

      List<PromotionRecommendationServiceResponse> services = mappings.stream()
          .map(BundleServiceMap::getService)
          .map(PromotionRecommendationServiceResponse::from)
          .toList();
      PromotionRecommendationEvaluation evaluation = promotionRecommendationRule.calculateBundleEvaluation(
          bundle,
          bundleServiceIds,
          subscriptionsByServiceId,
          checkinsByServiceId);

      candidates.add(new BundleRecommendationCandidate(
          bundle,
          services,
          evaluation,
          promotionRecommendationRule.calculateMonthlySavingAmount(bundle),
          resolveBundleStartsAt(bundle)));
    }

    return candidates;
  }

  private List<PromotionRecommendationItemResponse> getTopRecommendationsByType(
      Set<Long> serviceIds,
      PromotionType promotionType,
      int limit,
      Map<Long, Subscription> subscriptionsByServiceId,
      Map<Long, List<CheckinRecord>> checkinsByServiceId,
      LocalDateTime now) {
    if (serviceIds == null || serviceIds.isEmpty()) {
      return List.of();
    }

    List<Promotion> promotions = promotionRepository.findRecommendedPromotions(
        serviceIds,
        promotionType,
        now);

    if (promotions.isEmpty()) {
      return List.of();
    }

    Map<Long, List<PromotionRecommendationServiceResponse>> servicesByPromotionId =
        getRecommendationServicesByPromotionId(promotions);
    Map<Long, Set<Long>> promotionServiceIds = servicesByPromotionId.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> entry.getValue().stream()
                .map(PromotionRecommendationServiceResponse::serviceId)
                .collect(Collectors.toSet())));
    Map<Long, PromotionRecommendationEvaluation> evaluationsByPromotionId = calculateEvaluations(
        promotions,
        promotionServiceIds,
        subscriptionsByServiceId,
        checkinsByServiceId,
        now);

    return promotions.stream()
        .filter(promotion -> evaluationScore(evaluationsByPromotionId, promotion.getPromotionId())
            >= LIST_RECOMMENDATION_SCORE_THRESHOLD)
        .sorted(buildRecommendationComparator(evaluationsByPromotionId))
        .limit(limit)
        .map(promotion -> PromotionRecommendationItemResponse.of(
            promotion,
            evaluationsByPromotionId.getOrDefault(
                promotion.getPromotionId(),
                new PromotionRecommendationEvaluation(0, List.of())),
            servicesByPromotionId.getOrDefault(promotion.getPromotionId(), List.of())))
        .toList();
  }

  private ScoredRecommendationSummary getTopPromotionRecommendationSummary(
      Set<Long> targetServiceIds,
      Set<Long> excludedPromotionIds,
      Map<Long, Subscription> subscriptionsByServiceId,
      Map<Long, List<CheckinRecord>> checkinsByServiceId,
      LocalDateTime now) {
    List<Promotion> promotions = promotionRepository.findRecommendedPromotions(
        targetServiceIds,
        null,
        now)
        .stream()
        .filter(promotion -> !extractExcludedPromotionIds(excludedPromotionIds).contains(promotion.getPromotionId()))
        .toList();

    if (promotions.isEmpty()) {
      return null;
    }

    Map<Long, List<PromotionRecommendationServiceResponse>> servicesByPromotionId =
        getRecommendationServicesByPromotionId(promotions);
    Map<Long, Set<Long>> promotionServiceIds = servicesByPromotionId.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> entry.getValue().stream()
                .map(PromotionRecommendationServiceResponse::serviceId)
                .collect(Collectors.toSet())));
    Map<Long, PromotionRecommendationEvaluation> evaluationsByPromotionId = calculateEvaluations(
        promotions,
        promotionServiceIds,
        subscriptionsByServiceId,
        checkinsByServiceId,
        now);

    return promotions.stream()
        .filter(promotion -> evaluationScore(evaluationsByPromotionId, promotion.getPromotionId())
            >= SUMMARY_RECOMMENDATION_SCORE_THRESHOLD)
        .sorted(buildRecommendationComparator(evaluationsByPromotionId))
        .findFirst()
        .map(promotion -> new ScoredRecommendationSummary(
            evaluationScore(evaluationsByPromotionId, promotion.getPromotionId()),
            PromotionRecommendationSummary.of(
                promotion,
                promotionRecommendationRule.calculateMonthlySavingAmount(promotion),
                servicesByPromotionId.getOrDefault(promotion.getPromotionId(), List.of()))))
        .orElse(null);
  }

  private Map<Long, List<PromotionRecommendationServiceResponse>> getRecommendationServicesByPromotionId(
      List<Promotion> promotions) {
    List<Long> promotionIds = promotions.stream()
        .map(Promotion::getPromotionId)
        .toList();

    if (promotionIds.isEmpty()) {
      return Collections.emptyMap();
    }

    return promotionServiceMappingRepository.findByPromotionPromotionIdIn(promotionIds)
        .stream()
        .collect(Collectors.groupingBy(
            mapping -> mapping.getPromotion().getPromotionId(),
            Collectors.mapping(
                PromotionRecommendationServiceResponse::from,
                Collectors.collectingAndThen(Collectors.toList(), this::sortRecommendationServices))));
  }

  private Map<Long, List<CheckinRecord>> getRecentCheckinsByServiceId(Long userId) {
    List<CheckinRecord> recentCheckins = checkinRecordRepository.findHistory(
        userId,
        null,
        null,
        null,
        PageRequest.of(0, RECENT_CHECKIN_SAMPLE_SIZE));

    return recentCheckins.stream()
        .collect(Collectors.groupingBy(checkinRecord -> checkinRecord.getService().getServiceId()));
  }

  private Map<Long, PromotionRecommendationEvaluation> calculateEvaluations(
      List<Promotion> promotions,
      Map<Long, Set<Long>> promotionServiceIds,
      Map<Long, Subscription> subscriptionsByServiceId,
      Map<Long, List<CheckinRecord>> checkinsByServiceId,
      LocalDateTime now) {
    return promotions.stream()
        .collect(Collectors.toMap(
            Promotion::getPromotionId,
            promotion -> promotionRecommendationRule.calculateEvaluation(
                promotion,
                promotionServiceIds.getOrDefault(promotion.getPromotionId(), Set.of()),
                subscriptionsByServiceId,
                checkinsByServiceId,
                now)));
  }

  private Comparator<Promotion> buildRecommendationComparator(
      Map<Long, PromotionRecommendationEvaluation> evaluationsByPromotionId) {
    return Comparator
        .comparingInt((Promotion promotion) -> cardBenefitPriorityWeight(promotion))
        .reversed()
        .thenComparing(
            Comparator.comparingInt((Promotion promotion) -> evaluationScore(
                evaluationsByPromotionId,
                promotion.getPromotionId()))
                .reversed())
        .thenComparing(
            Promotion::getPromotionId,
            Comparator.reverseOrder());
  }

  private Comparator<BundleRecommendationCandidate> buildBundleRecommendationComparator() {
    return Comparator
        .comparingInt((BundleRecommendationCandidate candidate) -> candidate.evaluation().score())
        .reversed()
        .thenComparing(
            BundleRecommendationCandidate::monthlySavingAmount,
            Comparator.nullsLast(Comparator.reverseOrder()))
        .thenComparing(BundleRecommendationCandidate::startsAt, Comparator.reverseOrder())
        .thenComparing(candidate -> candidate.bundle().getBundleId(), Comparator.reverseOrder());
  }

  private int evaluationScore(
      Map<Long, PromotionRecommendationEvaluation> evaluationsByPromotionId,
      Long promotionId) {
    return evaluationsByPromotionId.getOrDefault(
        promotionId,
        new PromotionRecommendationEvaluation(0, List.of())).score();
  }

  private int cardBenefitPriorityWeight(Promotion promotion) {
    if (promotion.getPromotionType() != PromotionType.CARD_BENEFIT) {
      return 0;
    }
    String searchable = String.join(" ",
        promotion.getTitle() == null ? "" : promotion.getTitle(),
        promotion.getSummary() == null ? "" : promotion.getSummary());
    return CARD_BENEFIT_PRIORITY_KEYWORDS.stream()
        .anyMatch(searchable::contains) ? 1 : 0;
  }

  private PromotionType parsePromotionType(String promotionType) {
    if (promotionType == null || promotionType.isBlank() || "ALL".equalsIgnoreCase(promotionType)) {
      return null;
    }

    try {
      return PromotionType.valueOf(promotionType.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT,
          "promotionType은 ALL, BUNDLE, CARD_BENEFIT, PROMO 중 하나여야 합니다.");
    }
  }

  private int normalizePage(Integer page) {
    if (page == null) {
      return 0;
    }
    if (page < 0) {
      throw new BusinessException(ErrorCode.INVALID_INPUT, "page는 0 이상이어야 합니다.");
    }
    return page;
  }

  private int normalizeSize(Integer size) {
    if (size == null) {
      return DEFAULT_SECTION_SIZE;
    }
    if (size < 1 || size > MAX_SECTION_SIZE) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT,
          "size는 1 이상 " + MAX_SECTION_SIZE + " 이하여야 합니다.");
    }
    return size;
  }

  private List<PromotionRecommendationServiceResponse> sortRecommendationServices(
      List<PromotionRecommendationServiceResponse> services) {
    return services.stream()
        .sorted(Comparator.comparing(PromotionRecommendationServiceResponse::serviceId))
        .toList();
  }

  private List<BundleServiceMap> sortBundleServiceMappings(List<BundleServiceMap> mappings) {
    return mappings.stream()
        .sorted(Comparator.comparing(mapping -> mapping.getService().getServiceId()))
        .toList();
  }

  private Map<Long, Service> buildSubscribedServicesById(List<Subscription> subscriptions) {
    Map<Long, Service> servicesById = new LinkedHashMap<>();

    for (Subscription subscription : subscriptions) {
      if (subscription.getService() != null) {
        servicesById.put(subscription.getService().getServiceId(), subscription.getService());
      }
    }

    Map<Long, List<BundleServiceMap>> mappingsByBundleId = getSubscribedBundleMappings(subscriptions);
    for (List<BundleServiceMap> mappings : mappingsByBundleId.values()) {
      for (BundleServiceMap mapping : mappings) {
        servicesById.putIfAbsent(mapping.getService().getServiceId(), mapping.getService());
      }
    }

    return servicesById;
  }

  private Map<Long, Subscription> buildSubscriptionsByServiceId(List<Subscription> subscriptions) {
    Map<Long, Subscription> subscriptionsByServiceId = new LinkedHashMap<>();

    for (Subscription subscription : subscriptions) {
      if (subscription.getService() != null) {
        subscriptionsByServiceId.put(subscription.getService().getServiceId(), subscription);
      }
    }

    Map<Long, List<BundleServiceMap>> mappingsByBundleId = getSubscribedBundleMappings(subscriptions);
    for (Subscription subscription : subscriptions) {
      if (subscription.getBundle() == null) {
        continue;
      }
      List<BundleServiceMap> mappings = mappingsByBundleId.getOrDefault(subscription.getBundle().getBundleId(), List.of());
      for (BundleServiceMap mapping : mappings) {
        subscriptionsByServiceId.putIfAbsent(mapping.getService().getServiceId(), subscription);
      }
    }

    return subscriptionsByServiceId;
  }

  private Map<Long, List<BundleServiceMap>> getSubscribedBundleMappings(List<Subscription> subscriptions) {
    Set<Long> subscribedBundleIds = getSubscribedBundleIds(subscriptions);
    if (subscribedBundleIds.isEmpty()) {
      return Collections.emptyMap();
    }

    return bundleServiceMapRepository.findByBundle_BundleIdIn(subscribedBundleIds.stream().toList())
        .stream()
        .collect(Collectors.groupingBy(
            mapping -> mapping.getBundle().getBundleId(),
            Collectors.collectingAndThen(Collectors.toList(), this::sortBundleServiceMappings)));
  }

  private Set<Long> getSubscribedBundleIds(List<Subscription> subscriptions) {
    return subscriptions.stream()
        .filter(subscription -> subscription.getBundle() != null)
        .map(subscription -> subscription.getBundle().getBundleId())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private Set<Long> extractTargetServiceIds(Subscription subscription) {
    if (subscription.getService() != null) {
      return Set.of(subscription.getService().getServiceId());
    }
    if (subscription.getBundle() == null) {
      return Set.of();
    }
    return bundleServiceMapRepository.findByBundle_BundleIdOrderByService_ServiceIdAsc(subscription.getBundle().getBundleId())
        .stream()
        .map(mapping -> mapping.getService().getServiceId())
        .collect(Collectors.toSet());
  }

  private Set<Long> extractExcludedPromotionIds(Set<Long> excludedPromotionIds) {
    if (excludedPromotionIds == null || excludedPromotionIds.isEmpty()) {
      return Set.of();
    }
    return excludedPromotionIds.stream()
        .filter(referenceId -> referenceId != null && referenceId > 0)
        .collect(Collectors.toSet());
  }

  private Set<Long> extractExcludedBundleIds(Set<Long> excludedPromotionIds) {
    if (excludedPromotionIds == null || excludedPromotionIds.isEmpty()) {
      return Set.of();
    }
    return excludedPromotionIds.stream()
        .filter(referenceId -> referenceId != null && referenceId < 0)
        .map(referenceId -> Math.negateExact(referenceId))
        .collect(Collectors.toSet());
  }

  private PromotionDetailResponse getBundlePromotionDetail(Long bundleId) {
    Bundle bundle = bundleRepository.findById(bundleId)
        .orElseThrow(() -> new BusinessException(ErrorCode.PROMOTION_NOT_FOUND));

    List<PromotionDetailServiceResponse> services = bundleServiceMapRepository
        .findByBundle_BundleIdOrderByService_ServiceIdAsc(bundleId)
        .stream()
        .map(BundleServiceMap::getService)
        .map(PromotionDetailServiceResponse::from)
        .toList();

    return PromotionDetailResponse.ofBundle(
        bundle,
        services,
        resolveBundleStartsAt(bundle),
        BUNDLE_DEFAULT_ENDS_AT);
  }

  private LocalDateTime resolveBundleStartsAt(Bundle bundle) {
    return bundle.getCreatedAt() != null ? bundle.getCreatedAt() : LocalDateTime.now();
  }

  private PromotionRecommendationSummary pickTopSummary(
      ScoredRecommendationSummary left,
      ScoredRecommendationSummary right) {
    if (left == null) {
      return right == null ? null : right.summary();
    }
    if (right == null) {
      return left.summary();
    }
    return left.score() >= right.score() ? left.summary() : right.summary();
  }

  private record BundleRecommendationCandidate(
      Bundle bundle,
      List<PromotionRecommendationServiceResponse> services,
      PromotionRecommendationEvaluation evaluation,
      Integer monthlySavingAmount,
      LocalDateTime startsAt) {
  }

  private record ScoredRecommendationSummary(
      int score,
      PromotionRecommendationSummary summary) {
  }
}
