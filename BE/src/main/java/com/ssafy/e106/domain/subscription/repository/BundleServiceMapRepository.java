package com.ssafy.e106.domain.subscription.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.e106.domain.subscription.entity.BundleServiceMap;

public interface BundleServiceMapRepository extends JpaRepository<BundleServiceMap, Long> {

  List<BundleServiceMap> findByBundle_BundleIdIn(List<Long> bundleIds);

  List<BundleServiceMap> findByBundle_BundleIdOrderByService_ServiceIdAsc(Long bundleId);

  void deleteAllByBundle_BundleId(Long bundleId);
}
