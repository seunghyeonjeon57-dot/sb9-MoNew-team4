package com.example.monew.domain.comment.service;

import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.exception.ArticleNotFoundException;
import com.example.monew.domain.article.repository.ArticleRepository;
import com.example.monew.domain.comment.dto.*;
import com.example.monew.domain.comment.entity.CommentEntity;
import com.example.monew.domain.comment.exception.CommentAccessDenied;
import com.example.monew.domain.comment.exception.CommentDuplicateLike;
import com.example.monew.domain.comment.exception.CommentNotFoundException;
import com.example.monew.domain.comment.mapper.CommentMapper;
import com.example.monew.domain.comment.repository.CommentLikeRepository;
import com.example.monew.domain.comment.repository.CommentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {

  @Mock private CommentRepository commentRepository;
  @Mock private CommentMapper commentMapper;
  @Mock private ArticleRepository articleRepository;
  @Mock private CommentLikeRepository commentLikeRepository;

  @InjectMocks
  private CommentService commentService;

  @Test
  @DisplayName("요청 DTO를 받아 댓글을 성공적으로 등록한다.")
  void registerComment() {
    
    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    CommentRegisterRequest request = CommentRegisterRequest.builder()
        .articleId(articleId)
        .userId(userId)
        .content("테스트 댓글")
        .build();

    CommentEntity savedComment = CommentEntity.builder()
        .id(UUID.randomUUID())
        .content(request.content())
        .build();

    
    CommentDto expectedDto = new CommentDto(
        savedComment.getId(),
        articleId,
        userId,
        "테스트유저",
        "테스트 댓글",
        0L,
        false,
        LocalDateTime.now()
    );

    given(articleRepository.findById(articleId)).willReturn(Optional.of(ArticleEntity.builder().build()));
    given(commentRepository.save(any(CommentEntity.class))).willReturn(savedComment);
    given(commentMapper.toDto(any(CommentEntity.class), eq(null), eq(false))).willReturn(expectedDto);

    
    CommentDto result = commentService.registerComment(request);

    
    assertThat(result.content()).isEqualTo("테스트 댓글");
    verify(commentRepository, times(1)).save(any(CommentEntity.class));
  }

  @Test
  @DisplayName("존재하지 않는 기사 ID로 댓글을 등록하면 예외가 발생한다.")
  void registerComment_ArticleNotFound() {
    UUID articleId = UUID.randomUUID();
    CommentRegisterRequest request = CommentRegisterRequest.builder().articleId(articleId).build();

    given(articleRepository.findById(articleId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> commentService.registerComment(request))
        .isInstanceOf(ArticleNotFoundException.class);
  }

  @Test
  @DisplayName("작성자가 아닌 사용자가 수정을 요청하면 예외가 발생한다.")
  void updateComment_Unauthorized() {
    UUID commentId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID requesterId = UUID.randomUUID();
    CommentUpdateRequest request = new CommentUpdateRequest("수정 내용");

    given(commentRepository.findById(commentId))
        .willReturn(Optional.of(CommentEntity.builder().userId(ownerId).build()));

    assertThatThrownBy(() -> commentService.updateComment(commentId, requesterId, request))
        .isInstanceOf(CommentAccessDenied.class);
  }

  @Test
  @DisplayName("이미 좋아요를 누른 댓글에 다시 좋아요를 누르면 예외가 발생한다.")
  void addLike_AlreadyLiked() {
    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    given(commentRepository.findById(commentId)).willReturn(Optional.of(CommentEntity.builder().build()));
    given(commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)).willReturn(true);

    assertThatThrownBy(() -> commentService.addLike(commentId, userId))
        .isInstanceOf(CommentDuplicateLike.class);
  }

  @Test
  @DisplayName("뉴스 기사 별 댓글 조회 시 페이징과 hasNext가 정상 동작한다.")
  void getArticleComments_Pagination_Success() {
    UUID articleId = UUID.randomUUID();
    int size = 1;

    CommentActivityDto c1 = CommentActivityDto.builder().id(UUID.randomUUID()).createdAt(LocalDateTime.now()).build();
    CommentActivityDto c2 = CommentActivityDto.builder().id(UUID.randomUUID()).createdAt(LocalDateTime.now()).build();
    List<CommentActivityDto> mockResult = new ArrayList<>(List.of(c1, c2));

    given(articleRepository.existsById(articleId)).willReturn(true);
    given(commentRepository.findCommentsByArticleWithCursor(eq(articleId), any(), any(), any(), eq("DATE"), eq(size)))
        .willReturn(mockResult);

    CursorPageResponseCommentDto response = commentService.getArticleComments(articleId, null, null, null, "DATE", size);

    assertThat(response.content()).hasSize(1);
    assertThat(response.hasNext()).isTrue();
  }
}