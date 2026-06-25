package com.ssafy.e106.domain.notification.batch.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.ssafy.e106.domain.notification.batch.exception.FcmSendBatchException;
import com.ssafy.e106.domain.notification.batch.model.CheckinSendTarget;
import com.ssafy.e106.domain.notification.entity.Notification;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class CheckinNotificationSendJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;

  @Bean
  public Job checkinNotificationSendJob(Step checkinNotificationSendStep) {
    return new JobBuilder("checkinNotificationSendJob", jobRepository)
        .start(checkinNotificationSendStep)
        .build();
  }

  @Bean
  public Step checkinNotificationSendStep(
      ItemReader<Notification> checkinPendingNotificationReader,
      ItemProcessor<Notification, CheckinSendTarget> checkinNotificationSendProcessor,
      ItemWriter<CheckinSendTarget> checkinNotificationSendWriter) {
    return new StepBuilder("checkinNotificationSendStep", jobRepository)
        .<Notification, CheckinSendTarget>chunk(1, transactionManager)
        .reader(checkinPendingNotificationReader)
        .processor(checkinNotificationSendProcessor)
        .writer(checkinNotificationSendWriter)
        .faultTolerant()
        .retry(FcmSendBatchException.class)
        .retryLimit(2)
        .skip(FcmSendBatchException.class)
        .skipLimit(1000)
        .build();
  }
}
