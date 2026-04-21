package com.example.monew.domain.user.batch;

import com.example.monew.domain.comment.repository.CommentRepository;
import com.example.monew.domain.user.entity.User;
import com.example.monew.domain.user.entity.type.UserStatus;
import com.example.monew.domain.user.repository.UserRepository;
import com.example.monew.domain.user.service.UserService;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

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
        .reader(userPurgeReader())
        .writer(userPurgeWriter())
        .build();
  }

  @Bean
  public JpaPagingItemReader<User> userPurgeReader() {
    return new JpaPagingItemReaderBuilder<User>()
        .name("userPurgeReader")
        .entityManagerFactory(entityManagerFactory)
        // 요구사항: 상태가 DELETED이고 삭제된 지 1일이 지난 유저 조회
        .queryString("SELECT u FROM User u WHERE u.status = :status AND u.deletedAt <= :targetTime")
        .parameterValues(Map.of(
            "status", UserStatus.DELETED,
            "targetTime", LocalDateTime.now().minusDays(1)
        ))
        .pageSize(100)
        .build();
  }

  @Bean
  public ItemWriter<User> userPurgeWriter() {
    return users -> {
      for (User user : users) {
       userService.hardDeleteUser(user.getId());
      }
    };
  }
}