package com.ssafy.e106.domain.subscription.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ssafy.e106.domain.subscription.entity.CheckinRecord;

public interface CheckinRecordRepository extends JpaRepository<CheckinRecord, Long> {

  boolean existsByUser_UserIdAndService_ServiceIdAndCycleYm(
      Long userId,
      Long serviceId,
      String cycleYm);

  @EntityGraph(attributePaths = {"service"})
  @Query("""
      select cr
      from CheckinRecord cr
      where cr.user.userId = :userId
        and (:serviceId is null or cr.service.serviceId = :serviceId)
        and (:fromCycleYm is null or cr.cycleYm >= :fromCycleYm)
        and (:toCycleYm is null or cr.cycleYm <= :toCycleYm)
      order by cr.cycleYm desc, cr.respondedAt desc
      """)
  List<CheckinRecord> findHistory(
      @Param("userId") Long userId,
      @Param("serviceId") Long serviceId,
      @Param("fromCycleYm") String fromCycleYm,
      @Param("toCycleYm") String toCycleYm,
      Pageable pageable);

  void deleteAllByUser_UserId(Long userId);
}
