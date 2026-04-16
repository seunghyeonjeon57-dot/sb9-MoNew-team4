package com.example.monew.domain.comment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.monew.domain.comment.entity.CommentEntity;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
public class CommentRepositoryTest {
  // RED 원인: CommentRepository 인터페이스를 아직 만들지 않아서 컴파일 에러 발생
  @Autowired
  private CommentRepository commentRepository;

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


}
