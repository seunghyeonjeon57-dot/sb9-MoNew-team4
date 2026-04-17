package com.example.monew.domain.article.entity;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ArticleViewEntityUnitTest {

  @Test
  @DisplayName("엔티티 생성 테스트")
  void CreateArticleViewTest() {
    ArticleEntity article = ArticleEntity.builder()
        .title("테스트 기사")
        .build();

    ArticleViewEntity view = ArticleViewEntity.builder()
        .article(article)
        .viewerId("YUK")
        .clientIp("0.0.0.0")
        .build();

    assertThat(view.getViewerId()).isEqualTo("YUK");
    assertThat(view.getClientIp()).isEqualTo("0.0.0.0");
    assertThat(view.getArticleEntity().getTitle()).isEqualTo("테스트 기사");
  }
}