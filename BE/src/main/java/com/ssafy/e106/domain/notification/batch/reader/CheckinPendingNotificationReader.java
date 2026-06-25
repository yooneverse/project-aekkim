package com.ssafy.e106.domain.notification.batch.reader;

import java.util.Iterator;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.ssafy.e106.domain.notification.entity.Notification;
import com.ssafy.e106.domain.notification.enums.NotificationType;
import com.ssafy.e106.domain.notification.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

@Component
@StepScope
@RequiredArgsConstructor
public class CheckinPendingNotificationReader implements ItemReader<Notification>, ItemStream {

  private static final int CHUNK_SIZE = 100;
  private static final String LAST_READ_NOTIFICATION_ID_KEY =
      "checkinPendingNotificationReader.lastReadNotificationId";

  private final NotificationRepository notificationRepository;

  private Iterator<Notification> iterator;
  private long lastReadNotificationId;

  @Override
  public Notification read() {
    while (iterator == null || !iterator.hasNext()) {
      iterator = notificationRepository
          .findAllByTypeAndSentAtIsNullAndNotificationIdGreaterThanOrderByNotificationIdAsc(
              NotificationType.CHECKIN,
              lastReadNotificationId,
              PageRequest.of(0, CHUNK_SIZE))
          .iterator();

      if (!iterator.hasNext()) {
        return null;
      }
    }

    final Notification notification = iterator.next();
    lastReadNotificationId = notification.getNotificationId();
    return notification;
  }

  @Override
  public void open(final ExecutionContext executionContext) throws ItemStreamException {
    if (executionContext.containsKey(LAST_READ_NOTIFICATION_ID_KEY)) {
      lastReadNotificationId = executionContext.getLong(LAST_READ_NOTIFICATION_ID_KEY);
    } else {
      lastReadNotificationId = 0L;
    }
    iterator = null;
  }

  @Override
  public void update(final ExecutionContext executionContext) throws ItemStreamException {
    executionContext.putLong(LAST_READ_NOTIFICATION_ID_KEY, lastReadNotificationId);
  }

  @Override
  public void close() throws ItemStreamException {
    iterator = null;
  }
}
