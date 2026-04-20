package com.example.monew.batch;

import com.example.monew.batch.service.BackupService;
import com.example.monew.batch.service.S3Service;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.repository.ArticleRepository;
import com.example.monew.domain.article.service.ArticleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsBatchSchedulerTest {

  @Mock
  private NewsRss newsRss;

  @Mock
  private NewsCollector newsCollector;

  @Mock
  private BackupService backupService;

  @Mock
  private ArticleService articleService;

  @Mock
  private ArticleRepository articleRepository;

  @Mock
  private S3Service s3Service;

  @Spy
  private ObjectMapper objectMapper = new ObjectMapper();

  @InjectMocks
  private NewsBatchScheduler newsBatchScheduler;

  @Test
  void NewsBatchTest() {

    ArticleEntity mockArticle = ArticleEntity.builder()
        .title("테스트 뉴스")
        .sourceUrl("https://test.com")
        .build();

    when(newsCollector.fetchNaver(anyString()))
        .thenReturn(List.of(mockArticle));

    newsBatchScheduler.runNewsBatch();

    verify(articleService, atLeastOnce())
        .saveInChunks(anyList());
  }
}
