package com.example.monew.domain.comment.service;

import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.repository.ArticleRepository;
import com.example.monew.domain.comment.dto.CommentDto;
import com.example.monew.domain.comment.dto.CommentRegisterRequest;
import com.example.monew.domain.comment.entity.CommentEntity;
import com.example.monew.domain.comment.mapper.CommentMapper;
import com.example.monew.domain.comment.repository.CommentRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {
  @Mock
  private CommentRepository commentRepository;
  @Mock
  private CommentMapper commentMapper;
  @Mock
  private ArticleRepository articleRepository;
  @InjectMocks
  private CommentService commentService;

  @Test
  @DisplayName("요청 DTO를 받아 댓글을 성공적으로 등록한다.")
  void registerComment() {
    CommentRegisterRequest request = new CommentRegisterRequest(
        UUID.randomUUID(), UUID.randomUUID(), "서비스 테스트 댓글"
    );
    ArticleEntity article = ArticleEntity.builder().build();
    given(articleRepository.findById(request.articleId())).willReturn(Optional.of(article));

    CommentDto result = commentService.registerComment(request);

    verify(commentRepository, times(1)).save(any(CommentEntity.class));
  }

  @Test
  @DisplayName("존재하지 않는 기사 ID로 댓글을 등록하면 예외가 발생한다.")
  void registerComment_ArticleNotFound() {

    UUID fakeArticleId = UUID.randomUUID();
    CommentRegisterRequest request = new CommentRegisterRequest(
        fakeArticleId,
        UUID.randomUUID(),
        "기사가 없는 유령 댓글"
    );

    given(articleRepository.findById(fakeArticleId)).willReturn(Optional.empty());


    assertThatThrownBy(() -> commentService.registerComment(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("존재하지 않는 기사입니다.");
  }
}
