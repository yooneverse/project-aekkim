package com.ssafy.e106.domain.subscriptionusage.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import com.ssafy.e106.domain.subscription.entity.BundleServiceMap;
import com.ssafy.e106.domain.subscription.entity.ServicePlan;
import com.ssafy.e106.domain.subscription.entity.Subscription;
import com.ssafy.e106.domain.subscription.entity.Service;
import com.ssafy.e106.domain.subscription.enums.ServiceCategory;
import com.ssafy.e106.domain.subscription.enums.SubscriptionType;
import com.ssafy.e106.domain.subscription.repository.BundleServiceMapRepository;
import com.ssafy.e106.domain.subscription.repository.ServiceRepository;
import com.ssafy.e106.domain.subscription.repository.SubscriptionRepository;
import com.ssafy.e106.domain.subscriptionusage.dto.request.SubscriptionUsageDailyItemRequest;
import com.ssafy.e106.domain.subscriptionusage.dto.request.SubscriptionUsageDailyUpsertRequest;
import com.ssafy.e106.domain.subscriptionusage.dto.response.SubscriptionUsageDailyPointResponse;
import com.ssafy.e106.domain.subscriptionusage.dto.response.SubscriptionUsageDailyResponse;
import com.ssafy.e106.domain.subscriptionusage.dto.response.SubscriptionUsageDailyUpsertResponse;
import com.ssafy.e106.domain.subscriptionusage.dto.response.SubscriptionUsageReportInsightResponse;
import com.ssafy.e106.domain.subscriptionusage.dto.response.SubscriptionUsageReportItemResponse;
import com.ssafy.e106.domain.subscriptionusage.dto.response.SubscriptionUsageReportResponse;
import com.ssafy.e106.domain.subscriptionusage.dto.response.SubscriptionUsageReportSummaryResponse;
import com.ssafy.e106.domain.subscriptionusage.entity.SubscriptionUsageDaily;
import com.ssafy.e106.domain.subscriptionusage.repository.SubscriptionUsageDailyRepository;
import com.ssafy.e106.global.exception.BusinessException;
import com.ssafy.e106.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class SubscriptionUsageService {

  private static final Long DEFAULT_USER_ID = 1L;
  private static final int DEFAULT_WINDOW_DAYS = 30;
  private static final String NUDGE_KEEP_PAYING = "구독한 거 잊으신건 아니죠?";
  private static final String NUDGE_SLOWING_DOWN = "요즘은 좀 뜸해요";
  private static final String NUDGE_FREQUENT = "자주 쓰는 편이에요";

  private final SubscriptionUsageDailyRepository subscriptionUsageDailyRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final ServiceRepository serviceRepository;
  private final BundleServiceMapRepository bundleServiceMapRepository;

  @Transactional
  public SubscriptionUsageDailyUpsertResponse upsertDailyUsage(SubscriptionUsageDailyUpsertRequest request) {
    Long userId = DEFAULT_USER_ID;
    int savedCount = 0;

    for (SubscriptionUsageDailyItemRequest item : request.items()) {
      Service service = serviceRepository.findById(item.serviceId())
          .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_NOT_FOUND));

      SubscriptionUsageDaily usage = subscriptionUsageDailyRepository
          .findByUserIdAndService_ServiceIdAndUsageDate(userId, item.serviceId(), item.usageDate())
          .orElseGet(() -> SubscriptionUsageDaily.builder()
              .userId(userId)
              .service(service)
              .usageDate(item.usageDate())
              .usedMinutes(item.usedMinutes())
              .build());

      usage.updateUsedMinutes(item.usedMinutes());
      subscriptionUsageDailyRepository.save(usage);
      savedCount++;
    }

    return new SubscriptionUsageDailyUpsertResponse(savedCount);
  }

  @Transactional(readOnly = true)
  public SubscriptionUsageReportResponse getUsageReport(Long currentUserId, Integer days) {
    int windowDays = normalizeDays(days);
    LocalDate endDate = LocalDate.now();
    LocalDate startDate = endDate.minusDays(windowDays - 1L);

    List<Subscription> subscriptions = subscriptionRepository.findAllByUserIdOrderBySubscriptionIdDesc(currentUserId);
    List<SubscriptionUsageDaily> usages = subscriptionUsageDailyRepository
        .findAllByUserIdAndUsageDateBetweenOrderByUsageDateAscService_ServiceIdAsc(
            DEFAULT_USER_ID,
            startDate,
            endDate);

    Map<Long, List<BundleServiceMap>> bundleMappingsByBundleId = mapBundleServices(subscriptions);
    Map<Long, List<SubscriptionUsageDaily>> usagesByServiceId = usages.stream()
        .collect(Collectors.groupingBy(usage -> usage.getService().getServiceId()));

    List<SubscriptionUsageReportItemResponse> items = subscriptions.stream()
        .map(subscription -> toReportItem(subscription, bundleMappingsByBundleId, usagesByServiceId))
        .toList();

    int totalUsedMinutes = usages.stream()
        .mapToInt(SubscriptionUsageDaily::getUsedMinutes)
        .sum();

    SubscriptionUsageReportItemResponse mostUsedItem = items.stream()
        .max(Comparator.comparingInt(SubscriptionUsageReportItemResponse::totalUsedMinutes))
        .orElse(null);

    int lowUsageSubscriptionCount = (int) items.stream()
        .map(SubscriptionUsageReportItemResponse::nudgeMessage)
        .filter(Objects::nonNull)
        .filter(message -> NUDGE_KEEP_PAYING.equals(message) || NUDGE_SLOWING_DOWN.equals(message))
        .count();

    SubscriptionUsageReportSummaryResponse summary = new SubscriptionUsageReportSummaryResponse(
        windowDays,
        totalUsedMinutes,
        subscriptions.size(),
        lowUsageSubscriptionCount,
        mostUsedItem == null ? null : mostUsedItem.serviceName(),
        mostUsedItem == null ? 0 : mostUsedItem.totalUsedMinutes());

    return new SubscriptionUsageReportResponse(
        summary,
        buildRelatedInsights(items),
        items);
  }

  @Transactional(readOnly = true)
  public SubscriptionUsageDailyResponse getDailyUsage(Long currentUserId, Integer days, Long subscriptionId) {
    int windowDays = normalizeDays(days);
    LocalDate endDate = LocalDate.now();
    LocalDate startDate = endDate.minusDays(windowDays - 1L);
    List<SubscriptionUsageDaily> usages;

    Set<Long> targetServiceIds = null;
    Long targetSubscriptionId = null;
    if (subscriptionId != null) {
      Subscription subscription = subscriptionRepository.findBySubscriptionId(subscriptionId)
          .filter(found -> found.getUserId().equals(currentUserId))
          .orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_NOT_FOUND));
      List<BundleServiceMap> bundleMappings = subscription.getBundle() == null
          ? List.of()
          : bundleServiceMapRepository.findByBundle_BundleIdOrderByService_ServiceIdAsc(
              subscription.getBundle().getBundleId());
      targetServiceIds = resolveCoveredServices(subscription, bundleMappings).stream()
          .map(Service::getServiceId)
          .collect(Collectors.toSet());
      targetSubscriptionId = subscription.getSubscriptionId();
      final Set<Long> resolvedTargetServiceIds = targetServiceIds;

      List<SubscriptionUsageDaily> allUsages = subscriptionUsageDailyRepository
          .findAllByUserIdOrderByUsageDateAscService_ServiceIdAsc(DEFAULT_USER_ID);

      List<SubscriptionUsageDaily> filteredUsages = allUsages.stream()
          .filter(usage -> resolvedTargetServiceIds.contains(usage.getService().getServiceId()))
          .toList();

      if (filteredUsages.isEmpty()) {
        return new SubscriptionUsageDailyResponse(0, targetSubscriptionId, List.of());
      }

      startDate = filteredUsages.get(0).getUsageDate();
      windowDays = Math.toIntExact(ChronoUnit.DAYS.between(startDate, endDate) + 1);
      usages = filteredUsages;
    } else {
      usages = subscriptionUsageDailyRepository
          .findAllByUserIdAndUsageDateBetweenOrderByUsageDateAscService_ServiceIdAsc(
              DEFAULT_USER_ID,
              startDate,
              endDate);
    }

    Map<LocalDate, DailyUsageAggregate> dailyAggregates = initializeDailyAggregates(startDate, endDate);
    for (SubscriptionUsageDaily usage : usages) {
      if (usage.getUsageDate().isBefore(startDate) || usage.getUsageDate().isAfter(endDate)) {
        continue;
      }
      dailyAggregates.computeIfPresent(
          usage.getUsageDate(),
          (date, aggregate) -> aggregate.add(usage.getService().getCategory(), usage.getUsedMinutes()));
    }

    List<SubscriptionUsageDailyPointResponse> items = dailyAggregates.entrySet().stream()
        .map(entry -> toDailyPointResponse(entry.getKey(), entry.getValue()))
        .toList();

    return new SubscriptionUsageDailyResponse(windowDays, targetSubscriptionId, items);
  }

  private SubscriptionUsageReportItemResponse toReportItem(
      Subscription subscription,
      Map<Long, List<BundleServiceMap>> bundleMappingsByBundleId,
      Map<Long, List<SubscriptionUsageDaily>> usagesByServiceId) {
    Long bundleId = subscription.getBundle() == null ? null : subscription.getBundle().getBundleId();
    List<BundleServiceMap> bundleMappings = bundleId == null
        ? List.of()
        : bundleMappingsByBundleId.getOrDefault(bundleId, List.of());

    List<Service> coveredServices = resolveCoveredServices(
        subscription,
        bundleMappings);

    List<SubscriptionUsageDaily> aggregatedUsages = coveredServices.stream()
        .map(Service::getServiceId)
        .map(usagesByServiceId::get)
        .filter(Objects::nonNull)
        .flatMap(Collection::stream)
        .toList();

    int totalUsedMinutes = aggregatedUsages.stream()
        .mapToInt(SubscriptionUsageDaily::getUsedMinutes)
        .sum();

    int usedDays = (int) aggregatedUsages.stream()
        .filter(usage -> usage.getUsedMinutes() > 0)
        .map(SubscriptionUsageDaily::getUsageDate)
        .distinct()
        .count();

    LocalDate lastUsedDate = aggregatedUsages.stream()
        .filter(usage -> usage.getUsedMinutes() > 0)
        .map(SubscriptionUsageDaily::getUsageDate)
        .max(LocalDate::compareTo)
        .orElse(null);

    Integer monthlyPrice = resolveMonthlyPrice(subscription);
    Integer hourlyCost = calculateHourlyCost(monthlyPrice, totalUsedMinutes);

    return new SubscriptionUsageReportItemResponse(
        subscription.getSubscriptionId(),
        subscription.getSubscriptionType().name(),
        subscription.getService() == null ? null : subscription.getService().getServiceId(),
        subscription.getBundle() == null ? null : subscription.getBundle().getCode(),
        resolveDisplayName(subscription),
        resolvePlanName(subscription),
        resolveCategory(subscription),
        resolveLogoUrl(subscription),
        monthlyPrice,
        totalUsedMinutes,
        usedDays,
        lastUsedDate,
        hourlyCost,
        resolveNudgeMessage(subscription, totalUsedMinutes, usedDays));
  }

  private Map<Long, List<BundleServiceMap>> mapBundleServices(List<Subscription> subscriptions) {
    List<Long> bundleIds = subscriptions.stream()
        .map(Subscription::getBundle)
        .filter(Objects::nonNull)
        .map(bundle -> bundle.getBundleId())
        .distinct()
        .toList();

    if (bundleIds.isEmpty()) {
      return Map.of();
    }

    return bundleServiceMapRepository.findByBundle_BundleIdIn(bundleIds).stream()
        .collect(Collectors.groupingBy(mapping -> mapping.getBundle().getBundleId()));
  }

  private List<Service> resolveCoveredServices(Subscription subscription, List<BundleServiceMap> bundleMappings) {
    if (subscription.getSubscriptionType() == SubscriptionType.BUNDLE && subscription.getBundle() != null) {
      return bundleMappings.stream()
          .map(BundleServiceMap::getService)
          .toList();
    }

    if (subscription.getService() == null) {
      return List.of();
    }
    return List.of(subscription.getService());
  }

  private Integer resolveMonthlyPrice(Subscription subscription) {
    if (subscription.getBundle() != null) {
      return subscription.getBundle().getMonthlyPrice();
    }
    ServicePlan servicePlan = subscription.getServicePlan();
    return servicePlan == null ? null : servicePlan.getMonthlyPrice();
  }

  private Integer calculateHourlyCost(Integer monthlyPrice, int totalUsedMinutes) {
    if (monthlyPrice == null || totalUsedMinutes < 60) {
      return null;
    }
    return (int) Math.round(monthlyPrice * 60.0 / totalUsedMinutes);
  }

  private String resolveDisplayName(Subscription subscription) {
    if (subscription.getBundle() != null) {
      return subscription.getBundle().getName();
    }
    return subscription.getService().getName();
  }

  private String resolvePlanName(Subscription subscription) {
    if (subscription.getBundle() != null) {
      return subscription.getBundle().getPlanName();
    }
    return subscription.getServicePlan().getPlanName();
  }

  private String resolveCategory(Subscription subscription) {
    if (subscription.getBundle() != null) {
      return SubscriptionType.BUNDLE.name();
    }
    return subscription.getService().getCategory().name();
  }

  private String resolveLogoUrl(Subscription subscription) {
    if (subscription.getBundle() != null) {
      return subscription.getBundle().getLogoUrl();
    }
    return subscription.getService().getLogoUrl();
  }

  private String resolveNudgeMessage(Subscription subscription, int totalUsedMinutes, int usedDays) {
    long daysUntilBilling = subscription.getNextBillingDate() == null
        ? Long.MAX_VALUE
        : ChronoUnit.DAYS.between(LocalDate.now(), subscription.getNextBillingDate());

    if (totalUsedMinutes == 0 || (usedDays <= 1 && daysUntilBilling <= 7)) {
      return NUDGE_KEEP_PAYING;
    }
    if (usedDays <= 3 || totalUsedMinutes <= 30) {
      return NUDGE_SLOWING_DOWN;
    }
    if (usedDays >= 12 || totalUsedMinutes >= 600) {
      return NUDGE_FREQUENT;
    }
    return null;
  }

  private List<SubscriptionUsageReportInsightResponse> buildRelatedInsights(
      List<SubscriptionUsageReportItemResponse> items) {
    Map<String, List<SubscriptionUsageReportItemResponse>> itemsByCategory = items.stream()
        .filter(item -> SubscriptionType.SINGLE.name().equals(item.subscriptionType()))
        .filter(item -> item.category() != null)
        .collect(Collectors.groupingBy(SubscriptionUsageReportItemResponse::category));

    List<SubscriptionUsageReportInsightResponse> insights = new ArrayList<>();
    for (List<SubscriptionUsageReportItemResponse> categoryItems : itemsByCategory.values()) {
      List<SubscriptionUsageReportItemResponse> sortedItems = categoryItems.stream()
          .sorted(Comparator.comparingInt(SubscriptionUsageReportItemResponse::totalUsedMinutes).reversed())
          .toList();

      if (sortedItems.size() < 2) {
        continue;
      }

      SubscriptionUsageReportItemResponse first = sortedItems.get(0);
      SubscriptionUsageReportItemResponse second = sortedItems.get(1);
      if (first.totalUsedMinutes() >= second.totalUsedMinutes() + 60
          && first.totalUsedMinutes() >= Math.max(1, second.totalUsedMinutes()) * 1.5) {
        insights.add(new SubscriptionUsageReportInsightResponse(
            second.serviceName() + "보다 " + first.serviceName() + "을 더 자주 쓰고 있어요."));
      }
    }

    return insights.stream()
        .limit(3)
        .toList();
  }

  private SubscriptionUsageDailyPointResponse toDailyPointResponse(
      LocalDate usageDate,
      DailyUsageAggregate aggregate) {
    return new SubscriptionUsageDailyPointResponse(
        usageDate,
        aggregate.totalUsedMinutes(),
        aggregate.ottUsedMinutes(),
        aggregate.musicUsedMinutes(),
        aggregate.aiUsedMinutes(),
        aggregate.dominantCategory());
  }

  private Map<LocalDate, DailyUsageAggregate> initializeDailyAggregates(LocalDate startDate, LocalDate endDate) {
    Map<LocalDate, DailyUsageAggregate> dailyAggregates = new LinkedHashMap<>();
    LocalDate cursor = startDate;
    while (!cursor.isAfter(endDate)) {
      dailyAggregates.put(cursor, DailyUsageAggregate.empty());
      cursor = cursor.plusDays(1);
    }
    return dailyAggregates;
  }

  private int normalizeDays(Integer days) {
    if (days == null) {
      return DEFAULT_WINDOW_DAYS;
    }
    if (days < 1 || days > 365) {
      throw new BusinessException(ErrorCode.INVALID_INPUT, "days는 1 이상 365 이하여야 합니다.");
    }
    return days;
  }

  private record DailyUsageAggregate(
      int totalUsedMinutes,
      int ottUsedMinutes,
      int musicUsedMinutes,
      int aiUsedMinutes) {

    private static DailyUsageAggregate empty() {
      return new DailyUsageAggregate(0, 0, 0, 0);
    }

    private DailyUsageAggregate add(ServiceCategory category, int usedMinutes) {
      if (category == null || usedMinutes <= 0) {
        return this;
      }

      return switch (category) {
        case OTT -> new DailyUsageAggregate(
            totalUsedMinutes + usedMinutes,
            ottUsedMinutes + usedMinutes,
            musicUsedMinutes,
            aiUsedMinutes);
        case MUSIC -> new DailyUsageAggregate(
            totalUsedMinutes + usedMinutes,
            ottUsedMinutes,
            musicUsedMinutes + usedMinutes,
            aiUsedMinutes);
        case AI -> new DailyUsageAggregate(
            totalUsedMinutes + usedMinutes,
            ottUsedMinutes,
            musicUsedMinutes,
            aiUsedMinutes + usedMinutes);
      };
    }

    private ServiceCategory dominantCategory() {
      if (totalUsedMinutes <= 0) {
        return null;
      }
      if (ottUsedMinutes >= musicUsedMinutes && ottUsedMinutes >= aiUsedMinutes) {
        return ServiceCategory.OTT;
      }
      if (musicUsedMinutes >= aiUsedMinutes) {
        return ServiceCategory.MUSIC;
      }
      return ServiceCategory.AI;
    }
  }
}
