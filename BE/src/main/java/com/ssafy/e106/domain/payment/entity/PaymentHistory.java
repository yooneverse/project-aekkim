package com.ssafy.e106.domain.payment.entity;

import java.time.LocalDate;

import com.ssafy.e106.global.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
    name = "payment_history",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_payment_history_payment_id",
            columnNames = "payment_id")
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentHistory extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long paymentHistoryId;

  @Column(name = "payment_id", nullable = false, length = 100)
  private String paymentId;

  @Column(name = "merchant_raw", nullable = false, length = 255)
  private String merchantRaw;

  @Column(nullable = false)
  private Integer amount;

  @Column(name = "payment_date", nullable = false)
  private LocalDate paymentDate;

  @Column(length = 50)
  private String category;

  @Builder
  public PaymentHistory(
      String paymentId,
      String merchantRaw,
      Integer amount,
      LocalDate paymentDate,
      String category) {
    this.paymentId = paymentId;
    this.merchantRaw = merchantRaw;
    this.amount = amount;
    this.paymentDate = paymentDate;
    this.category = category;
  }
}
