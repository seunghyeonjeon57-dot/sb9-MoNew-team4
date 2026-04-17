package com.example.monew.domain.article.service;

import com.example.monew.domain.article.dto.ArticleDto;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.exception.ArticleNotFoundException;
import com.example.monew.domain.article.mapper.ArticleMapper;
import com.example.monew.domain.article.service.ArticleService;
import com.example.monew.domain.article.repository.ArticleRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

  @Mock
  private ArticleRepository articleRepository;

  @InjectMocks
  private ArticleService articleService;

  @Mock
  private ArticleMapper articleMapper;

  @Nested
  @DisplayName("기사 상세 조회")
  class GetArticleDetail {

    @Test
    @DisplayName("성공: 기사가 존재하면 조회수가 증가된 기사를 반환")
    void success() {
      UUID id = UUID.randomUUID();
      ArticleEntity article = ArticleEntity.builder()
          .id(id)
          .title("테스트")

          .build();

      given(articleRepository.findById(id)).willReturn(Optional.of(article));

      ArticleDto expectedDto = new ArticleDto(id, null, null, "테스트", null, null, null, 1L, false);
      given(articleMapper.toDto(any(ArticleEntity.class), anyBoolean())).willReturn(expectedDto);

      ArticleDto result = articleService.getArticleDetail(id);

      assertThat(result.viewCount()).isEqualTo(1);
      verify(articleRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("실패: 기사가 없으면 예외처리")
    void fail() {
      UUID id = UUID.randomUUID();
      given(articleRepository.findById(id)).willReturn(Optional.empty());

      assertThatThrownBy(() -> articleService.getArticleDetail(id))
          .isInstanceOf(ArticleNotFoundException.class)
          .hasMessageContaining("해당 기사를 찾을 수 없습니다.");
    }
  }

  @Nested
  @DisplayName("기사 저장")
  class SaveArticle {

    @Test
    @DisplayName("성공: 중복되지 않은 URL이면 기사를 저장")
    void success() {
      ArticleEntity article = ArticleEntity.builder().sourceUrl("http://new-url.com").build();
      given(articleRepository.existsBySourceUrl(article.getSourceUrl())).willReturn(false);

      articleService.saveArticle(article);

      verify(articleRepository, times(1)).save(article);
    }

    @Test
    @DisplayName("실패: 중복된 URL이면 저장 로직을 호출하지 않음")
    void fail_Duplicate() {
      ArticleEntity article = ArticleEntity.builder().sourceUrl("http://old-url.com").build();
      given(articleRepository.existsBySourceUrl(article.getSourceUrl())).willReturn(true);

      articleService.saveArticle(article);

      verify(articleRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("기사 논리 삭제")
  class isDeleted {

    @Test
    @DisplayName("성공: 존재하는 기사면 repository.delete()를 호출한다.")
    void success() {
      UUID id = UUID.randomUUID();
      ArticleEntity article = ArticleEntity.builder().build();
      given(articleRepository.findById(id)).willReturn(Optional.of(article));

      articleService.isDeleted(id);

      verify(articleRepository).delete(article);
    }

    @Test
    @DisplayName("실패: 없는 기사 아이디면 예외를 발생시킨다.")
    void fail() {
      UUID id = UUID.randomUUID();
      given(articleRepository.findById(id)).willReturn(Optional.empty());

      assertThatThrownBy(() -> articleService.isDeleted(id))
          .isInstanceOf(ArticleNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("기사 복구 테스트 (restore)")
  class RestoreArticle {

    @Test
    @DisplayName("성공: 복구 메서드 호출 시 레포지토리의 restoreById가 실행된다")
    void success() {
      UUID id = UUID.randomUUID();

      articleService.restore(id);

      verify(articleRepository, times(1)).restoreById(id);
    }
  }

  @Nested
  @DisplayName("출처 목록 조회 테스트 (getAllSources)")
  class GetAllSources {

    @Test
    @DisplayName("성공: 등록된 모든 언론사 리스트를 반환한다")
    void success() {

      List<String> sources = List.of("네이버", "0000");
      given(articleRepository.findAllSources()).willReturn(sources);

      List<String> result = articleService.getAllSources();

      assertThat(result).hasSize(2);
      assertThat(result).contains("네이버", "0000");
    }
  }

}