package com.example.monew.config;
import com.example.monew.domain.article.batch.service.S3Service;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.repository.ArticleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
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

@Configuration
@RequiredArgsConstructor
public class NewsBackupBatchConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final EntityManagerFactory entityManagerFactory;
  private final S3Service s3Service;
  private final ObjectMapper objectMapper;
  private final ArticleRepository articleRepository;
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
        .writer(articleJpaWriter())
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
      boolean exists = articleRepository.existsBySourceUrl(article.getSourceUrl());
      return exists ? null : article;
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
}