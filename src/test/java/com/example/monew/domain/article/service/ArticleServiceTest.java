package com.example.monew.domain.article.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.example.monew.domain.article.dto.ArticleDto;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.exception.ArticleNotFoundException;
import com.example.monew.domain.article.mapper.ArticleMapper;
import com.example.monew.domain.article.repository.ArticleRepository;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.example.monew.domain.notification.event.ArticleRegisteredEvent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

  @Mock
  private ArticleRepository articleRepository;

  @Mock
  private ArticleMapper articleMapper;

  @Mock
  private EntityManager entityManager;

  @Mock
  private ArticleViewService articleViewService;

  @InjectMocks
  private ArticleService articleService;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Nested
  @DisplayName("뉴스 상세 조회")
  class GetArticleDetail {
    @Test
    @DisplayName("상세 조회 성공")
    void success() {
      UUID id = UUID.randomUUID();
      ArticleEntity article = ArticleEntity.builder().id(id).build();
      given(articleRepository.findById(id)).willReturn(Optional.of(article));
      given(articleMapper.toDto(any(), anyBoolean())).willReturn(mock(ArticleDto.class));

      articleService.getArticleDetail(id);

      verify(articleRepository).findById(id);
    }

    @Test
    @DisplayName("실패: 존재하지 않거나 삭제된 기사 조회 시 예외 발생")
    void fail_NotFound() {
      UUID id = UUID.randomUUID();
      given(articleRepository.findById(id)).willReturn(Optional.empty());

      assertThatThrownBy(() -> articleService.getArticleDetail(id))
          .isInstanceOf(ArticleNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("뉴스 저장")
  class SaveArticle {
    @Test
    @DisplayName("성공: 신규 URL인 경우 저장")
    void success() {
      ArticleEntity article = ArticleEntity.builder().sourceUrl("new-url").build();
      given(articleRepository.existsBySourceUrl("new-url")).willReturn(false);

      articleService.saveArticle(article);

      verify(articleRepository).save(article);
    }

    @Test
    @DisplayName("중복 URL이면 저장하지 않음")
    void skipDuplicate() {
      ArticleEntity article = ArticleEntity.builder().sourceUrl("old-url").build();
      given(articleRepository.existsBySourceUrl("old-url")).willReturn(true);

      articleService.saveArticle(article);

      verify(articleRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("뉴스 논리 삭제")
  class DeleteArticle {
    @Test
    @DisplayName("논리 삭제 성공")
    void success() {
      UUID id = UUID.randomUUID();
      ArticleEntity article = ArticleEntity.builder().id(id).build();
      given(articleRepository.findById(id)).willReturn(Optional.of(article));

      articleService.isDeleted(id);

      verify(articleRepository).softDelete(id);
      verify(entityManager).flush();
    }

    @Test
    @DisplayName("성공: 관심사(interest)가 있으면 저장 후 알림 이벤트를 발행한다.")
    void success_WithInterest_PublishesEvent() {
      // given
      ArticleEntity article = ArticleEntity.builder()
          .sourceUrl("http://interest.com")
          .interest("IT") // 관심사 있음
          .build();
      given(articleRepository.existsBySourceUrl(article.getSourceUrl())).willReturn(false);

      // when
      articleService.saveArticle(article);

      // then
      verify(articleRepository).save(article);
      // 이벤트가 발행되었는지 검증
      verify(eventPublisher, times(1)).publishEvent(any(ArticleRegisteredEvent.class));
    }

    @Test
    @DisplayName("성공: 관심사(interest)가 없으면 저장만 하고 이벤트를 발행하지 않는다.")
    void success_WithoutInterest_NoEvent() {
      // given
      ArticleEntity article = ArticleEntity.builder()
          .sourceUrl("http://no-interest.com")
          .interest(null) // 관심사 없음
          .build();
      given(articleRepository.existsBySourceUrl(article.getSourceUrl())).willReturn(false);

      // when
      articleService.saveArticle(article);

      // then
      verify(articleRepository).save(article);
      // 이벤트가 발행되지 않았는지 검증
      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("실패: 중복된 URL이면 저장 로직을 호출하지 않음")
    void fail_Duplicate() {
      ArticleEntity article = ArticleEntity.builder().sourceUrl("http://old-url.com").build();
      given(articleRepository.existsBySourceUrl(article.getSourceUrl())).willReturn(true);

      articleService.saveArticle(article);

      verify(articleRepository, never()).save(any());
    }

    @Test
    @DisplayName("없는 ID 삭제 시도 시 예외 발생")
    void failNotFound() {
      UUID id = UUID.randomUUID();
      given(articleRepository.findById(id)).willReturn(Optional.empty());

      assertThatThrownBy(() -> articleService.isDeleted(id))
          .isInstanceOf(ArticleNotFoundException.class);
    }
  }

  @Test
  @DisplayName("뉴스 복구 성공")
  void restoreSuccess() {
    UUID id = UUID.randomUUID();

    articleService.restore(id);

    verify(articleRepository).restoreById(id);
    verify(entityManager).flush();
    verify(entityManager).clear();
  }

  @Test
  @DisplayName("페이지네이션 목록 조회 성공")
  void getArticles_Pagination_Success() {
    int size = 5;
    List<ArticleEntity> list = new ArrayList<>();
    for(int i=0; i <= size; i++) list.add(ArticleEntity.builder().id(UUID.randomUUID()).build());

    given(articleRepository.findByCursor(any(), any(), eq(size))).willReturn(list);

    var result = articleService.getArticles(null, null, size);

    assertThat(result.hasNext()).isTrue();
    assertThat(result.content()).hasSize(5);
  }
  @Nested
  @DisplayName("뉴스 물리 삭제")
  class HardDelete {
    @Test
    @DisplayName("물리삭제 호출 성공")
    void success() {
      UUID id = UUID.randomUUID();

      articleService.hardDelete(id);

      verify(articleRepository, times(1)).hardDeleteById(id);
    }
  }

  @Nested
  @DisplayName("청크 저장")
  class SaveInChunks {
    @Test
    @DisplayName("중복된 URL은 제외, 일괄 저장")
    void success() {
      ArticleEntity newArticle = ArticleEntity.builder().sourceUrl("new-url").build();
      ArticleEntity existingArticle = ArticleEntity.builder().sourceUrl("old-url").build();
      List<ArticleEntity> articles = List.of(newArticle, existingArticle);

      given(articleRepository.findAllBySourceUrlIn(any())).willReturn(List.of(existingArticle));

      articleService.saveInChunks(articles);

      verify(articleRepository, times(1)).saveAll(argThat((List<ArticleEntity> list) -> list.size() == 1));    }

    @Test
    @DisplayName("실패: 빈 리스트나 null이 들어오면 아무것도 수행하지 않음")
    void skip_EmptyOrNull() {
      articleService.saveInChunks(new ArrayList<>());
      articleService.saveInChunks(null);

      verify(articleRepository, never()).saveAll(any());
    }
  }

  @Nested
  @DisplayName("조회수 증가 로직")
  class IncrementViewCount {

    @Test
    @DisplayName("조회 수 증가 로직 호출")
    void success() {
      UUID articleId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      String clientIp = "127.0.0.1";

      articleService.incrementViewCount(articleId, userId, clientIp);

      verify(articleViewService, times(1)).logView(articleId, userId, clientIp);
    }
  }
}
