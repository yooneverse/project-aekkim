package com.ssafy.e106.domain.admin.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.e106.domain.admin.dto.response.AdminUserViewResponse;
import com.ssafy.e106.domain.admin.dto.response.AdminBundleServiceViewResponse;
import com.ssafy.e106.domain.admin.dto.response.AdminBundleViewResponse;
import com.ssafy.e106.domain.admin.dto.response.AdminPromotionServiceViewResponse;
import com.ssafy.e106.domain.admin.dto.response.AdminPromotionViewResponse;
import com.ssafy.e106.domain.admin.dto.response.AdminServiceCatalogViewResponse;
import com.ssafy.e106.domain.admin.dto.response.AdminServicePlanViewResponse;
import com.ssafy.e106.domain.admin.dto.response.AdminSubscriptionViewResponse;
import com.ssafy.e106.domain.auth.entity.User;
import com.ssafy.e106.domain.auth.repository.UserRepository;
import com.ssafy.e106.domain.admin.dto.response.AdminSubscriptionUsageViewResponse;
import com.ssafy.e106.domain.promotion.entity.Promotion;
import com.ssafy.e106.domain.promotion.entity.PromotionServiceMapping;
import com.ssafy.e106.domain.promotion.repository.PromotionRepository;
import com.ssafy.e106.domain.promotion.repository.PromotionServiceMappingRepository;
import com.ssafy.e106.domain.subscription.entity.Bundle;
import com.ssafy.e106.domain.subscription.entity.BundleServiceMap;
import com.ssafy.e106.domain.subscription.entity.Service;
import com.ssafy.e106.domain.subscription.entity.ServicePlan;
import com.ssafy.e106.domain.subscription.entity.Subscription;
import com.ssafy.e106.domain.subscription.repository.BundleRepository;
import com.ssafy.e106.domain.subscription.repository.BundleServiceMapRepository;
import com.ssafy.e106.domain.subscription.repository.ServicePlanRepository;
import com.ssafy.e106.domain.subscription.repository.ServiceRepository;
import com.ssafy.e106.domain.subscription.repository.SubscriptionRepository;
import com.ssafy.e106.domain.subscriptionusage.entity.SubscriptionUsageDaily;
import com.ssafy.e106.domain.subscriptionusage.repository.SubscriptionUsageDailyRepository;

