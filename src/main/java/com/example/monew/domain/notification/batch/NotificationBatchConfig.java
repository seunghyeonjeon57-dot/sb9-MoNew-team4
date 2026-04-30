package com.example.monew.domain.notification.batch;

import com.example.monew.domain.notification.entity.Notification;
import com.example.monew.domain.notification.repository.NotificationRepository;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class NotificationBatchConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final EntityManagerFactory entityManagerFactory;
  private final NotificationRepository notificationRepository;

  @Bean
  public Job cleanupNotificationsJob() {
    return new JobBuilder("cleanupNotificationsJob", jobRepository)
        .start(cleanupStep())
        .build();
  }

  @Bean
  public Step cleanupStep() {
    return new StepBuilder("cleanupStep", jobRepository)
        .<Notification, Notification>chunk(100, transactionManager)
        .reader(oldNotificationReader(null))
        .writer(oldNotificationWriter())
        .build();
  }

  @Bean
  @StepScope
  public JpaCursorItemReader<Notification> oldNotificationReader(
      @Value("#{jobParameters['requestTime']}") String requestTime
  ) {
    // 스케줄러가 넘겨준 시간이 있으면 쓰고, 없으면 현재 시간 기준 1주일 전
    LocalDateTime threshold = (requestTime != null)
        ? LocalDateTime.parse(requestTime).minusWeeks(1)
        : LocalDateTime.now().minusWeeks(1);

    log.info(">>>> [Batch Reader] 기준 시간({}) 이전의 알림을 Cursor로 조회합니다.", threshold);

    return new JpaCursorItemReaderBuilder<Notification>()
        .name("oldNotificationReader")
        .entityManagerFactory(entityManagerFactory)
        .queryString("SELECT n FROM Notification n WHERE n.confirmed = true AND n.updatedAt < :threshold")
        .parameterValues(Map.of("threshold", threshold))
        .build();
  }

  @Bean
  public ItemWriter<Notification> oldNotificationWriter() {
    return chunk -> {
      List<? extends Notification> items = chunk.getItems();
      if (!items.isEmpty()) {
        log.info(">>>> [Batch Writer] {}건의 오래된 알림을 벌크 삭제합니다.", items.size());
        notificationRepository.deleteAllInBatch((Iterable<Notification>) items);
      }
    };
  }
}
