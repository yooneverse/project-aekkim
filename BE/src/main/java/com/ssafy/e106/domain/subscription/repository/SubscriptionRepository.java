package com.ssafy.e106.domain.subscription.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ssafy.e106.domain.subscription.entity.Subscription;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

  @EntityGraph(attributePaths = {"service", "servicePlan", "bundle"})
  List<Subscription> findAllByOrderBySubscriptionIdDesc();

  @EntityGraph(attributePaths = {"service", "servicePlan", "bundle"})
  List<Subscription> findAllByUserIdOrderBySubscriptionIdDesc(Long userId);

  @EntityGraph(attributePaths = {"service", "servicePlan", "bundle"})
  Optional<Subscription> findBySubscriptionId(Long subscriptionId);

  boolean existsByUserIdAndService_ServiceId(Long userId, Long serviceId);

  boolean existsByUserIdAndBundle_BundleId(Long userId, Long bundleId);

  boolean existsByService_ServiceId(Long serviceId);

  boolean existsByServicePlan_ServicePlanId(Long servicePlanId);

  @EntityGraph(attributePaths = {"service", "servicePlan", "bundle"})
  Optional<Subscription> findByUserIdAndService_ServiceId(Long userId, Long serviceId);

  @EntityGraph(attributePaths = {"service", "servicePlan", "bundle"})
  Optional<Subscription> findByUserIdAndBundle_BundleId(Long userId, Long bundleId);

  @EntityGraph(attributePaths = {"service", "servicePlan", "bundle"})
  @Query("""
      select s
      from Subscription s
      where s.nextBillingDate = :billingDate
        and s.lowUsageDetected = true
        and s.lowUsageCycleYm = :cycleYm
      order by s.subscriptionId asc
      """)
  List<Subscription> findCheckinSelectionCandidates(
      @Param("billingDate") LocalDate billingDate,
      @Param("cycleYm") String cycleYm);

  @EntityGraph(attributePaths = {"service", "servicePlan", "bundle"})
  @Query("""
      select s
      from Subscription s
      where s.lowUsageDetected = true
        and s.lowUsageCycleYm = :cycleYm
      order by s.userId asc, s.subscriptionId asc
      """)
  List<Subscription> findPromoSelectionCandidates(@Param("cycleYm") String cycleYm);

  void deleteAllByUserId(Long userId);
}
