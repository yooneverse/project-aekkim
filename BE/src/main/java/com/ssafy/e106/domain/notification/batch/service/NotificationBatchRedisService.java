package com.ssafy.e106.domain.notification.batch.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationBatchRedisService {

  private static final String JOB_LOCK_PREFIX = "batch:lock:";
  private static final String CHECKIN_SENT_PREFIX = "checkin:sent:";
  private static final String CHECKIN_SEND_LOCK_PREFIX = "checkin:send-lock:";
  private static final String PROMO_SENT_PREFIX = "promo:sent:";
  private static final String PROMO_SEND_LOCK_PREFIX = "promo:send-lock:";

  private final StringRedisTemplate stringRedisTemplate;

  public boolean tryAcquireJobLock(String jobName, Duration ttl) {
    Boolean acquired = stringRedisTemplate.opsForValue()
        .setIfAbsent(JOB_LOCK_PREFIX + jobName, "1", ttl);
    return Boolean.TRUE.equals(acquired);
  }

  public void releaseJobLock(String jobName) {
    stringRedisTemplate.delete(JOB_LOCK_PREFIX + jobName);
  }

  public boolean isCheckinAlreadySent(String dedupKey) {
    return Boolean.TRUE.equals(stringRedisTemplate.hasKey(CHECKIN_SENT_PREFIX + dedupKey));
  }

  public boolean tryAcquireCheckinSendLock(String dedupKey, Duration ttl) {
    Boolean acquired = stringRedisTemplate.opsForValue()
        .setIfAbsent(CHECKIN_SEND_LOCK_PREFIX + dedupKey, "1", ttl);
    return Boolean.TRUE.equals(acquired);
  }

  public void releaseCheckinSendLock(String dedupKey) {
    stringRedisTemplate.delete(CHECKIN_SEND_LOCK_PREFIX + dedupKey);
  }

  public void markCheckinSent(String dedupKey, Duration ttl) {
    stringRedisTemplate.opsForValue()
        .set(CHECKIN_SENT_PREFIX + dedupKey, "1", ttl);
  }

  public boolean isPromoAlreadySent(String dedupKey) {
    return Boolean.TRUE.equals(stringRedisTemplate.hasKey(PROMO_SENT_PREFIX + dedupKey));
  }

  public boolean tryAcquirePromoSendLock(String dedupKey, Duration ttl) {
    Boolean acquired = stringRedisTemplate.opsForValue()
        .setIfAbsent(PROMO_SEND_LOCK_PREFIX + dedupKey, "1", ttl);
    return Boolean.TRUE.equals(acquired);
  }

  public void releasePromoSendLock(String dedupKey) {
    stringRedisTemplate.delete(PROMO_SEND_LOCK_PREFIX + dedupKey);
  }

  public void markPromoSent(String dedupKey, Duration ttl) {
    stringRedisTemplate.opsForValue()
        .set(PROMO_SENT_PREFIX + dedupKey, "1", ttl);
  }
}
