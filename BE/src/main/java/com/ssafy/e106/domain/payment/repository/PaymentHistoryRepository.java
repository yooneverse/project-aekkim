package com.ssafy.e106.domain.payment.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.e106.domain.payment.entity.PaymentHistory;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {

  boolean existsByPaymentId(String paymentId);

  Optional<PaymentHistory> findByPaymentId(String paymentId);

  List<PaymentHistory> findAllByPaymentIdIn(Collection<String> paymentIds);

  List<PaymentHistory> findAllByOrderByPaymentDateDescPaymentHistoryIdDesc();

  List<PaymentHistory> findAllByPaymentDateBetweenOrderByPaymentDateDescPaymentHistoryIdDesc(
      LocalDate startDate,
      LocalDate endDate);

  List<PaymentHistory> findAllByCategoryOrderByPaymentDateDescPaymentHistoryIdDesc(String category);
}
