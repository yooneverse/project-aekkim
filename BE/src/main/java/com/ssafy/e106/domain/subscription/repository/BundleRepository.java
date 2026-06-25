package com.ssafy.e106.domain.subscription.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.e106.domain.subscription.entity.Bundle;

public interface BundleRepository extends JpaRepository<Bundle, Long> {

  java.util.List<Bundle> findAllByOrderByBundleIdAsc();

  Optional<Bundle> findByCode(String code);
}
