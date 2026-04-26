package com.example.monew.domain.comment.service;

import com.example.monew.domain.activity.service.ActivityService;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.exception.ArticleNotFoundException;
import com.example.monew.domain.article.repository.ArticleRepository;
import com.example.monew.domain.comment.dto.CommentDto;
import com.example.monew.domain.comment.dto.CommentRegisterRequest;
import com.example.monew.domain.comment.dto.CommentUpdateRequest;
import com.example.monew.domain.comment.dto.CursorPageResponseCommentDto;
import com.example.monew.domain.comment.entity.CommentEntity;
import com.example.monew.domain.comment.entity.CommentLikeEntity;
import com.example.monew.domain.comment.exception.CommentAccessDenied;
import com.example.monew.domain.comment.exception.CommentDuplicateLike;
import com.example.monew.domain.comment.exception.CommentNotFoundException;
import com.example.monew.domain.comment.mapper.CommentMapper;
import com.example.monew.domain.comment.repository.CommentLikeRepository;
import com.example.monew.domain.comment.repository.CommentRepository;
import com.example.monew.domain.notification.event.CommentLikedEvent;
import com.example.monew.domain.user.entity.User;
import com.example.monew.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {

  @Mock
  private CommentRepository commentRepository;

  @Mock
  private CommentMapper commentMapper;

  @Mock
  private ArticleRepository articleRepository;

  @Mock
  private CommentLikeRepository commentLikeRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ActivityService activityService;

  @InjectMocks
  private CommentService commentService;


  @Test
  @DisplayName("요청 DTO를 받아 댓글을 성공적으로 등록한다.")
  void registerComment() {
    CommentRegisterRequest request = CommentRegisterRequest.builder()
        .articleId(UUID.randomUUID())
        .userId(UUID.randomUUID())
        .content("서비스 테스트 댓글")
        .build();

    ArticleEntity article = ArticleEntity.builder().build();
    User user = User.builder().build();

    given(articleRepository.findById(request.articleId())).willReturn(Optional.of(article)); // 이후에 변경
    given(userRepository.findById(request.userId())).willReturn(Optional.of(user)); // 추가
    given(commentRepository.save(any(CommentEntity.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    commentService.registerComment(request);
    verify(commentRepository, times(1)).save(any(CommentEntity.class));
  }

  @Test
  @DisplayName("존재하지 않는 기사 ID로 댓글을 등록하면 예외가 발생한다.")
  void registerComment_ArticleNotFound() {

    UUID articleId = UUID.randomUUID();
    CommentRegisterRequest request = CommentRegisterRequest.builder()
        .articleId(articleId)
        .userId(UUID.randomUUID())
        .content("기사가 없는 유령 댓글")
        .build();

    given(articleRepository.findById(articleId)).willReturn(Optional.empty()); // 이후에 변경


    assertThatThrownBy(() -> commentService.registerComment(request))
        .isInstanceOf(ArticleNotFoundException.class)
        .hasMessage("해당 기사를 찾을 수 없습니다.");
  }

  @Test
  @DisplayName("존재하지 않는 댓글을 수정하려고 하면 예외가 발생한다.")
  void updateComment_NotFound() {

    UUID fakeCommentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    CommentUpdateRequest request = CommentUpdateRequest.builder()
        .content("수정 내용")
        .build();

    given(commentRepository.findByIdAndDeletedAtIsNull(fakeCommentId)).willReturn(Optional.empty());


    assertThatThrownBy(() -> commentService.updateComment(fakeCommentId, userId, request))
        .isInstanceOf(CommentNotFoundException.class)
        .hasMessage("해당 댓글을 찾을 수 없습니다.");
  }

  @Test
  @DisplayName("작성자가 아닌 사용자가 수정을 요청하면 예외가 발생한다.")
  void updateComment_Unauthorized() {
    UUID commentId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID requesterId = UUID.randomUUID(); // 다른 사용자
    CommentUpdateRequest request = CommentUpdateRequest.builder()
        .content("수정 내용")
        .build();

    CommentEntity existingComment = CommentEntity.builder()
        .articleId(UUID.randomUUID())
        .userId(ownerId)
        .content("원본 내용")
        .likeCount(0L)
        .build();

    given(commentRepository.findByIdAndDeletedAtIsNull(any(UUID.class)))
        .willReturn(Optional.of(existingComment));

    assertThatThrownBy(() -> commentService.updateComment(commentId, requesterId, request))
        .isInstanceOf(CommentAccessDenied.class)
        .hasMessage("댓글 작성자만 삭제할 수 있습니다.");
  }

  @Test
  @DisplayName("댓글 삭제 시 논리 삭제(Soft Delete) 처리가 되어야 한다.")
  void deleteComment_SoftDelete() {
    UUID commentId = UUID.randomUUID();

    CommentEntity comment = CommentEntity.builder()
        .id(commentId)
        .articleId(UUID.randomUUID())
        .userId(UUID.randomUUID())
        .content("삭제될 댓글")
        .likeCount(0L)
        .build();

    given(commentRepository.findByIdAndDeletedAtIsNull(commentId)).willReturn(Optional.of(comment));

    commentService.softDeleteComment(commentId);

    // 논리 삭제이므로 DB에서 지워지는게 아니라 삭제 시간이 기록되어야 함
    assertThat(comment.getDeletedAt()).isNotNull();
  }


  @Test
  @DisplayName("특정 사용자의 모든 댓글을 논리 삭제하도록 레포지토리에 위임한다.")
  void softDeleteByUserId() {
    UUID userId = UUID.randomUUID();

    commentService.softDeleteAllByUserId(userId);

    verify(commentRepository, times(1)).softDeleteAllByUserId(userId);
  }

  @Test
  @DisplayName("특정 사용자의 모든 댓글을 물리 삭제하도록 레포지토리에 위임한다.")
  void hardDeleteAllByUserId() {
    UUID userId = UUID.randomUUID();

    commentService.deleteAllByUserId(userId);

    verify(commentRepository, times(1)).deleteAllByUserId(userId);
  }

  @Test
  @DisplayName("댓글에 좋아요를 누르면 좋아요 테이블에 기록이 남고, 댓글의 좋아요 수가 1 증가한다.")
  void addLike_Success() {
    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID commentAuthorId = UUID.randomUUID();

    User liker = User.builder().nickname("tester").build();

    UUID articleId = UUID.randomUUID();

    User user = User.builder()
        .nickname("tester")
        .email("test@test.com")
        .password("pw")
        .build();

    CommentEntity comment = CommentEntity.builder()
        .id(commentId)
        .articleId(UUID.randomUUID())
        .userId(userId) // [수정] userId를 명시적으로 맞춰주는 것이 더 정확합니다.
        .content("좋아요 받을 댓글")
        .likeCount(0L)
        .build();

    ArticleEntity article = ArticleEntity.builder().build();
    // 중복된 given을 제거하고 깔끔하게 한 번씩만 작성합니다.
    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(commentRepository.findByIdAndDeletedAtIsNull(commentId)).willReturn(Optional.of(comment));
    given(commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)).willReturn(false);
    given(userRepository.findById(userId)).willReturn(Optional.of(liker));

    given(articleRepository.findById(any(UUID.class))).willReturn(Optional.of(article));

    // when
    commentService.addLike(commentId, userId);

    // then
    verify(eventPublisher, times(1)).publishEvent(any(CommentLikedEvent.class));
    verify(commentLikeRepository, times(1)).save(any(CommentLikeEntity.class));
    assertThat(comment.getLikeCount()).isEqualTo(1L);

    ArgumentCaptor<CommentLikedEvent> captor = ArgumentCaptor.forClass(CommentLikedEvent.class);
    verify(eventPublisher, times(1)).publishEvent(captor.capture());

    CommentLikedEvent event = captor.getValue();

    assertThat(event.receiverId()).isEqualTo(userId); // 알림을 받는 사람이 댓글 작성자가 맞는지
    assertThat(event.likerNickname()).isEqualTo("tester"); // 좋아요 누른 사람 닉네임이 맞는지
    assertThat(event.commentId()).isEqualTo(commentId); // 좋아요가 눌린 댓글 ID가 맞는지
  }

  @Test
  @DisplayName("이미 좋아요를 누른 댓글에 다시 좋아요를 누르면 예외가 발생한다.")
  void addLike_AlreadyLiked() {
    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    CommentEntity comment = CommentEntity.builder()
        .articleId(UUID.randomUUID())
        .userId(commentId)
        .content("댓글")
        .likeCount(0L)
        .build();

    given(commentRepository.findByIdAndDeletedAtIsNull(commentId)).willReturn(Optional.of(comment));

    given(commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)).willReturn(true);

    assertThatThrownBy(() -> commentService.addLike(commentId, userId))
        .isInstanceOf(CommentDuplicateLike.class)
        .hasMessage("이미 좋아요를 누른 댓글입니다.");
  }

  @Test
  @DisplayName("뉴스 기사 별 댓글 조회 시 좋아요 순 정렬과 커서 페이징이 정상 동작한다.")
  void getArticleComments_LikesSort_Success() {
    UUID articleId = UUID.randomUUID();
    UUID currentUserId = UUID.randomUUID();

    given(articleRepository.existsById(any(UUID.class))).willReturn(true);

    CommentDto comment1 = new CommentDto(
        UUID.randomUUID(),
        articleId,
        UUID.randomUUID(),
        "test",
        "content",
        10L,
        true,
        LocalDateTime.now()
    );

    CommentDto comment2 = new CommentDto(
        UUID.randomUUID(),
        articleId,
        UUID.randomUUID(),
        "test",
        "content",
        5L,
        false,
        LocalDateTime.now()
    );

    List<CommentDto> mockResult = List.of(comment1, comment2);

    given(commentRepository.findCommentsByArticleWithCursor(
        eq(articleId),
        eq(currentUserId),
        isNull(),
        isNull(),
        eq("likeCount"),
        eq("DESC"),
        eq(2)
    )).willReturn(mockResult);

    CursorPageResponseCommentDto response = commentService.getArticleComments(
        articleId,
        currentUserId,
        null,
        null,
        "likeCount",
        "DESC",
        1
    );

    assertThat(response.content()).hasSize(1);
    assertThat(response.hasNext()).isTrue();

    String expectedNextCursor = String.format("%d_%s",
        comment1.likeCount(),
        comment1.id()
    );

    assertThat(response.nextCursor()).isEqualTo(expectedNextCursor);
  }

  @Test
  @DisplayName("뉴스 기사 별 댓글 조회 시 날짜 순 정렬과 커서 페이징이 정상 동작한다.")
  void getArticleComments_DateSort_Success() {
    UUID articleId = UUID.randomUUID();
    UUID currentUserId = UUID.randomUUID();

    given(articleRepository.existsById(articleId)).willReturn(true);

    CommentDto comment = new CommentDto(
        UUID.randomUUID(),
        articleId,
        UUID.randomUUID(),
        "test",
        "content",
        1L,
        false,
        LocalDateTime.now()
    );

    List<CommentDto> mockResult = List.of(comment);

    given(commentRepository.findCommentsByArticleWithCursor(
        eq(articleId),
        eq(currentUserId),
        isNull(),
        isNull(),
        eq("createdAt"),
        eq("DESC"),
        eq(11)
    )).willReturn(mockResult);

    CursorPageResponseCommentDto response = commentService.getArticleComments(
        articleId,
        currentUserId,
        null,
        null,
        "createdAt",
        "DESC",
        10
    );

    assertThat(response.content()).hasSize(1);
    assertThat(response.hasNext()).isFalse();
  }
}
