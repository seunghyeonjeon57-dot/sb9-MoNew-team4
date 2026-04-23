package com.example.monew.domain.article.articleconfig;
import com.example.monew.domain.article.batch.service.S3Service;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.repository.ArticleRepository;
import com.example.monew.domain.article.repository.ArticleViewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;
@Slf4j
@Configuration
@RequiredArgsConstructor
public class NewsBackupBatchConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final EntityManagerFactory entityManagerFactory;
  private final S3Service s3Service;
  private final ObjectMapper objectMapper;
  private final ArticleRepository articleRepository;
  private final ArticleViewRepository articleViewRepository;


  @Bean
  public Job backupJob() {
    return new JobBuilder("backupJob", jobRepository)
        .start(backupStep())
        .build();
  }

  @Bean
  public Step backupStep() {
    return new StepBuilder("backupStep", jobRepository)
        .<ArticleEntity, ArticleEntity>chunk(100, transactionManager)
        .reader(articleReader())
        .writer(articleS3Writer())
        .build();
  }

  @Bean
  public Job restoreJob() {
    return new JobBuilder("restoreJob", jobRepository)
        .start(restoreStep())
        .build();
  }

  @Bean
  public Step restoreStep() {
    return new StepBuilder("restoreStep", jobRepository)
        .<ArticleEntity, ArticleEntity>chunk(100, transactionManager)
        .reader(jsonFileItemReader(null))
        .processor(duplicateCheckProcessor())
        .writer(articleRestoreWriter())
        .build();
  }
  @Bean
  @StepScope
  public FlatFileItemReader<ArticleEntity> jsonFileItemReader(
      @Value("#{jobParameters['filePath']}") String filePath) {
    return new FlatFileItemReaderBuilder<ArticleEntity>()
        .name("jsonFileItemReader")
        .resource(new FileSystemResource(filePath))
        .lineMapper((line, lineNumber) -> objectMapper.readValue(line, ArticleEntity.class))
        .build();
  }

  @Bean
  public ItemProcessor<ArticleEntity, ArticleEntity> duplicateCheckProcessor() {
    return article -> {
      Optional<ArticleEntity> existingOpt = articleRepository.findBySourceUrl(article.getSourceUrl());
      if (existingOpt.isPresent() && !existingOpt.get().isDeleted()) {
        return null;
      }
      return article;
    };
  }

  @Bean
  public JpaItemWriter<ArticleEntity> articleJpaWriter() {
    return new JpaItemWriterBuilder<ArticleEntity>()
        .entityManagerFactory(entityManagerFactory)
        .build();
  }
  @Bean
  public JpaPagingItemReader<ArticleEntity> articleReader() {
    return new JpaPagingItemReaderBuilder<ArticleEntity>()
        .name("articleReader")
        .entityManagerFactory(entityManagerFactory)
        .queryString("SELECT a FROM ArticleEntity a WHERE a.publishDate >= :yesterday")
        .parameterValues(Map.of("yesterday", LocalDate.now().minusDays(1).atStartOfDay()))
        .pageSize(100)
        .build();
  }

  @Bean
  public ItemWriter<ArticleEntity> articleS3Writer() {
    return chunk -> {
      String jsonChunk = objectMapper.writeValueAsString(chunk.getItems());

      String fileName = "backups/" + LocalDate.now() + ".json";

      s3Service.upload(fileName, jsonChunk);
    };
  }
  @Bean
  public ItemWriter<ArticleEntity> articleRestoreWriter() {
    return chunk -> {
      for (ArticleEntity article : chunk) {
        Optional<ArticleEntity> existingOpt = articleRepository.findBySourceUrl(article.getSourceUrl());

        if (existingOpt.isPresent()) {
          ArticleEntity existing = existingOpt.get();
          if (existing.isDeleted()) {
            log.info("유실된 데이터 복구 실행: {}", existing.getSourceUrl());
            articleRepository.restoreById(existing.getId());
          }
        } else {
          log.info("신규 데이터 등록: {}", article.getSourceUrl());
          articleRepository.save(article);
        }
      }
    };
  }

  @Bean
  public Job hardDeleteJob() {
    return new JobBuilder("hardDeleteJob", jobRepository)
        .start(hardDeleteStep())
        .build();
  }

  @Bean
  public Step hardDeleteStep() {
    return new StepBuilder("hardDeleteStep", jobRepository)
        .<ArticleEntity, ArticleEntity>chunk(100, transactionManager)
        .reader(oldDeletedArticleReader())
        .writer(articleHardDeleteWriter())
        .build();
  }

  @Bean
  public JpaPagingItemReader<ArticleEntity> oldDeletedArticleReader() {
    return new JpaPagingItemReaderBuilder<ArticleEntity>()
        .name("oldDeletedArticleReader")
        .entityManagerFactory(entityManagerFactory)
        .queryString("SELECT a FROM ArticleEntity a WHERE a.deletedAt <= :threshold")
        .parameterValues(Map.of("threshold", LocalDateTime.now().minusDays(30)))
        .pageSize(100)
        .build();
  }

  @Bean
  public ItemWriter<ArticleEntity> articleHardDeleteWriter() {
    return chunk -> {
      for (ArticleEntity article : chunk) {
        articleViewRepository.deleteByArticleEntity(article);

        articleRepository.delete(article);

        log.info("물리 삭제 완료 (기사 ID: {}, 관련 로그 포함)", article.getId());
      }
    };
  }
}