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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({QueryDslTestConfig.class, JpaAuditConfig.class})
public class CommentRepositoryTest {
  // RED 원인: CommentRepository 인터페이스를 아직 만들지 않아서 컴파일 에러 발생
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

    assertThat(savedComment.getId()).isNotNull();
    assertThat(savedComment.getContent()).isEqualTo("레포지토리 테스트 댓글");
    assertThat(savedComment.getLikeCount()).isEqualTo(0L);
    assertThat(savedComment.getCreatedAt()).isNotNull();
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
    commentRepository.save(c1);

    CommentEntity c2 = CommentEntity.builder()
        .articleId(article.getId())
        .userId(user.getId())
        .content("중간 댓글")
        .likeCount(0L)
        .build();
    commentRepository.save(c2);

    CommentEntity c3 = CommentEntity.builder()
        .articleId(article.getId())
        .userId(user.getId())
        .content("가장 최신 댓글")
        .likeCount(0L)
        .build();
    commentRepository.save(c3);

    em.flush();
    em.clear();

    List<CommentDto> result = commentRepository.findCommentsByArticleWithCursor(
        article.getId(),
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
}
