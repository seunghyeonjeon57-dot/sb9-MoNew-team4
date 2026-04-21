package com.example.monew.domain.user.batch;

import com.example.monew.domain.user.entity.User;
import com.example.monew.domain.user.entity.type.UserStatus;
import com.example.monew.domain.user.service.UserService;
import jakarta.persistence.EntityManagerFactory;
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
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class UserPurgeBatchConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final EntityManagerFactory entityManagerFactory;
  private final UserService userService;

  @Bean
  public Job userPurgeJob() {
    return new JobBuilder("userPurgeJob", jobRepository)
        .start(userPurgeStep())
        .build();
  }

  @Bean
  public Step userPurgeStep() {
    return new StepBuilder("userPurgeStep", jobRepository)
        .<User, User>chunk(100, transactionManager) 
        .reader(userPurgeReader(null))
        .writer(userPurgeWriter())
        .build();
  }

  @Bean
  @StepScope
  public JpaCursorItemReader<User> userPurgeReader(
      @Value("#{jobParameters['requestDate']}") String requestDate
  ) {
    
    LocalDateTime targetTime = (requestDate != null)
        ? LocalDateTime.parse(requestDate).minusDays(1)
        : LocalDateTime.now().minusDays(1);

    log.info("Batch Reader(Cursor) - 1일 경과 유저 조회 기준 시간: {}", targetTime);

    
    return new JpaCursorItemReaderBuilder<User>()
        .name("userPurgeReader")
        .entityManagerFactory(entityManagerFactory)
        .queryString("SELECT u FROM User u WHERE u.status = :status AND u.deletedAt <= :targetTime")
        .parameterValues(Map.of(
            "status", UserStatus.DELETED,
            "targetTime", targetTime
        ))
        .build(); 
  }

  @Bean
  public ItemWriter<User> userPurgeWriter() {
    return chunk -> {
      for (User user : chunk) {
        
        userService.hardDeleteUser(user.getId());
      }
      log.info("Batch Writer - {}명의 유저 물리 삭제 완료", chunk.size());
    };
  }
}