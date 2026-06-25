package com.ssafy.e106.domain.subscriptionusage.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.e106.domain.subscriptionusage.entity.SubscriptionUsageDaily;

public interface SubscriptionUsageDailyRepository extends JpaRepository<SubscriptionUsageDaily, Long> {

  java.util.List<SubscriptionUsageDaily> findAllByOrderByUsageDateDescUsageIdDesc();

  Optional<SubscriptionUsageDaily> findByUserIdAndService_ServiceIdAndUsageDate(
      Long userId,
      Long serviceId,
      LocalDate usageDate);

  List<SubscriptionUsageDaily> findAllByUserIdAndUsageDateBetweenOrderByUsageDateAscService_ServiceIdAsc(
      Long userId,
      LocalDate startDate,
      LocalDate endDate);

  List<SubscriptionUsageDaily> findAllByUserIdOrderByUsageDateAscService_ServiceIdAsc(Long userId);
}
