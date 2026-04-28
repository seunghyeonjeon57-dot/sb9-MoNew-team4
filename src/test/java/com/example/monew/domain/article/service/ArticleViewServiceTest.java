package com.example.monew.domain.article.service;

import com.example.monew.domain.activity.service.ActivityService;
import com.example.monew.domain.article.dto.ArticleViewDto;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.entity.ArticleViewEntity;
import com.example.monew.domain.article.repository.ArticleRepository;
import com.example.monew.domain.article.repository.ArticleViewRepository;
import com.example.monew.domain.user.entity.User;
import com.example.monew.domain.user.repository.UserRepository;
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
  private UserRepository userRepository;

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

    User user = User.builder().build();

    ArticleEntity article = ArticleEntity.builder()
        .title("네이버")
        .source("테스트")
        .sourceUrl("https://" + UUID.randomUUID())
        .build();

    ArticleViewEntity savedLog = ArticleViewEntity.builder()
        .article(article)
        .viewedBy(user)
        .clientIp(clientIp)
        .build();

    given(articleRepository.findById(articleId)).willReturn(Optional.of(article));
    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(articleViewRepository.save(any(ArticleViewEntity.class))).willReturn(savedLog);

    ArticleViewDto result = articleViewService.logView(articleId, userId, clientIp);

    assertThat(result).isNotNull();

    verify(articleRepository).findById(articleId);
    verify(userRepository).findById(userId);
    verify(articleViewRepository).save(any(ArticleViewEntity.class));
  }

  @Test
  @DisplayName("중복 조회 방지 테스트: 이미 조회한 유저인 경우 조회수가 증가하지 않음")
  void logView_Duplicate_Test() {
    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String clientIp = "127.0.0.1";

    User user = User.builder().build();

    ArticleEntity article = ArticleEntity.builder()
        .title("중복 테스트 기사")
        .source("테스트")
        .sourceUrl("https://test.com")
        .build();

    long initialViewCount = article.getViewCount();

    given(articleRepository.findById(articleId)).willReturn(Optional.of(article));
    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(articleViewRepository.existsByArticleEntityIdAndViewedBy(articleId, user)).willReturn(true);

    ArticleViewDto result = articleViewService.logView(articleId, userId, clientIp);

    assertThat(article.getViewCount()).isEqualTo(initialViewCount);
    assertThat(result.getId()).isNull();

    verify(articleViewRepository).existsByArticleEntityIdAndViewedBy(articleId, user);
    verify(articleViewRepository, org.mockito.Mockito.never()).save(any(ArticleViewEntity.class));
  }
}