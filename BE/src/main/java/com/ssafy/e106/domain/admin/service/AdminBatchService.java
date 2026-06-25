package com.ssafy.e106.domain.admin.service;

import java.time.Duration;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.ssafy.e106.domain.admin.dto.response.AdminBatchExecutionResponse;
import com.ssafy.e106.domain.notification.batch.service.NotificationBatchRedisService;
import com.ssafy.e106.global.exception.BusinessException;
import com.ssafy.e106.global.exception.ErrorCode;

@Service
public class AdminBatchService {

  private static final Duration JOB_LOCK_TTL = Duration.ofMinutes(30);

  private final JobLauncher jobLauncher;
  private final NotificationBatchRedisService notificationBatchRedisService;
  private final Job checkinTargetSelectionJob;
  private final Job checkinNotificationSendJob;
  private final Job promoTargetSelectionJob;
  private final Job promoNotificationSendJob;

  public AdminBatchService(
      JobLauncher jobLauncher,
      NotificationBatchRedisService notificationBatchRedisService,
      @Qualifier("checkinTargetSelectionJob") Job checkinTargetSelectionJob,
      @Qualifier("checkinNotificationSendJob") Job checkinNotificationSendJob,
      @Qualifier("promoTargetSelectionJob") Job promoTargetSelectionJob,
      @Qualifier("promoNotificationSendJob") Job promoNotificationSendJob) {
    this.jobLauncher = jobLauncher;
    this.notificationBatchRedisService = notificationBatchRedisService;
    this.checkinTargetSelectionJob = checkinTargetSelectionJob;
    this.checkinNotificationSendJob = checkinNotificationSendJob;
    this.promoTargetSelectionJob = promoTargetSelectionJob;
    this.promoNotificationSendJob = promoNotificationSendJob;
  }

  public AdminBatchExecutionResponse runJob(String jobName) {
    Job job = resolveJob(jobName);

    if (!notificationBatchRedisService.tryAcquireJobLock(jobName, JOB_LOCK_TTL)) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT,
          "job is already running. jobName=" + jobName);
    }

    try {
      JobParameters jobParameters = new JobParametersBuilder()
          .addLong("runAt", System.currentTimeMillis())
          .addString("triggeredBy", "admin-web")
          .toJobParameters();

      JobExecution jobExecution = jobLauncher.run(job, jobParameters);
      return new AdminBatchExecutionResponse(
          jobName,
          jobExecution.getId(),
          jobExecution.getStatus().name());
    } catch (Exception e) {
      throw new BusinessException(
          ErrorCode.INTERNAL_ERROR,
          "admin batch execution failed. jobName=" + jobName + ", message=" + e.getMessage());
    } finally {
      notificationBatchRedisService.releaseJobLock(jobName);
    }
  }

  private Job resolveJob(String jobName) {
    return switch (jobName) {
      case "checkin-target-selection" -> checkinTargetSelectionJob;
      case "checkin-notification-send" -> checkinNotificationSendJob;
      case "promo-target-selection" -> promoTargetSelectionJob;
      case "promo-notification-send" -> promoNotificationSendJob;
      default -> throw new BusinessException(
          ErrorCode.INVALID_INPUT,
          "unsupported admin batch job. jobName=" + jobName);
    };
  }
}
