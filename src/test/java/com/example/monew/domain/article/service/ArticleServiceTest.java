package com.example.monew.domain.article.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.example.monew.domain.article.dto.ArticleDto;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.mapper.ArticleMapper;
import com.example.monew.domain.article.repository.ArticleRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleServiceSuccessTest {

  @Mock private ArticleRepository articleRepository;
  @Mock private ArticleMapper articleMapper;
  @Mock private EntityManager entityManager;
  @Mock private ArticleViewService articleViewService;

  @InjectMocks
  private ArticleService articleService;

  @Test
  @DisplayName("상세 조회 성공")
  void getArticleDetail_Success() {
    UUID id = UUID.randomUUID();
    ArticleEntity article = ArticleEntity.builder().id(id).build();
    given(articleRepository.findById(id)).willReturn(Optional.of(article));
    given(articleMapper.toDto(any(), anyBoolean())).willReturn(mock(ArticleDto.class));

    articleService.getArticleDetail(id);

    verify(articleRepository).findById(id);
  }

  @Test
  @DisplayName("페이지네이션 목록 조회 성공")
  void getArticles_Success() {
    UUID cursor = UUID.randomUUID();
    LocalDateTime after = LocalDateTime.now();
    int size = 10;

    // size보다 하나 더 많게 반환하여 hasNext 로직 타게 함
    List<ArticleEntity> list = new ArrayList<>();
    for(int i=0; i <= size; i++) list.add(ArticleEntity.builder().id(UUID.randomUUID()).build());

    given(articleRepository.findByCursor(cursor, after, size)).willReturn(list);

    var result = articleService.getArticles(cursor, after, size);

    assertThat(result.hasNext()).isTrue();
    verify(articleRepository).findByCursor(cursor, after, size);
  }

  @Test
  @DisplayName("신규 뉴스 일괄 저장 성공")
  void saveInChunks_Success() {
    ArticleEntity news1 = ArticleEntity.builder().sourceUrl("url1").build();
    ArticleEntity news2 = ArticleEntity.builder().sourceUrl("url2").build();
    List<ArticleEntity> list = List.of(news1, news2);

    // DB에 아무것도 없다고 가정
    given(articleRepository.findAllBySourceUrlIn(any())).willReturn(List.of());

    articleService.saveInChunks(list);

    verify(articleRepository).saveAll(any());
  }

  @Test
  @DisplayName("논리 삭제 성공")
  void isDeleted_Success() {
    UUID id = UUID.randomUUID();
    ArticleEntity article = ArticleEntity.builder().id(id).build();
    given(articleRepository.findById(id)).willReturn(Optional.of(article));

    articleService.isDeleted(id);

    verify(articleRepository).softDelete(id);
    verify(entityManager).flush();
  }

  @Test
  @DisplayName("조회수 증가 호출 성공")
  void incrementViewCount_Success() {
    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    articleService.incrementViewCount(articleId, userId, "127.0.0.1");

    verify(articleViewService).logView(articleId, userId, "127.0.0.1");
  }

  @Test
  @DisplayName("물리 삭제 성공")
  void hardDelete_Success() {
    UUID id = UUID.randomUUID();
    articleService.hardDelete(id);
    verify(articleRepository).hardDeleteById(id);
  }
}