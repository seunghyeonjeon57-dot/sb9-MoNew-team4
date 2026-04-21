package com.example.monew.domain.article.repository;

import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.entity.ArticleViewEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ArticleViewRepositoryTest {

  @Autowired
  private ArticleRepository articleRepository;

  @Autowired
  private ArticleViewRepository articleViewRepository;

  @Test
  @DisplayName("기사 뷰 레포 테스트 - 생성 후 삭제")
  void createAndDeleteViewTest() {
    // Given
    ArticleEntity article = articleRepository.save(ArticleEntity.builder()
        .title("테스트 뉴스 제목")
        .source("테스트 사이트")
        .sourceUrl("https://" + UUID.randomUUID())
        .build());

    ArticleViewEntity viewLog = ArticleViewEntity.builder()
        .article(article)
        .viewedBy(UUID.randomUUID())
        .clientIp("0.0.0.0")
        .build();

    ArticleViewEntity saved = articleViewRepository.save(viewLog);

    assertThat(saved.getId()).isNotNull();
    assertThat(articleViewRepository.existsById(saved.getId())).isTrue();

    articleViewRepository.deleteById(saved.getId());
    assertThat(articleViewRepository.existsById(saved.getId())).isFalse();
  }
}