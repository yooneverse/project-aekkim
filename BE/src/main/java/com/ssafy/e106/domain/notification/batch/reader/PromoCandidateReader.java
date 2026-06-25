package com.ssafy.e106.domain.notification.batch.reader;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import com.ssafy.e106.domain.notification.batch.model.PromoCandidate;
import com.ssafy.e106.domain.subscription.entity.Subscription;
import com.ssafy.e106.domain.subscription.repository.SubscriptionRepository;

import lombok.RequiredArgsConstructor;

@Component
@StepScope
@RequiredArgsConstructor
public class PromoCandidateReader implements ItemReader<PromoCandidate> {

  private static final DateTimeFormatter CYCLE_YM_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

  private final SubscriptionRepository subscriptionRepository;

  private Iterator<PromoCandidate> iterator;

  @Override
  public PromoCandidate read() {
    if (iterator == null) {
      iterator = loadCandidates().iterator();
    }

    if (!iterator.hasNext()) {
      return null;
    }
    return iterator.next();
  }

  private List<PromoCandidate> loadCandidates() {
    String cycleYm = LocalDate.now().format(CYCLE_YM_FORMATTER);

    Map<Long, LinkedHashSet<Long>> serviceIdsByUserId = subscriptionRepository.findPromoSelectionCandidates(cycleYm)
        .stream()
        .collect(Collectors.groupingBy(
            Subscription::getUserId,
            Collectors.mapping(
                subscription -> subscription.getService().getServiceId(),
                Collectors.toCollection(LinkedHashSet::new))));

    return serviceIdsByUserId.entrySet().stream()
        .map(entry -> new PromoCandidate(entry.getKey(), entry.getValue()))
        .toList();
  }
}
