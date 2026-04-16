package com.example.monew.domain.article.service;

import com.example.monew.domain.article.dto.ArticleDto;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.mapper.ArticleMapper;
import com.example.monew.domain.article.repository.ArticleRepository;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServicePagingTest {

  @Mock
  ArticleRepository articleRepository;

  @Mock
  ArticleMapper articleMapper;

  @InjectMocks
  ArticleService articleService;

  @Test
  void PagingTest() {

    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    UUID id3 = UUID.randomUUID();

    ArticleEntity a1 = mock(ArticleEntity.class);
    ArticleEntity a2 = mock(ArticleEntity.class);
    ArticleEntity a3 = mock(ArticleEntity.class);

    when(a2.getId()).thenReturn(id2);

    when(articleRepository.findByCursor(null, null, 2))
        .thenReturn(new ArrayList<>(List.of(a1, a2, a3)));

    when(articleMapper.toDto(any(), eq(false)))
        .thenReturn(mock(ArticleDto.class));

    var result = articleService.getArticles(null, null, 2);

    assertThat(result.content().size()).isEqualTo(2);
    assertThat(result.hasNext()).isTrue();
    assertThat(result.nextCursor()).isEqualTo(id2.toString());
  }
}