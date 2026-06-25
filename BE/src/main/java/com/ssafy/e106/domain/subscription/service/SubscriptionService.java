package com.ssafy.e106.domain.subscription.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.e106.domain.promotion.dto.response.PromotionRecommendationSummary;
import com.ssafy.e106.domain.promotion.service.PromotionService;
import com.ssafy.e106.domain.subscription.dto.request.SubscriptionCreateRequest;
import com.ssafy.e106.domain.subscription.dto.request.SubscriptionUpdateRequest;
import com.ssafy.e106.domain.subscription.dto.request.SubscriptionUsageQualificationItemRequest;
import com.ssafy.e106.domain.subscription.dto.request.SubscriptionUsageQualificationRequest;
import com.ssafy.e106.domain.subscription.dto.response.ServiceDetailResponse;
import com.ssafy.e106.domain.subscription.dto.response.ServiceListItemResponse;
import com.ssafy.e106.domain.subscription.dto.response.ServiceListResponse;
import com.ssafy.e106.domain.subscription.dto.response.ServicePlanResponse;
import com.ssafy.e106.domain.subscription.dto.response.BundleDetailResponse;
import com.ssafy.e106.domain.subscription.dto.response.BundleListItemResponse;
import com.ssafy.e106.domain.subscription.dto.response.BundleListResponse;
import com.ssafy.e106.domain.subscription.dto.response.SubscriptionCoveredServiceResponse;
import com.ssafy.e106.domain.subscription.dto.response.SubscriptionCreateResponse;
import com.ssafy.e106.domain.subscription.dto.response.SubscriptionDetailResponse;
import com.ssafy.e106.domain.subscription.dto.response.SubscriptionListItemResponse;
import com.ssafy.e106.domain.subscription.dto.response.SubscriptionListResponse;
import com.ssafy.e106.domain.subscription.dto.response.SubscriptionRecentUsageResponse;
import com.ssafy.e106.domain.subscription.dto.response.SubscriptionRecommendationResponse;
import com.ssafy.e106.domain.subscription.dto.response.SubscriptionUpdateResponse;
import com.ssafy.e106.domain.subscription.dto.response.SubscriptionUsageQualificationResponse;
import com.ssafy.e106.domain.subscription.entity.Bundle;
import com.ssafy.e106.domain.subscription.entity.BundleServiceMap;
import com.ssafy.e106.domain.subscription.entity.CheckinRecord;
import com.ssafy.e106.domain.subscription.entity.Service;
import com.ssafy.e106.domain.subscription.entity.ServicePlan;
import com.ssafy.e106.domain.subscription.entity.Subscription;
import com.ssafy.e106.domain.subscription.enums.ServiceCategory;
import com.ssafy.e106.domain.subscription.enums.SubscriptionType;
import com.ssafy.e106.domain.subscription.repository.BundleRepository;
import com.ssafy.e106.domain.subscription.repository.BundleServiceMapRepository;
import com.ssafy.e106.domain.subscription.repository.CheckinRecordRepository;
import com.ssafy.e106.domain.subscription.repository.ServicePlanRepository;
import com.ssafy.e106.domain.subscription.repository.ServiceRepository;
import com.ssafy.e106.domain.subscription.repository.SubscriptionRepository;
import com.ssafy.e106.global.exception.BusinessException;
import com.ssafy.e106.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class SubscriptionService {

  private final ServiceRepository serviceRepository;
  private final ServicePlanRepository servicePlanRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final BundleRepository bundleRepository;
  private final BundleServiceMapRepository bundleServiceMapRepository;
  private final CheckinRecordRepository checkinRecordRepository;
  private final PromotionService promotionService;

  @Transactional(readOnly = true)
  public ServiceListResponse getServices(String category) {
    ServiceCategory serviceCategory = parseCategory(category);

    List<ServiceListItemResponse> services = serviceRepository
        .findAllByCategoryOrderByNameAsc(serviceCategory)
        .stream()
        .map(ServiceListItemResponse::of)
        .toList();

    return new ServiceListResponse(serviceCategory.name(), services);
  }

  @Transactional(readOnly = true)
  public ServiceDetailResponse getServicePlans(Long serviceId) {
    Service service = serviceRepository.findById(serviceId)
        .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_NOT_FOUND));

    List<ServicePlanResponse> plans = servicePlanRepository
        .findAllByService_ServiceIdOrderByMonthlyPriceAscServicePlanIdAsc(serviceId)
        .stream()
        .map(ServicePlanResponse::from)
        .toList();

    return ServiceDetailResponse.of(service, plans);
  }

  @Transactional(readOnly = true)
  public BundleListResponse getBundles() {
    List<BundleListItemResponse> bundles = bundleRepository.findAllByOrderByBundleIdAsc()
        .stream()
        .map(bundle -> BundleListItemResponse.of(bundle, getCoveredServices(bundle)))
        .toList();

    return new BundleListResponse(bundles);
  }

  @Transactional(readOnly = true)
  public BundleDetailResponse getBundle(String bundleCode) {
    Bundle bundle = bundleRepository.findByCode(normalizeRequired(bundleCode, "bundleCode"))
        .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_NOT_FOUND));

    return BundleDetailResponse.of(bundle, getCoveredServices(bundle));
  }

  @Transactional
  public SubscriptionCreateResponse createSubscription(
      Long userId,
      SubscriptionCreateRequest request) {
    SubscriptionType subscriptionType = parseSubscriptionType(request.subscriptionType());
    LocalDate nextBillingDate = request.nextBillingDate();

    Subscription subscription;
    if (subscriptionType == SubscriptionType.BUNDLE) {
      Bundle bundle = bundleRepository.findByCode(normalizeRequired(request.bundleCode(), "bundleCode"))
          .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_NOT_FOUND));
      Service representativeService = resolveBundleRepresentativeService(bundle);

      if (subscriptionRepository.existsByUserIdAndBundle_BundleId(userId, bundle.getBundleId())) {
        throw new BusinessException(ErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
      }

      subscription = subscriptionRepository.save(Subscription.builder()
          .userId(userId)
          .subscriptionType(subscriptionType)
          .service(representativeService)
          .bundle(bundle)
          .nextBillingDate(nextBillingDate)
          .build());
    } else {
      Service service = serviceRepository.findById(required(request.serviceId(), "serviceId"))
          .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_NOT_FOUND));

      ServicePlan servicePlan = servicePlanRepository
          .findByServicePlanIdAndService_ServiceId(
              required(request.servicePlanId(), "servicePlanId"),
              service.getServiceId())
          .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_PLAN_NOT_FOUND));

      if (subscriptionRepository.existsByUserIdAndService_ServiceId(userId, service.getServiceId())) {
        throw new BusinessException(ErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
      }

      subscription = subscriptionRepository.save(Subscription.builder()
          .userId(userId)
          .subscriptionType(subscriptionType)
          .service(service)
          .servicePlan(servicePlan)
          .nextBillingDate(nextBillingDate)
          .build());
    }

    return SubscriptionCreateResponse.of(subscription);
  }

  @Transactional(readOnly = true)
  public SubscriptionListResponse getSubscriptions(Long userId) {
    List<SubscriptionListItemResponse> subscriptions = subscriptionRepository
        .findAllByUserIdOrderBySubscriptionIdDesc(userId)
        .stream()
        .map(subscription -> SubscriptionListItemResponse.of(
            subscription,
            resolveRepresentativeServiceId(subscription),
            getCoveredServices(subscription)))
        .toList();

    int monthlyTotalAmount = subscriptions.stream()
        .mapToInt(SubscriptionListItemResponse::monthlyPrice)
        .sum();

    return new SubscriptionListResponse(monthlyTotalAmount, subscriptions);
  }

  @Transactional(readOnly = true)
  public SubscriptionDetailResponse getSubscription(Long userId, Long subscriptionId) {
    Subscription subscription = subscriptionRepository.findBySubscriptionId(subscriptionId)
        .orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

    if (!subscription.getUserId().equals(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    Service representativeService = resolveRepresentativeService(subscription);
    List<SubscriptionCoveredServiceResponse> coveredServices = getCoveredServices(subscription);

    List<SubscriptionRecentUsageResponse> recentUsage = getRecentUsage(userId, subscription).stream()
        .map(SubscriptionRecentUsageResponse::from)
        .toList();

    PromotionRecommendationSummary recommendationSummary =
        representativeService == null ? null : promotionService.getTopRecommendationForSubscription(userId, subscription);
    SubscriptionRecommendationResponse recommendation = recommendationSummary == null
        ? null
        : SubscriptionRecommendationResponse.from(recommendationSummary);

    return SubscriptionDetailResponse.of(
        subscription,
        subscription.getBundle() == null ? representativeService.getName() : subscription.getBundle().getName(),
        subscription.getBundle() == null ? subscription.getServicePlan().getPlanName() : subscription.getBundle().getPlanName(),
        subscription.getBundle() == null ? representativeService.getLogoUrl() : subscription.getBundle().getLogoUrl(),
        subscription.getBundle() == null ? subscription.getServicePlan().getMonthlyPrice() : subscription.getBundle().getMonthlyPrice(),
        subscription.getBundle() == null ? null : subscription.getBundle().getOriginalPrice(),
        subscription.getBundle() == null ? subscription.getServicePlan().getBillingCycle() : subscription.getBundle().getBillingCycle(),
        calculateDaysUntilBilling(subscription.getNextBillingDate()),
        coveredServices,
        recentUsage,
        recommendation);
  }

  @Transactional
  public SubscriptionUpdateResponse updateSubscription(
      Long userId,
      Long subscriptionId,
      SubscriptionUpdateRequest request) {
    if (request.servicePlanId() == null && request.bundleCode() == null && request.nextBillingDate() == null) {
      throw new BusinessException(ErrorCode.INVALID_INPUT, "수정할 항목이 없습니다.");
    }

    Subscription subscription = subscriptionRepository.findBySubscriptionId(subscriptionId)
        .orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

    if (!subscription.getUserId().equals(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    SubscriptionType subscriptionType = subscription.getSubscriptionType();
    Service service = subscription.getService();
    ServicePlan servicePlan = subscription.getServicePlan();
    Bundle bundle = subscription.getBundle();

    if (subscriptionType == SubscriptionType.BUNDLE) {
      if (request.bundleCode() != null && !request.bundleCode().isBlank()) {
        bundle = bundleRepository.findByCode(request.bundleCode().trim())
            .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_NOT_FOUND));
      }
      service = resolveBundleRepresentativeService(bundle);
      servicePlan = null;
    } else if (request.servicePlanId() != null) {
      servicePlan = servicePlanRepository
          .findByServicePlanIdAndService_ServiceId(
              request.servicePlanId(),
              subscription.getService().getServiceId())
          .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_PLAN_NOT_FOUND));
    }

    LocalDate nextBillingDate = request.nextBillingDate() != null
        ? request.nextBillingDate()
        : subscription.getNextBillingDate();

    subscription.update(subscriptionType, service, servicePlan, bundle, nextBillingDate);

    Subscription updatedSubscription = subscriptionRepository.saveAndFlush(subscription);
    return SubscriptionUpdateResponse.of(updatedSubscription);
  }

  @Transactional
  public SubscriptionUsageQualificationResponse updateUsageQualification(
      Long userId,
      SubscriptionUsageQualificationRequest request) {
    for (SubscriptionUsageQualificationItemRequest item : request.items()) {
      Subscription subscription = subscriptionRepository.findBySubscriptionId(item.subscriptionId())
          .orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

      if (!subscription.getUserId().equals(userId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN);
      }

      subscription.updateLowUsageStatus(item.lowUsageDetected(), item.cycleYm());
    }

    return new SubscriptionUsageQualificationResponse(request.items().size());
  }

  @Transactional
  public void deleteSubscription(Long userId, Long subscriptionId) {
    Subscription subscription = subscriptionRepository.findBySubscriptionId(subscriptionId)
        .orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

    if (!subscription.getUserId().equals(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    subscriptionRepository.delete(subscription);
  }

  private List<SubscriptionCoveredServiceResponse> getCoveredServices(Subscription subscription) {
    if (subscription.getBundle() == null) {
      return List.of(SubscriptionCoveredServiceResponse.from(subscription.getService()));
    }

    return getCoveredServices(subscription.getBundle());
  }

  private List<SubscriptionCoveredServiceResponse> getCoveredServices(Bundle bundle) {
    return bundleServiceMapRepository.findByBundle_BundleIdOrderByService_ServiceIdAsc(bundle.getBundleId())
        .stream()
        .map(BundleServiceMap::getService)
        .map(SubscriptionCoveredServiceResponse::from)
        .toList();
  }

  private Service resolveRepresentativeService(Subscription subscription) {
    if (subscription.getService() != null) {
      return subscription.getService();
    }

    if (subscription.getBundle() == null) {
      return null;
    }

    return bundleServiceMapRepository.findByBundle_BundleIdOrderByService_ServiceIdAsc(subscription.getBundle().getBundleId())
        .stream()
        .map(BundleServiceMap::getService)
        .findFirst()
        .orElse(null);
  }

  private Service resolveBundleRepresentativeService(Bundle bundle) {
    return bundleServiceMapRepository.findByBundle_BundleIdOrderByService_ServiceIdAsc(bundle.getBundleId())
        .stream()
        .map(BundleServiceMap::getService)
        .findFirst()
        .orElseThrow(() -> new BusinessException(
            ErrorCode.SERVICE_NOT_FOUND,
            "bundle has no mapped services. bundleCode=" + bundle.getCode()));
  }

  private Long resolveRepresentativeServiceId(Subscription subscription) {
    Service representativeService = resolveRepresentativeService(subscription);
    return representativeService == null ? null : representativeService.getServiceId();
  }

  private List<CheckinRecord> getRecentUsage(Long userId, Subscription subscription) {
    List<Long> serviceIds = getCoveredServices(subscription).stream()
        .map(SubscriptionCoveredServiceResponse::serviceId)
        .toList();
    if (serviceIds.isEmpty()) {
      return List.of();
    }

    return serviceIds.stream()
        .flatMap(serviceId -> checkinRecordRepository.findHistory(
            userId,
            serviceId,
            null,
            null,
            PageRequest.of(0, 3)).stream())
        .sorted(Comparator.comparing(CheckinRecord::getRespondedAt).reversed())
        .limit(3)
        .toList();
  }

  private ServiceCategory parseCategory(String category) {
    if (category == null || category.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT, "category는 필수입니다.");
    }

    try {
      return ServiceCategory.from(category);
    } catch (IllegalArgumentException e) {
      throw new BusinessException(ErrorCode.INVALID_INPUT, "지원하지 않는 category입니다.");
    }
  }

  private SubscriptionType parseSubscriptionType(String subscriptionType) {
    try {
      return SubscriptionType.from(subscriptionType);
    } catch (IllegalArgumentException e) {
      throw new BusinessException(ErrorCode.INVALID_INPUT, "subscriptionType은 SINGLE 또는 BUNDLE이어야 합니다.");
    }
  }

  private Long required(Long value, String fieldName) {
    if (value == null) {
      throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + "는 필수입니다.");
    }
    return value;
  }

  private String normalizeRequired(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + "는 필수입니다.");
    }
    return value.trim();
  }

  private Integer calculateDaysUntilBilling(LocalDate nextBillingDate) {
    if (nextBillingDate == null) {
      return null;
    }
    return Math.toIntExact(ChronoUnit.DAYS.between(LocalDate.now(), nextBillingDate));
  }
}
