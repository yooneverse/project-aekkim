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

import com.ssafy.e106.domain.notification.batch.model.PromoCandidate;
import com.ssafy.e106.domain.notification.batch.model.PromoTarget;
import com.ssafy.e106.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class PromoTargetSelectionJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;

  @Bean
  public Job promoTargetSelectionJob(Step promoTargetSelectionStep) {
    return new JobBuilder("promoTargetSelectionJob", jobRepository)
        .start(promoTargetSelectionStep)
        .build();
  }

  @Bean
  public Step promoTargetSelectionStep(
      ItemReader<PromoCandidate> promoCandidateReader,
      ItemProcessor<PromoCandidate, PromoTarget> promoTargetProcessor,
      ItemWriter<PromoTarget> promoTargetWriter) {
    return new StepBuilder("promoTargetSelectionStep", jobRepository)
        .<PromoCandidate, PromoTarget>chunk(100, transactionManager)
        .reader(promoCandidateReader)
        .processor(promoTargetProcessor)
        .writer(promoTargetWriter)
        .faultTolerant()
        .retry(BusinessException.class)
        .retryLimit(2)
        .skip(BusinessException.class)
        .skipLimit(1000)
        .build();
  }
}
