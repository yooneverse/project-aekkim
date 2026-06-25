package com.ssafy.e106.domain.admin.service;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.e106.domain.admin.dto.request.BundleSeedItem;
import com.ssafy.e106.domain.admin.dto.request.BundleSeedPayload;
import com.ssafy.e106.domain.admin.dto.request.PromotionSeedItem;
import com.ssafy.e106.domain.admin.dto.request.PromotionSeedPayload;
import com.ssafy.e106.domain.admin.dto.request.ServiceCatalogSeedPayload;
import com.ssafy.e106.domain.admin.dto.request.ServicePlanSeedItem;
import com.ssafy.e106.domain.admin.dto.request.ServiceSeedItem;
import com.ssafy.e106.domain.admin.dto.request.SubscriptionUsageSeedItem;
import com.ssafy.e106.domain.admin.dto.request.SubscriptionUsageSeedPayload;
import com.ssafy.e106.domain.admin.dto.response.AdminSeedImportResponse;
import com.ssafy.e106.domain.promotion.entity.Promotion;
import com.ssafy.e106.domain.promotion.entity.PromotionServiceMapping;
import com.ssafy.e106.domain.promotion.enums.PromotionType;
import com.ssafy.e106.domain.promotion.repository.PromotionRepository;
import com.ssafy.e106.domain.promotion.repository.PromotionServiceMappingRepository;
import com.ssafy.e106.domain.subscription.entity.Bundle;
import com.ssafy.e106.domain.subscription.entity.BundleServiceMap;
import com.ssafy.e106.domain.subscription.entity.Service;
import com.ssafy.e106.domain.subscription.entity.ServicePlan;
import com.ssafy.e106.domain.subscription.repository.BundleRepository;
import com.ssafy.e106.domain.subscription.repository.BundleServiceMapRepository;
import com.ssafy.e106.domain.subscription.repository.ServicePlanRepository;
import com.ssafy.e106.domain.subscription.repository.ServiceRepository;
import com.ssafy.e106.domain.subscriptionusage.entity.SubscriptionUsageDaily;
import com.ssafy.e106.domain.subscriptionusage.repository.SubscriptionUsageDailyRepository;
import com.ssafy.e106.global.exception.BusinessException;
import com.ssafy.e106.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class AdminSeedImportService {

  private final ObjectMapper objectMapper;
  private final ServiceRepository serviceRepository;
  private final ServicePlanRepository servicePlanRepository;
  private final BundleRepository bundleRepository;
  private final BundleServiceMapRepository bundleServiceMapRepository;
  private final SubscriptionUsageDailyRepository subscriptionUsageDailyRepository;
  private final PromotionRepository promotionRepository;
  private final PromotionServiceMappingRepository promotionServiceMappingRepository;

  @Transactional
  public AdminSeedImportResponse importServiceCatalog(MultipartFile file) {
    validateFile(file, "service catalog");
    ServiceCatalogSeedPayload payload = readFile(file, ServiceCatalogSeedPayload.class);

    int processedCount = 0;

    for (ServiceSeedItem item : nullSafeList(payload.services())) {
      upsertService(item);
      processedCount++;
    }

    for (ServicePlanSeedItem item : nullSafeList(payload.servicePlans())) {
      upsertServicePlan(item);
      processedCount++;
    }

    return new AdminSeedImportResponse(
        "SERVICE_CATALOG",
        List.of(file.getOriginalFilename()),
        processedCount);
  }

  @Transactional
  public AdminSeedImportResponse importPromotions(List<MultipartFile> files) {
    if (files == null || files.isEmpty()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT, "promotion import files must not be empty.");
    }

    int processedCount = 0;
    List<String> sourceFiles = files.stream()
        .peek(file -> validateFile(file, "promotion"))
        .map(MultipartFile::getOriginalFilename)
        .toList();

    for (MultipartFile file : files) {
      PromotionSeedPayload payload = readFile(file, PromotionSeedPayload.class);
      for (PromotionSeedItem item : nullSafeList(payload.promotions())) {
        upsertPromotion(item);
        if (item.promotionType() == PromotionType.BUNDLE) {
          upsertBundle(item);
        }
        processedCount++;
      }
    }

    return new AdminSeedImportResponse(
        "PROMOTION",
        sourceFiles,
        processedCount);
  }

  @Transactional
  public AdminSeedImportResponse importBundles(MultipartFile file) {
    validateFile(file, "bundle");
    BundleSeedPayload payload = readFile(file, BundleSeedPayload.class);

    validateBundleCodes(payload);

    int processedCount = 0;
    for (BundleSeedItem item : nullSafeList(payload.bundles())) {
      upsertBundle(item);
      processedCount++;
    }

    return new AdminSeedImportResponse(
        "BUNDLE",
        List.of(file.getOriginalFilename()),
        processedCount);
  }

  @Transactional
  public AdminSeedImportResponse importSubscriptionUsages(MultipartFile file) {
    validateFile(file, "subscription usage");
    SubscriptionUsageSeedPayload payload = readFile(file, SubscriptionUsageSeedPayload.class);

    int processedCount = 0;
    for (SubscriptionUsageSeedItem item : nullSafeList(payload.items())) {
      upsertSubscriptionUsage(item);
      processedCount++;
    }

    return new AdminSeedImportResponse(
        "SUBSCRIPTION_USAGE",
        List.of(file.getOriginalFilename()),
        processedCount);
  }

  private void upsertService(ServiceSeedItem item) {
    String code = normalize(item.code());

    Service service = serviceRepository.findByCode(code)
        .orElseGet(() -> serviceRepository.save(Service.builder()
            .code(code)
            .name(normalize(item.name()))
            .category(item.category())
            .logoUrl(normalizeNullable(item.logoUrl()))
            .cancelGuideUrl(normalizeNullable(item.cancelGuideUrl()))
            .customerServicePhone(normalizeNullable(item.customerServicePhone()))
            .contactEmail(normalizeNullable(item.contactEmail()))
            .build()));

    service.update(
        code,
        normalize(item.name()),
        item.category(),
        normalizeNullable(item.logoUrl()),
        normalizeNullable(item.cancelGuideUrl()),
        normalizeNullable(item.customerServicePhone()),
        normalizeNullable(item.contactEmail()));
  }

  private void upsertServicePlan(ServicePlanSeedItem item) {
    Service service = serviceRepository
        .findByCode(normalize(item.serviceCode()))
        .orElseThrow(() -> new BusinessException(
            ErrorCode.SERVICE_NOT_FOUND,
            "service code not found in seed import. code=" + item.serviceCode()));

    ServicePlan servicePlan = servicePlanRepository
        .findByService_ServiceIdAndPlanNameAndBillingCycle(
            service.getServiceId(),
            normalize(item.planName()),
            normalize(item.billingCycle()))
        .orElseGet(() -> servicePlanRepository.save(ServicePlan.builder()
            .service(service)
            .planName(normalize(item.planName()))
            .billingCycle(normalize(item.billingCycle()))
            .monthlyPrice(item.monthlyPrice())
            .build()));

    servicePlan.update(
        normalize(item.planName()),
        normalize(item.billingCycle()),
        item.monthlyPrice());
  }

  private void upsertPromotion(PromotionSeedItem item) {
    Promotion promotion = promotionRepository
        .findByPromotionTypeAndTitleAndStartsAtAndEndsAt(
            item.promotionType(),
            normalize(item.title()),
            item.startsAt(),
            item.endsAt())
        .orElseGet(() -> promotionRepository.save(Promotion.builder()
            .promotionType(item.promotionType())
            .title(normalize(item.title()))
            .summary(normalizeNullable(item.summary()))
            .originalPrice(item.originalPrice())
            .discountPrice(item.discountPrice())
            .billingCycle(normalizeNullable(item.billingCycle()))
            .startsAt(item.startsAt())
            .endsAt(item.endsAt())
            .sourceUrl(normalizeNullable(item.sourceUrl()))
            .imageUrl(normalizeNullable(item.imageUrl()))
            .build()));

    promotion.update(
        item.promotionType(),
        normalize(item.title()),
        normalizeNullable(item.summary()),
        item.originalPrice(),
        item.discountPrice(),
        normalizeNullable(item.billingCycle()),
        item.startsAt(),
        item.endsAt(),
        normalizeNullable(item.sourceUrl()),
        normalizeNullable(item.imageUrl()));

    promotionServiceMappingRepository.deleteAllByPromotion_PromotionId(promotion.getPromotionId());
    promotionServiceMappingRepository.flush();

    List<Service> services = resolveServices(item);
    List<PromotionServiceMapping> mappings = services.stream()
        .map(service -> PromotionServiceMapping.builder()
            .promotion(promotion)
            .service(service)
            .build())
        .toList();

    promotionServiceMappingRepository.saveAll(mappings);
  }

  private void upsertBundle(PromotionSeedItem item) {
    List<Service> services = resolveServices(item);
    if (services.size() < 2) {
      return;
    }

    String code = buildBundleCode(item, services);
    Bundle bundle = bundleRepository.findByCode(code)
        .orElseGet(() -> bundleRepository.save(Bundle.builder()
            .code(code)
            .name(normalize(item.title()))
            .planName(normalize(item.title()))
            .billingCycle(normalizeNullable(item.billingCycle()) == null ? "MONTHLY" : normalize(item.billingCycle()))
            .monthlyPrice(item.discountPrice() == null ? item.originalPrice() : item.discountPrice())
            .originalPrice(resolveBundleOriginalPrice(item))
            .logoUrl(normalizeNullable(item.imageUrl()))
            .sourceUrl(normalizeNullable(item.sourceUrl()))
            .build()));

    bundle.update(
        normalize(item.title()),
        normalize(item.title()),
        normalizeNullable(item.billingCycle()) == null ? "MONTHLY" : normalize(item.billingCycle()),
        item.discountPrice() == null ? item.originalPrice() : item.discountPrice(),
        resolveBundleOriginalPrice(item),
        normalizeNullable(item.imageUrl()),
        normalizeNullable(item.sourceUrl()));

    bundleServiceMapRepository.deleteAllByBundle_BundleId(bundle.getBundleId());
    bundleServiceMapRepository.flush();
    bundleServiceMapRepository.saveAll(services.stream()
        .map(service -> BundleServiceMap.builder()
            .bundle(bundle)
            .service(service)
            .build())
        .toList());
  }

  private void upsertBundle(BundleSeedItem item) {
    List<Service> services = resolveBundleServices(item);
    if (services.size() < 2) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT,
          "bundle seed must reference at least two services. code=" + item.code());
    }

    String code = normalize(item.code());
    String billingCycle = normalizeNullable(item.billingCycle()) == null
        ? "MONTHLY"
        : normalize(item.billingCycle());

    Bundle bundle = bundleRepository.findByCode(code)
        .orElseGet(() -> bundleRepository.save(Bundle.builder()
            .code(code)
            .name(normalize(item.name()))
            .planName(normalize(item.planName()))
            .billingCycle(billingCycle)
            .monthlyPrice(item.monthlyPrice())
            .originalPrice(item.originalPrice())
            .logoUrl(normalizeNullable(item.logoUrl()))
            .sourceUrl(normalizeNullable(item.sourceUrl()))
            .build()));

    bundle.update(
        normalize(item.name()),
        normalize(item.planName()),
        billingCycle,
        item.monthlyPrice(),
        item.originalPrice(),
        normalizeNullable(item.logoUrl()),
        normalizeNullable(item.sourceUrl()));

    bundleServiceMapRepository.deleteAllByBundle_BundleId(bundle.getBundleId());
    bundleServiceMapRepository.flush();
    bundleServiceMapRepository.saveAll(services.stream()
        .map(service -> BundleServiceMap.builder()
            .bundle(bundle)
            .service(service)
            .build())
        .toList());
  }

  private void upsertSubscriptionUsage(SubscriptionUsageSeedItem item) {
    Service service = serviceRepository.findByCode(normalize(item.serviceCode()))
        .orElseThrow(() -> new BusinessException(
            ErrorCode.SERVICE_NOT_FOUND,
            "subscription usage seed references unknown service code. code=" + item.serviceCode()));

    SubscriptionUsageDaily usage = subscriptionUsageDailyRepository
        .findByUserIdAndService_ServiceIdAndUsageDate(1L, service.getServiceId(), item.usageDate())
        .orElseGet(() -> SubscriptionUsageDaily.builder()
            .userId(1L)
            .service(service)
            .usageDate(item.usageDate())
            .usedMinutes(item.usedMinutes())
            .build());

    usage.updateUsedMinutes(item.usedMinutes());
    subscriptionUsageDailyRepository.save(usage);
  }

  private List<Service> resolveServices(PromotionSeedItem item) {
    Set<String> distinctServiceCodes = new LinkedHashSet<>();
    for (var serviceItem : nullSafeList(item.services())) {
      distinctServiceCodes.add(normalize(serviceItem.serviceCode()));
    }

    return distinctServiceCodes.stream()
        .map(serviceCode -> serviceRepository.findByCode(serviceCode)
            .orElseThrow(() -> new BusinessException(
                ErrorCode.SERVICE_NOT_FOUND,
                "promotion seed references unknown service code. code=" + serviceCode)))
        .toList();
  }

  private List<Service> resolveBundleServices(BundleSeedItem item) {
    Set<String> distinctServiceCodes = new LinkedHashSet<>();
    for (var serviceItem : nullSafeList(item.services())) {
      distinctServiceCodes.add(normalize(serviceItem.serviceCode()));
    }

    return distinctServiceCodes.stream()
        .map(serviceCode -> serviceRepository.findByCode(serviceCode)
            .orElseThrow(() -> new BusinessException(
                ErrorCode.SERVICE_NOT_FOUND,
                "bundle seed references unknown service code. code=" + serviceCode)))
        .toList();
  }

  private void validateBundleCodes(BundleSeedPayload payload) {
    Set<String> seenCodes = new LinkedHashSet<>();
    Set<String> duplicatedCodes = new LinkedHashSet<>();

    for (BundleSeedItem item : nullSafeList(payload.bundles())) {
      String code = normalize(item.code());
      if (!seenCodes.add(code)) {
        duplicatedCodes.add(code);
      }
    }

    if (!duplicatedCodes.isEmpty()) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT,
          "bundle seed contains duplicated codes. codes=" + duplicatedCodes);
    }
  }

  private String buildBundleCode(PromotionSeedItem item, List<Service> services) {
    String baseCode = services.stream()
        .map(Service::getCode)
        .sorted()
        .reduce((left, right) -> left + "_" + right)
        .orElse("BUNDLE");
    String suffix = normalize(item.title())
        .toUpperCase(Locale.ROOT)
        .replaceAll("[^A-Z0-9]+", "_")
        .replaceAll("_+", "_")
        .replaceAll("^_|_$", "");
    return suffix.isBlank() ? baseCode : baseCode + "_" + suffix;
  }

  private Integer resolveBundleOriginalPrice(PromotionSeedItem item) {
    if (item.originalPrice() != null) {
      return item.originalPrice();
    }
    return item.discountPrice();
  }

  private <T> T readFile(MultipartFile file, Class<T> payloadType) {
    try (var inputStream = file.getInputStream()) {
      return objectMapper.readValue(inputStream, payloadType);
    } catch (IOException e) {
      throw new BusinessException(
          ErrorCode.INTERNAL_ERROR,
          "failed to read import file. fileName=" + file.getOriginalFilename() + ", message=" + e.getMessage());
    }
  }

  private void validateFile(MultipartFile file, String importType) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT,
          importType + " import file must not be empty.");
    }
  }

  private String normalize(String value) {
    if (value == null) {
      throw new BusinessException(ErrorCode.INVALID_INPUT, "seed value must not be null.");
    }
    return value.trim();
  }

  private String normalizeRequired(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must not be blank.");
    }
    return value.trim();
  }

  private String normalizeNullable(String value) {
    if (value == null) {
      return null;
    }

    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private <T> List<T> nullSafeList(List<T> items) {
    return items == null ? Collections.emptyList() : items;
  }
}
