package com.example.monew.domain.article.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.monew.domain.article.batch.dto.ArticleBackupDto;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.interest.entity.Interest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ArticleBackupDtoTest {

  @Test
  @DisplayName("ArticleEntity에서 DTO로 변환 성공")
  void from_WithInterest_Success() {
    UUID articleId = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();

    Interest interest = Interest.builder()
        .name("테스트분야")
        .keywords(List.of("키워드1", "키워드2"))
        .build();

    ArticleEntity entity = ArticleEntity.builder()
        .id(articleId)
        .source("모뉴")
        .sourceUrl("https://monew.com")
        .title("테스트 뉴스")
        .publishDate(now)
        .summary("뉴스 요약")
        .interest(interest)
        .build();

    ReflectionTestUtils.setField(entity, "commentCount", 10L);
    ReflectionTestUtils.setField(entity, "viewCount", 100L);

    ArticleBackupDto dto = ArticleBackupDto.from(entity);

    assertThat(dto.getId()).isEqualTo(articleId);
    assertThat(dto.getTitle()).isEqualTo("테스트 뉴스");

    assertThat(dto.getCommentCount()).isEqualTo(10L);
    assertThat(dto.getViewCount()).isEqualTo(100L);

    assertThat(dto.getInterestName()).isEqualTo("테스트분야");
    assertThat(dto.getInterestKeywords()).containsExactly("키워드1", "키워드2");
  }

  @Test
  @DisplayName("Interest가 null인 경우에도 안전하게 변환된다")
  void from_WithoutInterest_Success() {
    ArticleEntity entity = ArticleEntity.builder()
        .id(UUID.randomUUID())
        .title("관심사 없는 뉴스")
        .interest(null)
        .build();

    ReflectionTestUtils.setField(entity, "commentCount", 0L);
    ReflectionTestUtils.setField(entity, "viewCount", 0L);

    ArticleBackupDto dto = ArticleBackupDto.from(entity);

    assertThat(dto.getInterestId()).isNull();
    assertThat(dto.getInterestKeywords()).isEmpty();
  }
}