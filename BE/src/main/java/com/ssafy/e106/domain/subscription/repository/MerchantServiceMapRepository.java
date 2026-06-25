package com.ssafy.e106.domain.subscription.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.e106.domain.subscription.entity.MerchantServiceMap;

public interface MerchantServiceMapRepository extends JpaRepository<MerchantServiceMap, Long> {

    Optional<MerchantServiceMap> findByMerchantRaw(String merchantRaw);

    @EntityGraph(attributePaths = {"service"})
    List<MerchantServiceMap> findAllByMerchantRawIn(Collection<String> merchantRaws);
}
