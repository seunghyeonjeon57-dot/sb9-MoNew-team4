package com.example.monew.batch;

import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.repository.ArticleRepository;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsBatchSchedulerTest {
  @Mock
  private NewsRss newsRss;
  @Mock
  private NewsCollector newsCollector;
  @Spy
  private ObjectMapper objectMapper = new ObjectMapper();
  @Mock
  private ArticleRepository articleRepository;
  @Mock
  private com.example.monew.batch.service.S3Service s3Service;
  @InjectMocks
  private NewsBatchScheduler newsBatchScheduler;

  @Test
  @DisplayName("뉴스 배치 성공")
  void NewsBatchTest() {
    when(newsRss.getUrlList()).thenReturn(List.of("hankyung"));

    when(articleRepository.findByPublishDateAfter(any())).thenReturn(List.of());

    ArticleEntity mockArticle = ArticleEntity.builder()
        .title("테스트 뉴스")
        .sourceUrl("https://test.com")
        .build();

    lenient().when(newsCollector.fetchNaver(anyString())).thenReturn(List.of(mockArticle));
    lenient().when(newsCollector.fetchRss(any(), any(), any())).thenReturn(List.of(mockArticle));

    lenient().when(articleRepository.findAllBySourceUrlIn(anyList())).thenReturn(List.of());
    lenient().when(articleRepository.existsBySourceUrl(anyString())).thenReturn(false);

    newsBatchScheduler.runNewsBatch();

    verify(articleRepository, atLeastOnce()).saveAll(anyList());
  }
}