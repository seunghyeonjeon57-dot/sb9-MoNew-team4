package com.example.monew.domain.article.service;

import com.example.monew.domain.activity.service.ActivityService;
import com.example.monew.domain.article.dto.ArticleViewDto;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.entity.ArticleViewEntity;
import com.example.monew.domain.article.repository.ArticleRepository;
import com.example.monew.domain.article.repository.ArticleViewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ArticleViewServiceTest {

  @Mock
  private ArticleRepository articleRepository;

  @Mock
  private ArticleViewRepository articleViewRepository;

  @Mock
  private ActivityService activityService;

  @InjectMocks
  private ArticleViewService articleViewService;

  @Test
  @DisplayName("로그 뷰 테스트 성공(로그 조회 시 조회수가 증가, 로그 저장 후 DTO를 반환)")
  void logViewTest() {
    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String clientIp = "0.0.0.0";

    ArticleEntity article = ArticleEntity.builder()
        .title("네이버")
        .source("테스트")
        .sourceUrl("https://" + UUID.randomUUID())
        .build();

    long initialViewCount = article.getViewCount();

    ArticleViewEntity savedLog = ArticleViewEntity.builder()
        .article(article)
        .viewedBy(userId)
        .clientIp(clientIp)
        .build();

    given(articleRepository.findById(articleId)).willReturn(Optional.of(article));
    given(articleViewRepository.save(any(ArticleViewEntity.class))).willReturn(savedLog);

    ArticleViewDto result = articleViewService.logView(articleId, userId, clientIp);

    assertThat(result).isNotNull();

    assertThat(article.getViewCount()).isEqualTo(initialViewCount + 1L);

    verify(articleRepository).findById(articleId);
    verify(articleViewRepository).save(any(ArticleViewEntity.class));
  }
}