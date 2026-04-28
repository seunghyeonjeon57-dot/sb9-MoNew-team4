package com.example.monew.domain.article.entity;


import com.example.monew.domain.user.entity.User;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ArticleViewTest {

  @Test
  @DisplayName("엔티티 생성 테스트")
  void CreateArticleViewTest() {
    User user = User.builder().build();
    ArticleEntity article = ArticleEntity.builder()
        .title("테스트 기사")
        .build();

    ArticleViewEntity view = ArticleViewEntity.builder()
        .article(article)
        .viewedBy(user)
        .clientIp("0.0.0.0")
        .build();

    assertThat(view.getViewedBy()).isEqualTo(user);
    assertThat(view.getClientIp()).isEqualTo("0.0.0.0");
    assertThat(view.getArticleEntity().getTitle()).isEqualTo("테스트 기사");
  }
}