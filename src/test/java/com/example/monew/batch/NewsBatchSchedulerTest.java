package com.example.monew.batch;

import com.example.monew.domain.article.batch.NewsBatchScheduler;
import com.example.monew.domain.article.batch.NewsCollector;
import com.example.monew.domain.article.batch.NewsRss;
import com.example.monew.domain.article.batch.exception.S3FileNotFoundException;
import com.example.monew.domain.article.batch.service.S3Service;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.repository.ArticleRepository;
import com.example.monew.domain.article.service.ArticleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

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
  private ArticleService articleService;

  @Mock
  private ArticleRepository articleRepository;

  @Mock
  private S3Service s3Service;

  @Spy
  private ObjectMapper objectMapper = new ObjectMapper()
      .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
      .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  @InjectMocks
  private NewsBatchScheduler newsBatchScheduler;

  @Test
  @DisplayName("뉴스 수집 배치 실행 테스트 - RSS 수집 및 저장 검증")
  void NewsBatchTest() {
    ArticleEntity mockArticle = ArticleEntity.builder()
        .title("테스트 뉴스")
        .sourceUrl("https://test.com")
        .publishDate(LocalDateTime.now()) // 오늘 날짜 설정
        .build();

    when(newsRss.getUrlList()).thenReturn(List.of("https://test-rss.com"));

    when(newsCollector.fetchRss(anyString(), anyString(), anyString()))
        .thenReturn(List.of(mockArticle));

    when(articleRepository.findByPublishDateAfter(any(LocalDateTime.class)))
        .thenReturn(List.of(mockArticle));

    when(s3Service.download(anyString())).thenThrow(new S3FileNotFoundException("파일 없음"));

    newsBatchScheduler.runNewsBatch();

    verify(articleService, atLeastOnce()).saveInChunks(anyList());

    verify(s3Service, times(1)).upload(anyString(), anyString());
  }
}