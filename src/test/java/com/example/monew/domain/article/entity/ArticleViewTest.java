package com.example.monew.domain.article.entity;


import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ArticleViewTest {

  @Test
  @DisplayName("엔티티 생성 테스트")
  void CreateArticleViewTest() {
    UUID testUserid = UUID.randomUUID();
    ArticleEntity article = ArticleEntity.builder()
        .title("테스트 기사")
        .build();

    ArticleViewEntity view = ArticleViewEntity.builder()
        .article(article)
        .viewedBy(testUserid)
        .clientIp("0.0.0.0")
        .build();

    assertThat(view.getViewedBy()).isEqualTo(testUserid);
    assertThat(view.getClientIp()).isEqualTo("0.0.0.0");
    assertThat(view.getArticleEntity().getTitle()).isEqualTo("테스트 기사");
  }
}