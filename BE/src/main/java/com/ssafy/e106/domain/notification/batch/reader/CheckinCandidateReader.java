package com.ssafy.e106.domain.notification.batch.reader;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

import com.ssafy.e106.domain.notification.batch.model.CheckinCandidate;
import com.ssafy.e106.domain.subscription.entity.Subscription;
import com.ssafy.e106.domain.subscription.repository.SubscriptionRepository;

import lombok.RequiredArgsConstructor;

@Component
@StepScope
@RequiredArgsConstructor
public class CheckinCandidateReader implements ItemReader<CheckinCandidate> {

  private static final DateTimeFormatter CYCLE_YM_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

  private final SubscriptionRepository subscriptionRepository;

  private Iterator<CheckinCandidate> iterator;

  @Override
  public CheckinCandidate read() {
    if (iterator == null) {
      iterator = loadCandidates().iterator();
    }

    if (!iterator.hasNext()) {
      return null;
    }
    return iterator.next();
  }

  private List<CheckinCandidate> loadCandidates() {
    LocalDate today = LocalDate.now();
    LocalDate billingDate = today.plusDays(3);
    String cycleYm = today.format(CYCLE_YM_FORMATTER);

    return subscriptionRepository.findCheckinSelectionCandidates(billingDate, cycleYm)
        .stream()
        .map(this::toCandidate)
        .toList();
  }

  private CheckinCandidate toCandidate(Subscription subscription) {
    return new CheckinCandidate(
        subscription.getUserId(),
        subscription.getSubscriptionId(),
        subscription.getService().getServiceId(),
        subscription.getService().getName(),
        subscription.getLowUsageCycleYm());
  }
}
