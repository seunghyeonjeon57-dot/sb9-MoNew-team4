package com.example.monew.domain.comment.repository;


import static org.assertj.core.api.Assertions.assertThat;

import com.example.monew.domain.comment.entity.CommentLikeEntity;
import jakarta.persistence.Table;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
public class CommentLikeRepositoryTest {

  @Autowired
  private CommentLikeRepository commentLikeRepository;

  @Autowired
  private TestEntityManager entityManager;

  @Test
  @DisplayName("특정 댓글에 특정 유저가 좋아요를 눌렀는지 확인한다.")
  void existCommentIdAndUserId_Test() {
    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    commentLikeRepository.save(new CommentLikeEntity(commentId, userId));

    entityManager.flush();
    entityManager.clear();

    boolean exists = commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);
    boolean noExists = commentLikeRepository.existsByCommentIdAndUserId(commentId, UUID.randomUUID());

    assertThat(exists).isTrue();
    assertThat(noExists).isFalse();
  }

  @Test
  @DisplayName("댓글 ID와 유저 ID로 좋아요 기록을 삭제한다")
  void deleteCommentIdAndUserId_Test() {
    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    commentLikeRepository.save(new CommentLikeEntity(commentId, userId));

    entityManager.flush();
    entityManager.clear();

    commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);

    entityManager.flush();
    entityManager.clear();

    boolean exists = commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);
    assertThat(exists).isFalse();
  }
}
