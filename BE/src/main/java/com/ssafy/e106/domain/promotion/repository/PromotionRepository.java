package com.ssafy.e106.domain.promotion.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ssafy.e106.domain.promotion.entity.Promotion;
import com.ssafy.e106.domain.promotion.enums.PromotionType;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

  List<Promotion> findAllByOrderByPromotionIdDesc();

  Optional<Promotion> findByPromotionTypeAndTitleAndStartsAtAndEndsAt(
      PromotionType promotionType,
      String title,
      LocalDateTime startsAt,
      LocalDateTime endsAt);

  @Query(
      value = """
      select p
      from Promotion p
      where exists (
        select 1
        from PromotionServiceMapping psm
        where psm.promotion = p
          and psm.service.serviceId in :serviceIds
      )
        and (:promotionType is null or p.promotionType = :promotionType)
        and p.startsAt <= :now
        and p.endsAt >= :now
      order by p.startsAt desc, p.promotionId desc
      """,
      countQuery = """
      select count(p)
      from Promotion p
      where exists (
        select 1
        from PromotionServiceMapping psm
        where psm.promotion = p
          and psm.service.serviceId in :serviceIds
      )
        and (:promotionType is null or p.promotionType = :promotionType)
        and p.startsAt <= :now
        and p.endsAt >= :now
      """)
  Page<Promotion> findRecommendedPromotions(
      @Param("serviceIds") Collection<Long> serviceIds,
      @Param("promotionType") PromotionType promotionType,
      @Param("now") LocalDateTime now,
      Pageable pageable);

  @Query("""
      select p
      from Promotion p
      where exists (
        select 1
        from PromotionServiceMapping psm
        where psm.promotion = p
          and psm.service.serviceId in :serviceIds
      )
        and (:promotionType is null or p.promotionType = :promotionType)
        and p.startsAt <= :now
        and p.endsAt >= :now
      """)
  List<Promotion> findRecommendedPromotions(
      @Param("serviceIds") Collection<Long> serviceIds,
      @Param("promotionType") PromotionType promotionType,
      @Param("now") LocalDateTime now);
}
