package com.example.monew.domain.article.controller;

import com.example.monew.domain.article.dto.ArticleDto;
import com.example.monew.domain.article.dto.CursorPageResponseArticleDto;
import com.example.monew.domain.article.service.ArticleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ArticleControllerPagingTest {

  @Mock
  private ArticleService articleService;

  @Test
  void PagingTest() {
    UUID nextId = UUID.randomUUID();
    LocalDateTime nextTime = LocalDateTime.now();
    ArticleDto mockDto = mock(ArticleDto.class);

    CursorPageResponseArticleDto mockResponse = new CursorPageResponseArticleDto(
        List.of(mockDto, mockDto),
        nextId.toString(),
        nextTime,
        2,
        null,
        true
    );

    given(articleService.getArticles(any(), any(), anyInt())).willReturn(mockResponse);

    CursorPageResponseArticleDto result = articleService.getArticles(null, null, 2);

    assertThat(result.content()).hasSize(2);
    assertThat(result.hasNext()).isTrue();
    assertThat(result.nextCursor()).isEqualTo(nextId.toString());
    assertThat(result.nextAfter()).isEqualTo(nextTime);
  }
}