import lombok.RequiredArgsConstructor;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class AdminCatalogQueryService {

    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final ServicePlanRepository servicePlanRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final BundleRepository bundleRepository;
    private final BundleServiceMapRepository bundleServiceMapRepository;
    private final PromotionRepository promotionRepository;
    private final PromotionServiceMappingRepository promotionServiceMappingRepository;
    private final SubscriptionUsageDailyRepository subscriptionUsageDailyRepository;

    @Transactional(readOnly = true)
    public List<AdminUserViewResponse> getUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "userId"))
                .stream()
                .map(this::toUserViewResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminServiceCatalogViewResponse> getServices() {
        List<Service> services = serviceRepository.findAllByOrderByServiceIdAsc();
        Map<Long, List<AdminServicePlanViewResponse>> plansByServiceId = servicePlanRepository.findAllByOrderByServicePlanIdAsc()
                .stream()
                .collect(Collectors.groupingBy(
                        servicePlan -> servicePlan.getService().getServiceId(),
                        Collectors.mapping(this::toServicePlanViewResponse, Collectors.toList())));

        return services.stream()
                .map(service -> new AdminServiceCatalogViewResponse(
                        service.getServiceId(),
                        service.getCode(),
                        service.getName(),
                        service.getCategory().name(),
                        service.getLogoUrl(),
                        plansByServiceId.getOrDefault(service.getServiceId(), List.of()).stream()
                                .sorted(Comparator.comparing(AdminServicePlanViewResponse::servicePlanId))
                                .toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminBundleViewResponse> getBundles() {
        List<Bundle> bundles = bundleRepository.findAllByOrderByBundleIdAsc();
        List<Long> bundleIds = bundles.stream()
                .map(Bundle::getBundleId)
                .toList();

        Map<Long, List<AdminBundleServiceViewResponse>> servicesByBundleId =
                bundleIds.isEmpty()
                        ? Map.of()
                        : bundleServiceMapRepository.findByBundle_BundleIdIn(bundleIds)
                                .stream()
                                .collect(Collectors.groupingBy(
                                        mapping -> mapping.getBundle().getBundleId(),
                                        Collectors.mapping(this::toBundleServiceViewResponse, Collectors.toList())));

        return bundles.stream()
                .map(bundle -> new AdminBundleViewResponse(
                        bundle.getBundleId(),
                        bundle.getCode(),
                        bundle.getName(),
                        bundle.getPlanName(),
                        bundle.getBillingCycle(),
                        bundle.getMonthlyPrice(),
                        bundle.getOriginalPrice(),
                        bundle.getLogoUrl(),
                        bundle.getSourceUrl(),
                        servicesByBundleId.getOrDefault(bundle.getBundleId(), List.of()).stream()
                                .sorted(Comparator.comparing(AdminBundleServiceViewResponse::serviceId))
                                .toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminSubscriptionViewResponse> getSubscriptions(Long userId) {
        List<Subscription> subscriptions = userId == null
                ? subscriptionRepository.findAllByOrderBySubscriptionIdDesc()
                : subscriptionRepository.findAllByUserIdOrderBySubscriptionIdDesc(userId);

        return subscriptions.stream()
                .map(this::toSubscriptionViewResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminPromotionViewResponse> getPromotions() {
        List<Promotion> promotions = promotionRepository.findAllByOrderByPromotionIdDesc();
        List<Long> promotionIds = promotions.stream()
                .map(Promotion::getPromotionId)
                .toList();

        Map<Long, List<AdminPromotionServiceViewResponse>> servicesByPromotionId =
                promotionIds.isEmpty()
                        ? Map.of()
                        : promotionServiceMappingRepository.findByPromotionPromotionIdIn(promotionIds)
                                .stream()
                                .collect(Collectors.groupingBy(
                                        mapping -> mapping.getPromotion().getPromotionId(),
                                        Collectors.mapping(this::toPromotionServiceViewResponse, Collectors.toList())));

        return promotions.stream()
                .map(promotion -> new AdminPromotionViewResponse(
                        promotion.getPromotionId(),
                        promotion.getPromotionType().name(),
                        promotion.getTitle(),
                        promotion.getOriginalPrice(),
                        promotion.getDiscountPrice(),
                        promotion.getBillingCycle(),
                        promotion.getStartsAt(),
                        promotion.getEndsAt(),
                        promotion.getImageUrl(),
                        servicesByPromotionId.getOrDefault(promotion.getPromotionId(), List.of()).stream()
                                .sorted(Comparator.comparing(AdminPromotionServiceViewResponse::serviceId))
                                .toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminSubscriptionUsageViewResponse> getSubscriptionUsages() {
        return subscriptionUsageDailyRepository.findAllByOrderByUsageDateDescUsageIdDesc().stream()
                .map(this::toSubscriptionUsageViewResponse)
                .toList();
    }

    private AdminServicePlanViewResponse toServicePlanViewResponse(ServicePlan servicePlan) {
        return new AdminServicePlanViewResponse(
                servicePlan.getServicePlanId(),
                servicePlan.getPlanName(),
                servicePlan.getBillingCycle(),
                servicePlan.getMonthlyPrice());
    }

    private AdminBundleServiceViewResponse toBundleServiceViewResponse(BundleServiceMap mapping) {
        Service service = mapping.getService();
        return new AdminBundleServiceViewResponse(
                service.getServiceId(),
                service.getCode(),
                service.getName());
    }

    private AdminPromotionServiceViewResponse toPromotionServiceViewResponse(PromotionServiceMapping mapping) {
        Service service = mapping.getService();
        return new AdminPromotionServiceViewResponse(
                service.getServiceId(),
                service.getCode(),
                service.getName());
    }

    private AdminUserViewResponse toUserViewResponse(User user) {
        return new AdminUserViewResponse(
                user.getUserId(),
                user.getProvider().name(),
                user.getProviderUserId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getCheckinAlertEnabled(),
                user.getPromoAlertEnabled(),
                user.getOptionalConsentAgreed(),
                user.getConnectedAt(),
                user.getLastLoginAt());
    }

    private AdminSubscriptionViewResponse toSubscriptionViewResponse(Subscription subscription) {
        return new AdminSubscriptionViewResponse(
            subscription.getSubscriptionId(),
            subscription.getUserId(),
            subscription.getSubscriptionType().name(),
            subscription.getService() == null ? null : subscription.getService().getServiceId(),
            subscription.getService() == null ? null : subscription.getService().getCode(),
            subscription.getService() == null ? null : subscription.getService().getName(),
            subscription.getServicePlan() == null ? null
                : subscription.getServicePlan().getServicePlanId(),
            subscription.getServicePlan() == null ? null
                : subscription.getServicePlan().getPlanName(),
            subscription.getBundle() != null
                ? subscription.getBundle().getBillingCycle()
                : subscription.getServicePlan() == null ? null
                    : subscription.getServicePlan().getBillingCycle(),
            subscription.getBundle() == null ? null : subscription.getBundle().getBundleId(),
            subscription.getBundle() == null ? null : subscription.getBundle().getCode(),
            subscription.getBundle() == null ? null : subscription.getBundle().getName(),
            subscription.getNextBillingDate());
    }

    private AdminSubscriptionUsageViewResponse toSubscriptionUsageViewResponse(SubscriptionUsageDaily usage) {
        Service service = usage.getService();
        return new AdminSubscriptionUsageViewResponse(
                usage.getUsageId(),
                usage.getUserId(),
                service.getServiceId(),
                service.getCode(),
                service.getName(),
                usage.getUsageDate(),
                usage.getUsedMinutes());
    }
}
