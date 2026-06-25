package com.ssafy.e106.domain.subscription.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.e106.domain.subscription.entity.ServicePlan;

public interface ServicePlanRepository extends JpaRepository<ServicePlan, Long> {

  List<ServicePlan> findAllByOrderByServicePlanIdAsc();

  List<ServicePlan> findAllByService_ServiceIdOrderByMonthlyPriceAscServicePlanIdAsc(Long serviceId);

  Optional<ServicePlan> findByService_ServiceIdAndPlanNameAndBillingCycle(
      Long serviceId,
      String planName,
      String billingCycle);

  Optional<ServicePlan> findByServicePlanIdAndService_ServiceId(Long servicePlanId, Long serviceId);
}
