package com.example.monew.domain.article.repository;

import com.example.monew.config.JpaAuditConfig;
import com.example.monew.config.QueryDslTestConfig;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.entity.ArticleViewEntity;
import com.example.monew.domain.user.entity.User;
import com.example.monew.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import com.example.monew.domain.user.entity.type.UserStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QueryDslTestConfig.class, JpaAuditConfig.class})
class ArticleViewRepositoryTest {

  @Autowired
  private ArticleRepository articleRepository;

  @Autowired
  private ArticleViewRepository articleViewRepository;

  @Autowired
  private UserRepository userRepository;

  @Test
  @Disabled
  @DisplayName("기사 뷰 레포 테스트 - 생성 후 삭제")
  void createAndDeleteViewTest() {

    User user = userRepository.save(User.builder()
        .email("test@test.com")
        .nickname("테스트유저") // <-- 이 부분을 추가하세요!
        .password("password123") // 만약 password도 NOT NULL이라면 추가
        .status(UserStatus.ACTIVE)
        .build());

    // Given
    ArticleEntity article = articleRepository.save(ArticleEntity.builder()
        .title("테스트 뉴스 제목")
        .source("테스트 사이트")
        .sourceUrl("https://" + UUID.randomUUID())
        .build());

    ArticleViewEntity viewLog = ArticleViewEntity.builder()
        .article(article)
        .viewedBy(user)
        .clientIp("0.0.0.0")
        .build();

    ArticleViewEntity saved = articleViewRepository.save(viewLog);

    assertThat(saved.getId()).isNotNull();
    assertThat(articleViewRepository.existsById(saved.getId())).isTrue();
    assertThat(articleViewRepository.existsByArticleEntityIdAndViewedBy(article.getId(), user)).isTrue();

    articleViewRepository.deleteById(saved.getId());
    assertThat(articleViewRepository.existsById(saved.getId())).isFalse();
  }
}