package com.ssafy.e106.domain.notification.batch.config;

import java.time.Duration;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ssafy.e106.domain.notification.batch.service.NotificationBatchRedisService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CheckinBatchScheduler {

  private static final String JOB_NAME = "checkin-target-selection";

  private final JobLauncher jobLauncher;
  private final Job checkinTargetSelectionJob;
  private final NotificationBatchRedisService notificationBatchRedisService;

  @Scheduled(cron = "0 0 4 * * *")
  public void runCheckinTargetSelectionJob() throws Exception {
    if (!notificationBatchRedisService.tryAcquireJobLock(JOB_NAME, Duration.ofMinutes(30))) {
      return;
    }

    try {
      JobParameters jobParameters = new JobParametersBuilder()
          .addLong("runAt", System.currentTimeMillis())
          .toJobParameters();

      jobLauncher.run(checkinTargetSelectionJob, jobParameters);
    } finally {
      notificationBatchRedisService.releaseJobLock(JOB_NAME);
    }
  }
}
