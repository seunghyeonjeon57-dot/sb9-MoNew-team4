package com.example.monew.domain.comment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.monew.config.JpaAuditConfig;
import com.example.monew.config.QueryDslTestConfig;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.repository.ArticleRepository;
import com.example.monew.domain.comment.dto.CommentDto;
import com.example.monew.domain.comment.entity.CommentEntity;
import com.example.monew.domain.user.entity.User;
import com.example.monew.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import({QueryDslTestConfig.class, JpaAuditConfig.class})
public class CommentRepositoryTest {
  @Autowired
  private CommentRepository commentRepository;
  @Autowired
  private TestEntityManager entityManager;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private ArticleRepository articleRepository;
  @Autowired
  private EntityManager em;

  @Test
  @DisplayName("댓글 엔티티를 DB에 저장하고 조회할 수 있다.")
  void saveAndFindComment() {
    CommentEntity comment = CommentEntity.builder()
        .articleId(UUID.randomUUID())
        .userId(UUID.randomUUID())
        .content("레포지토리 테스트 댓글")
        .likeCount(0L)
        .build();

    CommentEntity savedComment = commentRepository.save(comment);

    em.flush();
    em.clear();

    CommentEntity foundComment = commentRepository.findById(savedComment.getId()).get();

    assertThat(foundComment.getId()).isNotNull();
    assertThat(foundComment.getContent()).isEqualTo("레포지토리 테스트 댓글");
    assertThat(foundComment.getLikeCount()).isEqualTo(0L);
    assertThat(foundComment.getCreatedAt()).isNotNull();
  }

