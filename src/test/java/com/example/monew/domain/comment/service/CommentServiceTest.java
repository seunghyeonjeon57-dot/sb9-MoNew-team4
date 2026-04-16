package com.example.monew.domain.comment.service;

import com.example.monew.domain.comment.dto.CommentRegisterRequest;
import com.example.monew.domain.comment.entity.CommentEntity;
import com.example.monew.domain.comment.repository.CommentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DataJpaTest
public class CommentServiceTest {
  @Mock
  private CommentRepository commentRepository;

  // RED 원인: CommentService 클래스와 registerComment 메서드가 없어서 컴파일 에러 발생
  @InjectMocks
  private CommentService commentService;

  @Test
  @DisplayName("요청 DTO를 받아 댓글을 성공적으로 등록한다.")
  void registerComment() {
    CommentRegisterRequest request = new CommentRegisterRequest(
        UUID.randomUUID(), UUID.randomUUID(), "서비스 테스트 댓글"
    );

    commentService.registerComment(request);

    verify(commentRepository, times(1)).save(any(CommentEntity.class));
  }
}
