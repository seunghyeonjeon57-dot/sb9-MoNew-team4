package com.example.monew.domain.comment.service;

import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.exception.ArticleNotFoundException;
import com.example.monew.domain.article.repository.ArticleRepository;
import com.example.monew.domain.comment.dto.*;
import com.example.monew.domain.comment.entity.CommentEntity;
import com.example.monew.domain.comment.entity.CommentLikeEntity;
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
        .articleId(articleId).userId(userId).content("테스트 댓글").build();

    CommentEntity savedComment = CommentEntity.builder().id(UUID.randomUUID()).content(request.content()).build();
    CommentDto expectedDto = new CommentDto(savedComment.getId(), articleId, userId, "닉네임", "테스트 댓글", 0L, false, LocalDateTime.now());

    given(articleRepository.findById(articleId)).willReturn(Optional.of(ArticleEntity.builder().build()));
    given(commentRepository.save(any(CommentEntity.class))).willReturn(savedComment);
    given(commentMapper.toDto(any(), any(), eq(false))).willReturn(expectedDto);

    commentService.registerComment(request);

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
  @DisplayName("존재하지 않는 댓글을 수정하려고 하면 예외가 발생한다.")
  void updateComment_NotFound() {
    UUID fakeCommentId = UUID.randomUUID();
    UUID randomUserId = UUID.randomUUID();
    CommentUpdateRequest request = new CommentUpdateRequest("수정");

    given(commentRepository.findById(fakeCommentId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> commentService.updateComment(fakeCommentId, randomUserId, request))
        .isInstanceOf(CommentNotFoundException.class);
  }

  
  @Test
  @DisplayName("작성자가 아닌 사용자가 수정을 요청하면 예외가 발생한다.")
  void updateComment_Unauthorized() {
    UUID commentId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID requesterId = UUID.randomUUID();
    CommentUpdateRequest request = new CommentUpdateRequest("수정");

    given(commentRepository.findById(commentId)).willReturn(Optional.of(CommentEntity.builder().userId(ownerId).build()));

    assertThatThrownBy(() -> commentService.updateComment(commentId, requesterId, request))
        .isInstanceOf(CommentAccessDenied.class);
  }

  
  @Test
  @DisplayName("댓글 삭제 시 논리 삭제(Soft Delete) 처리가 되어야 한다.")
  void deleteComment_SoftDelete() {
    UUID commentId = UUID.randomUUID();
    CommentEntity comment = CommentEntity.builder().build();

    given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

    commentService.softDeleteComment(commentId);

    assertThat(comment.getDeletedAt()).isNotNull();
  }

  
  @Test
  @DisplayName("특정 사용자의 모든 댓글을 논리 삭제하도록 레포지토리에 위임한다.")
  void softDeleteByUserId() {
    UUID userId = UUID.randomUUID();
    commentService.softDeleteAllByUserId(userId);
    verify(commentRepository, times(1)).softDeleteAllByUserId(eq(userId), any(LocalDateTime.class));
  }

  
  @Test
  @DisplayName("특정 사용자의 모든 댓글을 물리 삭제하도록 레포지토리에 위임한다.")
  void hardDeleteAllByUserId() {
    UUID userId = UUID.randomUUID();
    commentService.hardDeleteAllByUserId(userId);
    verify(commentRepository, times(1)).deleteAllByUserId(userId);
  }

  
  @Test
  @DisplayName("댓글에 좋아요를 누르면 좋아요 테이블에 기록이 남고, 댓글의 좋아요 수가 1 증가한다.")
  void addLike_Success() {
    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    CommentEntity comment = CommentEntity.builder().likeCount(0L).build();

    given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
    given(commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)).willReturn(false);

    commentService.addLike(commentId, userId);

    verify(commentLikeRepository, times(1)).save(any(CommentLikeEntity.class));
    assertThat(comment.getLikeCount()).isEqualTo(1L);
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
  @DisplayName("뉴스 기사 별 댓글 조회 시 좋아요 순 정렬과 커서 페이징이 정상 동작한다.")
  void getArticleComments_LikesSort_Success() {
    UUID articleId = UUID.randomUUID();
    int size = 1;
    CommentActivityDto c1 = CommentActivityDto.builder().id(UUID.randomUUID()).createdAt(LocalDateTime.now()).build();
    CommentActivityDto c2 = CommentActivityDto.builder().id(UUID.randomUUID()).createdAt(LocalDateTime.now()).build();
    List<CommentActivityDto> mockResult = new ArrayList<>(List.of(c1, c2));

    given(articleRepository.existsById(articleId)).willReturn(true);
    given(commentRepository.findCommentsByArticleWithCursor(eq(articleId), any(), any(), any(), eq("LIKES"), eq(size)))
        .willReturn(mockResult);

    CursorPageResponseCommentDto response = commentService.getArticleComments(articleId, null, null, null, "LIKES", size);

    assertThat(response.content()).hasSize(1);
    assertThat(response.hasNext()).isTrue();
    assertThat(response.nextCursor()).isEqualTo(c1.id().toString());
  }

  
  @Test
  @DisplayName("뉴스 기사 별 댓글 조회 시 날짜 순 정렬과 커서 페이징이 정상 동작한다.")
  void getArticleComments_DateSort_Success() {
    UUID articleId = UUID.randomUUID();
    int size = 10;
    CommentActivityDto comment = CommentActivityDto.builder().id(UUID.randomUUID()).createdAt(LocalDateTime.now()).build();
    List<CommentActivityDto> mockResult = new ArrayList<>(List.of(comment));

    given(articleRepository.existsById(articleId)).willReturn(true);
    given(commentRepository.findCommentsByArticleWithCursor(eq(articleId), any(), any(), any(), eq("DATE"), eq(size)))
        .willReturn(mockResult);

    CursorPageResponseCommentDto response = commentService.getArticleComments(articleId, null, null, null, "DATE", size);

    assertThat(response.content()).hasSize(1);
    assertThat(response.hasNext()).isFalse();
  }
}