  @Test
  @DisplayName("엔티티 내용을 수정하고 플러시하면 DB에 반영된다.")
  void updateComment_Persistence() {
    CommentEntity comment = CommentEntity.builder()
        .articleId(UUID.randomUUID())
        .userId(UUID.randomUUID())
        .content("원본")
        .likeCount(0L)
        .build();
    CommentEntity saved = commentRepository.save(comment);

    saved.updateContent("수정됨");
    commentRepository.flush(); // 강제 반영

    CommentEntity found = commentRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getContent()).isEqualTo("수정됨");
  }

  @Test
  @DisplayName("사용자 ID로 해당 사용자가 작성한 모든 댓글을 물리 삭제한다.")
  void deleteAllByUserId_Test() {

    UUID targetUserId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    UUID articleId = UUID.randomUUID();

    commentRepository.save(CommentEntity.builder()
        .articleId(articleId).userId(targetUserId).content("타겟 유저 댓글 1").likeCount(0L).build());
    commentRepository.save(CommentEntity.builder()
        .articleId(articleId).userId(targetUserId).content("타겟 유저 댓글 2").likeCount(0L).build());
    commentRepository.save(CommentEntity.builder()
        .articleId(articleId).userId(otherUserId).content("다른 유저 댓글").likeCount(0L).build());

    entityManager.flush();
    entityManager.clear();


    commentRepository.deleteAllByUserId(targetUserId);

    entityManager.flush();
    entityManager.clear();


    long targetCount = commentRepository.findAll().stream()
        .filter(c -> c.getUserId().equals(targetUserId))
        .count();
    long otherCount = commentRepository.findAll().stream()
        .filter(c -> c.getUserId().equals(otherUserId))
        .count();

    assertThat(targetCount).isEqualTo(0);
    assertThat(otherCount).isEqualTo(1);
  }

  @Test
  @DisplayName("사용자 ID로 해당 사용자의 모든 댓글을 논리 삭제(Soft Delete) 한다.")
  void softDeleteAllByUserId_Test() {

    UUID targetUserId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    UUID articleId = UUID.randomUUID();

    commentRepository.save(CommentEntity.builder()
        .articleId(articleId).userId(targetUserId).content("타겟 유저 댓글 1").likeCount(0L).build());
    commentRepository.save(CommentEntity.builder()
        .articleId(articleId).userId(targetUserId).content("타겟 유저 댓글 2").likeCount(0L).build());
    commentRepository.save(CommentEntity.builder()
        .articleId(articleId).userId(otherUserId).content("다른 유저 댓글").likeCount(0L).build());

    entityManager.flush();
    entityManager.clear();


    commentRepository.softDeleteAllByUserId(targetUserId);

    List<CommentEntity> allComments = commentRepository.findAll();

    boolean targetDeleted = allComments.stream()
        .filter(c -> c.getUserId().equals(targetUserId))
        .allMatch(c -> c.getDeletedAt() != null);

    boolean otherNotDeleted = allComments.stream()
        .filter(c -> c.getUserId().equals(otherUserId))
        .allMatch(c -> c.getDeletedAt() == null);

    assertThat(targetDeleted).isTrue();
    assertThat(otherNotDeleted).isTrue();
  }

  @Test
  @DisplayName("특정 기사의 댓글 목록을 좋아요 순으로 커서 페이징 조회한다")
  void findCommentsByArticleId_OrderByLikes() {
    User user = User.builder()
        .nickname("테스트닉네임")
        .email("test2@test.com")
        .password("password")
        .build();
    userRepository.save(user);

    ArticleEntity article = ArticleEntity.builder()
        .source("출처")
        .sourceUrl("url" + UUID.randomUUID())
        .title("기사 제목")
        .publishDate(LocalDateTime.now())
        .summary("요약")
        .interest("IT")
        .build();
    articleRepository.save(article);

    CommentEntity c1 = CommentEntity.builder()
        .articleId(article.getId())
        .userId(user.getId())
        .content("댓글 1")
        .likeCount(10L)
        .build();
    CommentEntity c2 = CommentEntity.builder()
        .articleId(article.getId())
        .userId(user.getId())
        .content("댓글 2")
        .likeCount(5L)
        .build();
    CommentEntity c3 = CommentEntity.builder()
        .articleId(article.getId())
        .userId(user.getId())
        .content("댓글 3")
        .likeCount(0L)
        .build();
    commentRepository.saveAll(List.of(c1, c2, c3));

    em.flush();
    em.clear();

    List<CommentDto> result = commentRepository.findCommentsByArticleWithCursor(
        article.getId(),
        null,
        null,
        null,
        "likeCount",
        "DESC",
        10
    );

    assertThat(result).hasSize(3);
    assertThat(result.get(0).likeCount()).isEqualTo(10L);
    assertThat(result.get(1).likeCount()).isEqualTo(5L);
    assertThat(result.get(2).likeCount()).isZero();
  }

  @Test
  @DisplayName("특정 기사의 댓글 목록을 최신순으로 커서 페이징 조회한다")
  void findCommentsByArticleId_OrderByDate() {
    User user = User.builder()
        .nickname("테스트닉네임2")
        .email("test2@test.com")
        .password("password")
        .build();
    userRepository.save(user);

    ArticleEntity article = ArticleEntity.builder()
        .source("출처")
        .sourceUrl("url2" + UUID.randomUUID())
        .title("기사 제목2")
        .publishDate(LocalDateTime.now())
        .summary("요약")
        .interest("IT")
        .build();
    articleRepository.save(article);

    CommentEntity c1 = CommentEntity.builder()
        .articleId(article.getId())
        .userId(user.getId())
        .content("가장 오래된 댓글")
        .likeCount(0L)
        .build();
    ReflectionTestUtils.setField(c1, "createdAt", LocalDateTime.now().minusDays(2));
    commentRepository.save(c1);

    CommentEntity c2 = CommentEntity.builder()
        .articleId(article.getId())
        .userId(user.getId())
        .content("중간 댓글")
        .likeCount(0L)
        .build();
    ReflectionTestUtils.setField(c2, "createdAt", LocalDateTime.now().minusDays(1));
    commentRepository.save(c2);

    CommentEntity c3 = CommentEntity.builder()
        .articleId(article.getId())
        .userId(user.getId())
        .content("가장 최신 댓글")
        .likeCount(0L)
        .build();
    ReflectionTestUtils.setField(c3, "createdAt", LocalDateTime.now());
    commentRepository.save(c3);

    em.flush();
    em.clear();

    List<CommentDto> result = commentRepository.findCommentsByArticleWithCursor(
        article.getId(),
        null,
        null,
        null,
        "createdAt",
        "DESC",
        10
    );

    assertThat(result).hasSize(3);

    assertThat(result.get(0).content()).isEqualTo("가장 최신 댓글");
    assertThat(result.get(2).content()).isEqualTo("가장 오래된 댓글");

    assertThat(result).isSortedAccordingTo((a, b) -> b.createdAt().compareTo(a.createdAt()));
  }

  @Test
  @DisplayName("특정 기사의 댓글 목록을 최신순으로 커서 페이징 조회한다 (다음 페이지)")
  void findCommentsByArticleId_OrderByDate_WithCursor() {
    User user = User.builder().nickname("유저").email("u@test.com").password("p").build();
    userRepository.save(user);

    ArticleEntity article = ArticleEntity.builder()
        .source("출").sourceUrl("u" + UUID.randomUUID()).title("기").publishDate(LocalDateTime.now())
        .summary("요").interest("IT").build();
    articleRepository.save(article);

    CommentEntity c1 = CommentEntity.builder().articleId(article.getId()).userId(user.getId()).content("오래된").likeCount(0L).build();
    ReflectionTestUtils.setField(c1, "createdAt", LocalDateTime.now().minusDays(2));
    commentRepository.save(c1);

    CommentEntity c2 = CommentEntity.builder().articleId(article.getId()).userId(user.getId()).content("중간").likeCount(0L).build();
    ReflectionTestUtils.setField(c2, "createdAt", LocalDateTime.now().minusDays(1));
    commentRepository.save(c2);

    em.flush();
    em.clear();

    List<CommentDto> result = commentRepository.findCommentsByArticleWithCursor(
        article.getId(),
        null,
        c2.getId().toString(),
        c2.getCreatedAt(),
        "createdAt",
        "DESC",
        10
    );

    assertThat(result).hasSize(1);
    assertThat(result.get(0).content()).isEqualTo("오래된");
  }

  @Test
  @DisplayName("특정 기사의 댓글 목록을 좋아요 순으로 커서 페이징 조회한다 (다음 페이지)")
  void findCommentsByArticleId_OrderByLikes_WithCursor_Coverage() {
    User user = userRepository.save(User.builder().nickname("유저").email("like@test.com").password("p").build());
    ArticleEntity article = articleRepository.save(ArticleEntity.builder().source("출").sourceUrl("l" + UUID.randomUUID()).title("기").publishDate(LocalDateTime.now()).interest("IT").build());

    CommentEntity c1 = commentRepository.save(CommentEntity.builder().articleId(article.getId()).userId(user.getId()).content("좋아요 많음").likeCount(10L).build());
    CommentEntity c2 = commentRepository.save(CommentEntity.builder().articleId(article.getId()).userId(user.getId()).content("좋아요 적음").likeCount(5L).build());

    em.flush();
    em.clear();

    String cursor = c1.getLikeCount() + "_" + c1.getId();

    List<CommentDto> result = commentRepository.findCommentsByArticleWithCursor(
        article.getId(),
        null,
        cursor,
        null,
        "likeCount",
        "DESC",
        10
    );

    assertThat(result).hasSize(1);
    assertThat(result.get(0).content()).isEqualTo("좋아요 적음");
  }

  @Test
  @DisplayName("로그인한 유저(currentUserId)가 있을 때 좋아요 여부 서브쿼리를 실행한다")
  void findCommentsByArticleId_WithCurrentUserId_Coverage() {
    User user = userRepository.save(User.builder().nickname("유저").email("me@test.com").password("p").build());
    ArticleEntity article = articleRepository.save(ArticleEntity.builder().source("출").sourceUrl("m" + UUID.randomUUID()).title("기").publishDate(LocalDateTime.now()).interest("IT").build());

    commentRepository.save(CommentEntity.builder().articleId(article.getId()).userId(user.getId()).content("내 댓글").likeCount(0L).build());

    em.flush();
    em.clear();

    List<CommentDto> result = commentRepository.findCommentsByArticleWithCursor(
        article.getId(),
        user.getId(),
        null,
        null,
        "createdAt",
        "DESC",
        10
    );

    assertThat(result).isNotEmpty();
    assertThat(result.get(0).likedByMe()).isFalse();
  }
}
