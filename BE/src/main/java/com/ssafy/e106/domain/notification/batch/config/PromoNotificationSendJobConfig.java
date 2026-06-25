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
import com.ssafy.e106.domain.notification.batch.model.PromoSendTarget;
import com.ssafy.e106.domain.notification.entity.Notification;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class PromoNotificationSendJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;

  @Bean
  public Job promoNotificationSendJob(Step promoNotificationSendStep) {
    return new JobBuilder("promoNotificationSendJob", jobRepository)
        .start(promoNotificationSendStep)
        .build();
  }

  @Bean
  public Step promoNotificationSendStep(
      ItemReader<Notification> promoPendingNotificationReader,
      ItemProcessor<Notification, PromoSendTarget> promoNotificationSendProcessor,
      ItemWriter<PromoSendTarget> promoNotificationSendWriter) {
    return new StepBuilder("promoNotificationSendStep", jobRepository)
        .<Notification, PromoSendTarget>chunk(1, transactionManager)
        .reader(promoPendingNotificationReader)
        .processor(promoNotificationSendProcessor)
        .writer(promoNotificationSendWriter)
        .faultTolerant()
        .retry(FcmSendBatchException.class)
        .retryLimit(2)
        .skip(FcmSendBatchException.class)
        .skipLimit(1000)
        .build();
  }
}
