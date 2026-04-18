package com.example.monew.batch;

import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.repository.ArticleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsBatchSchedulerTest {

  @Mock
  private NewsCollector newsCollector;

  @Mock
  private ArticleRepository articleRepository;

  @InjectMocks
  private NewsBatchScheduler newsBatchScheduler;

  @Test
  @DisplayName("뉴스 배치 성공")
  void NewsBatchTest() {
    ArticleEntity mockArticle = ArticleEntity.builder()
        .title("테스트 뉴스")
        .sourceUrl("https://test.com")
        .build();

    when(newsCollector.fetchNaver(anyString())).thenReturn(List.of(mockArticle));
    when(newsCollector.fetchRss(any(), any(), any())).thenReturn(List.of());

    when(articleRepository.existsBySourceUrl(anyString())).thenReturn(false);

    newsBatchScheduler.runNewsBatch();

    verify(articleRepository, atLeastOnce()).save(any(ArticleEntity.class));
  }
}