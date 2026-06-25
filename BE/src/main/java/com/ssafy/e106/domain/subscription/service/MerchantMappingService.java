package com.ssafy.e106.domain.subscription.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import com.ssafy.e106.domain.subscription.dto.request.MerchantMappingBatchConfirmItemRequest;
import com.ssafy.e106.domain.subscription.dto.request.MerchantMappingBatchLookupItemRequest;
import com.ssafy.e106.domain.subscription.dto.request.SubscriptionCreateRequest;
import com.ssafy.e106.domain.subscription.dto.response.MerchantMappingBatchConfirmItemResponse;
import com.ssafy.e106.domain.subscription.dto.response.MerchantMappingBatchConfirmResponse;
import com.ssafy.e106.domain.subscription.dto.response.MerchantMappingBatchLookupItemResponse;
import com.ssafy.e106.domain.subscription.dto.response.MerchantMappingBatchLookupResponse;
import com.ssafy.e106.domain.subscription.entity.MerchantServiceMap;
import com.ssafy.e106.domain.subscription.entity.Service;
import com.ssafy.e106.domain.subscription.entity.Subscription;
import com.ssafy.e106.domain.subscription.repository.MerchantServiceMapRepository;
import com.ssafy.e106.domain.subscription.repository.ServiceRepository;
import com.ssafy.e106.domain.subscription.repository.SubscriptionRepository;
import com.ssafy.e106.global.exception.BusinessException;
import com.ssafy.e106.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class MerchantMappingService {

  private static final int AUTO_MATCH_HIT_COUNT = 3;

  private final MerchantServiceMapRepository merchantServiceMapRepository;
  private final ServiceRepository serviceRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final SubscriptionService subscriptionService;

  @Transactional(readOnly = true)
  public MerchantMappingBatchLookupResponse batchLookup(
      List<MerchantMappingBatchLookupItemRequest> items) {
    Set<String> merchantRaws = items.stream()
        .map(MerchantMappingBatchLookupItemRequest::merchantRaw)
        .collect(Collectors.toCollection(LinkedHashSet::new));

    Map<String, MerchantServiceMap> mappingsByMerchantRaw = merchantServiceMapRepository
        .findAllByMerchantRawIn(merchantRaws)
        .stream()
        .collect(Collectors.toMap(
            MerchantServiceMap::getMerchantRaw,
            Function.identity()));

    List<MerchantMappingBatchLookupItemResponse> results = items.stream()
        .map(item -> toLookupResponse(item, mappingsByMerchantRaw.get(item.merchantRaw())))
        .toList();

    return new MerchantMappingBatchLookupResponse(results);
  }

  @Transactional
  public MerchantMappingBatchConfirmResponse batchConfirm(
      Long userId,
      List<MerchantMappingBatchConfirmItemRequest> items) {
    validateDuplicateMerchantRaw(items);

    List<MerchantMappingBatchConfirmItemResponse> results = items.stream()
        .map(item -> confirmOne(userId, item))
        .toList();

    return new MerchantMappingBatchConfirmResponse(results);
  }

  private void validateDuplicateMerchantRaw(List<MerchantMappingBatchConfirmItemRequest> items) {
    Set<String> uniqueMerchantRaws = items.stream()
        .map(MerchantMappingBatchConfirmItemRequest::merchantRaw)
        .collect(Collectors.toSet());

    if (uniqueMerchantRaws.size() != items.size()) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT,
          "동일한 merchantRaw는 한 번만 확정할 수 있습니다.");
    }
  }

  private MerchantMappingBatchConfirmItemResponse confirmOne(
      Long userId,
      MerchantMappingBatchConfirmItemRequest request) {
    Service service = serviceRepository.findById(request.serviceId())
        .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_NOT_FOUND));

    subscriptionService.createSubscription(
        userId,
        new SubscriptionCreateRequest(
            "SINGLE",
            request.serviceId(),
            request.servicePlanId(),
            null,
            request.nextBillingDate()));

    MerchantServiceMap merchantServiceMap = merchantServiceMapRepository
        .findByMerchantRaw(request.merchantRaw())
        .map(existing -> {
          existing.confirm(service);
          return existing;
        })
        .orElseGet(() -> MerchantServiceMap.builder()
            .merchantRaw(request.merchantRaw())
            .service(service)
            .hitCount(1)
            .build());

    merchantServiceMapRepository.save(merchantServiceMap);

    Subscription subscription = subscriptionRepository
        .findByUserIdAndService_ServiceId(userId, request.serviceId())
        .orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

    return MerchantMappingBatchConfirmItemResponse.from(subscription);
  }

  private MerchantMappingBatchLookupItemResponse toLookupResponse(
      MerchantMappingBatchLookupItemRequest request,
      MerchantServiceMap mapping) {
    if (mapping == null) {
      return new MerchantMappingBatchLookupItemResponse(
          request.merchantRaw(),
          false,
          request.predictedServiceId(),
          request.predictedServicePlanId(),
          null);
    }

    return new MerchantMappingBatchLookupItemResponse(
        request.merchantRaw(),
        mapping.getHitCount() >= AUTO_MATCH_HIT_COUNT,
        mapping.getService().getServiceId(),
        request.predictedServicePlanId(),
        mapping.getHitCount());
  }
}
