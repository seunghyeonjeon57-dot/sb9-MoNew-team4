package com.example.monew.domain.comment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.monew.domain.comment.entity.CommentEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
public class CommentRepositoryTest {
  // RED 원인: CommentRepository 인터페이스를 아직 만들지 않아서 컴파일 에러 발생
  @Autowired
  private CommentRepository commentRepository;
  @Autowired
  private TestEntityManager entityManager;

  @Test
  @DisplayName("댓글 엔티티를 DB에 저장하고 조회할 수 있다.")
  void saveAndFindComment() {
    CommentEntity comment = new CommentEntity(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "레포지토리 테스트 댓글"
    );

    CommentEntity savedComment = commentRepository.save(comment);

    assertThat(savedComment.getId()).isNotNull();
    assertThat(savedComment.getContent()).isEqualTo("레포지토리 테스트 댓글");
    assertThat(savedComment.getLikeCount()).isEqualTo(0L);
    assertThat(savedComment.getCreatedAt()).isNotNull();
  }

  @Test
  @DisplayName("엔티티 내용을 수정하고 플러시하면 DB에 반영된다.")
  void updateComment_Persistence() {
    CommentEntity comment = new CommentEntity(UUID.randomUUID(), UUID.randomUUID(), "원본");
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

    commentRepository.save(new CommentEntity(articleId, targetUserId, "타겟 유저 댓글 1"));
    commentRepository.save(new CommentEntity(articleId, targetUserId, "타겟 유저 댓글 2"));
    commentRepository.save(new CommentEntity(articleId, otherUserId, "다른 유저 댓글"));

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

    commentRepository.save(new CommentEntity(articleId, targetUserId, "타겟 유저 댓글 1"));
    commentRepository.save(new CommentEntity(articleId, targetUserId, "타겟 유저 댓글 2"));
    commentRepository.save(new CommentEntity(articleId, otherUserId, "다른 유저 댓글"));

    entityManager.flush();
    entityManager.clear();


    LocalDateTime now = LocalDateTime.now();
    commentRepository.softDeleteAllByUserId(targetUserId, now);

    // Repository 인터페이스에 @Modifying(clearAutomatically = true)를 적어두었다면
    // 여기서 entityManager.clear()를 생략해도 됩니다! (옵션이 잘 먹는지 테스트해보는 것도 좋습니다)

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
}
