package com.ssafy.e106.domain.subscription.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.e106.domain.subscription.entity.Service;
import com.ssafy.e106.domain.subscription.enums.ServiceCategory;

public interface ServiceRepository extends JpaRepository<Service, Long> {

    List<Service> findAllByOrderByServiceIdAsc();

    List<Service> findAllByCategoryOrderByNameAsc(ServiceCategory category);

    Optional<Service> findByCode(String code);

    boolean existsByCode(String code);
}
