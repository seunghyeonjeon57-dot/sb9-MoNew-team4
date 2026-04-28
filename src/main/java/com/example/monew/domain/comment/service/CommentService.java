package com.example.monew.domain.comment.service;

import com.example.monew.domain.activity.dto.CommentActivityDto;
import com.example.monew.domain.activity.dto.CommentLikeActivityDto;
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
import com.example.monew.domain.comment.exception.CommentLikeNotFound;
import com.example.monew.domain.comment.exception.CommentNotFoundException;
import com.example.monew.domain.comment.mapper.CommentMapper;
import com.example.monew.domain.comment.repository.CommentLikeRepository;
import com.example.monew.domain.comment.repository.CommentRepository;
import com.example.monew.domain.notification.event.CommentLikedEvent;
import com.example.monew.domain.user.entity.User;
import com.example.monew.domain.user.exception.UserNotFoundException;
import com.example.monew.domain.user.repository.UserRepository;
import com.example.monew.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

  private final CommentRepository commentRepository;
  private final CommentMapper commentMapper;
  private final ArticleRepository articleRepository;
  private final CommentLikeRepository commentLikeRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final UserRepository userRepository;
  private final ActivityService activityService;

  @Transactional
  public CommentDto registerComment(CommentRegisterRequest request) {
    log.info("댓글 등록 시도: articleId={}", request.articleId());

    // 1. 엔티티 조회 (기존 유지)
    ArticleEntity article = articleRepository.findById(request.articleId())
        .orElseThrow(() -> new ArticleNotFoundException(ErrorCode.ARTICLE_NOT_FOUND));
    User user = userRepository.findById(request.userId())
        .orElseThrow(() -> new UserNotFoundException("유저를 찾을 수 없습니다."));

    // 2. 중요: save 대신 saveAndFlush를 써서 즉시 INSERT를 강제하세요.
    // 여기서 INSERT가 안 찍히면 연관관계 매핑이나 엔티티 설정을 뜯어봐야 합니다.
    CommentEntity comment = commentRepository.saveAndFlush(request.toEntity());

    // 3. 벌크 업데이트 수행 (댓글 수 증가)
    articleRepository.incrementCommentCount(request.articleId());
    activityService.incrementCommentCountInRecentArticles(request.articleId());

    // 4. 활동 내역 업데이트는 '가장 마지막'에 수행
    CommentActivityDto activityDto = CommentActivityDto.builder()
        .id(comment.getId())
        .articleId(request.articleId())
        .articleTitle(article.getTitle())
        .userId(request.userId())
        .userNickname(user.getNickname())
        .content(comment.getContent())
        .likeCount(comment.getLikeCount())
        .createdAt(comment.getCreatedAt())
        .build();

    activityService.updateRecentComments(request.userId(), activityDto);

    log.info("댓글 등록 완료: commentId={}", comment.getId());
    return commentMapper.toDto(comment, user.getNickname(), false);
  }

  @Transactional
  public CommentDto updateComment(UUID commentId, UUID userId, CommentUpdateRequest request){
    log.info("댓글 수정 시도: commentId={}, userId={}", commentId, userId);
    CommentEntity comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
        .orElseThrow(() -> {
          log.warn("댓글 수정 실패: 존재하지 않는 댓글입니다. commentId={}", commentId);
          return new CommentNotFoundException(ErrorCode.COMMENT_NOT_FOUND);
        });

    if(!comment.getUserId().equals(userId)) {
      log.warn("댓글 수정 권한 없음: userID={}, commentId={}", userId, commentId);
      throw new CommentAccessDenied(ErrorCode.COMMENT_ACCESS_DENIED);
    }

    comment.updateContent(request.content());

    activityService.updateRecentCommentsInactivity(userId, commentId, request.content());

    log.info("댓글 수정 완료: commentId={}", commentId);
    return commentMapper.toDto(comment, null, false);
  }

  @Transactional
  public void softDeleteComment(UUID commentId){
    log.info("댓글 논리 삭제 시도: commentId={}", commentId);

    CommentEntity comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
        .orElseThrow(() -> {
          log.warn("댓글 논리 삭제 실패: 존재하지 않는 댓글 CommentID={}", commentId);
          return new CommentNotFoundException(ErrorCode.COMMENT_NOT_FOUND);
        });

    comment.markDeleted();
    commentRepository.saveAndFlush(comment);
    articleRepository.decrementCommentCount(comment.getArticleId());
    activityService.decrementCommentCountInRecentArticles(comment.getArticleId());
    log.info("댓글 논리 삭제 완료: commentId={}", commentId);
  }

  @Transactional
  public void hardDeleteComment(UUID commentId){
    log.info("댓글 물리 삭제 시도: commentId={}", commentId);

    CommentEntity comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
        .orElseThrow(() -> {
          log.warn("댓글 물리 삭제 실패: 존재 하지 않는 댓글 commentId={}", commentId);
          return new CommentNotFoundException(ErrorCode.COMMENT_NOT_FOUND);
        });

    commentRepository.delete(comment);
    articleRepository.decrementCommentCount(comment.getArticleId());
    activityService.decrementCommentCountInRecentArticles(comment.getArticleId());
    log.info("댓글 물리 삭제 완료: commentId={}", commentId);
  }

  @Transactional
  public void softDeleteAllByUserId(UUID userId){
    log.info("사용자 댓글 일괄 논리 삭제 시도: userID={}", userId);
    long updatedCount = commentRepository.softDeleteAllByUserId(userId);

    log.info("사용자 댓글 {}개 일괄 논리 삭제 완료", updatedCount);
  }

  @Transactional
  public void deleteAllByUserId(UUID userId){
    log.info("사용자 댓글 일괄 물리 삭제 시도: userId={}", userId);
    long deletedCount =commentRepository.deleteAllByUserId(userId);
    log.info("사용자 댓글 {}개 일괄 물리 삭제 완료", deletedCount);
  }

  @Transactional
  public void addLike(UUID commentId, UUID userId) {
    log.info("좋아요 추가 시도: userId={}, commentId={}", userId, commentId);
    CommentEntity comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
        .orElseThrow(() -> {
          log.warn("좋아요 추가 실패: 존재하지 않는 댓글 ID={}", commentId);
          return new CommentNotFoundException(ErrorCode.COMMENT_NOT_FOUND);
        });

    if(commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
      log.warn("좋아요 추가 실패: 이미 좋아요를 누른 댓글, userId={}, commentId={}", userId, commentId);
      throw new CommentDuplicateLike(ErrorCode.DUPLICATE_LIKE);
    }

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("유저를 찾을 수 없습니다."));

    ArticleEntity article = articleRepository.findById(comment.getArticleId())
        .orElseThrow(() -> new ArticleNotFoundException(ErrorCode.ARTICLE_NOT_FOUND));

    comment.incrementLikeCount();

    CommentLikeEntity commentLike = CommentLikeEntity.builder()
        .commentId(commentId)
        .userId(userId)
        .build();
    commentLikeRepository.save(commentLike);

    activityService.commentLikeCountInRecentComments(
        comment.getUserId(),
        commentId,
        comment.getLikeCount()
    );

    CommentLikeActivityDto activityDto = CommentLikeActivityDto.builder()
        .id(UUID.randomUUID())
        .createdAt(LocalDateTime.now())
        .commentId(comment.getId())
        .articleId(comment.getArticleId())
        .articleTitle(article.getTitle())
        .commentUserId(comment.getUserId())
        .commentUserNickname(user.getNickname())
        .commentContent(comment.getContent())
        .commentLikeCount(comment.getLikeCount())
        .commentCreatedAt(comment.getCreatedAt())
        .build();
    activityService.updateRecentLikedComments(userId, activityDto);

    log.info("좋아요 추가 완료: userId={}, commentId={}", userId, commentId);

    User liker = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND.getMessage()));

    eventPublisher.publishEvent(new CommentLikedEvent(
        comment.getUserId(),
        comment.getId(),
        userId,
        liker.getNickname()
    ));
  }

  @Transactional
  public void removeLike(UUID commentId, UUID userId) {
    log.info("좋아요 취소 시도: userId={}, commentId={}", userId, commentId);

    CommentEntity comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
        .orElseThrow(() -> {
          log.warn("좋아요 취소 실패: 존재하지 않는 댓글 ID={}", commentId);
          return new CommentNotFoundException(ErrorCode.COMMENT_NOT_FOUND);
        });

    if(!commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
      log.warn("좋아요 취소 실패: 좋아요를 누르지 않은 댓글, userId={}, commentId={}", userId, commentId);
      throw new CommentLikeNotFound(ErrorCode.LIKE_NOT_FOUND);
    }

    comment.decrementLikeCount();

    activityService.commentLikeCountInRecentComments(
        comment.getUserId(),
        commentId,
        comment.getLikeCount()
    );

    activityService.removeCommentLikeInActivity(userId, commentId);
    commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);

    activityService.removeRecentLikedComments(userId, commentId);

    log.info("좋아요 취소 완료: userId={}, commentId={}", userId, commentId);
  }

  @Transactional(readOnly = true)
  public CursorPageResponseCommentDto getArticleComments(
      UUID articleId,
      UUID userId,
      String cursor,
      LocalDateTime after,
      String orderBy,
      String direction,
      int limit
  ) {
    log.info("댓글 목록 조회 요청: articleId={}, orderBy={}, direction={}, limit={}", articleId, orderBy, direction, limit);

    if (!articleRepository.existsById(articleId)) {
      throw new ArticleNotFoundException(ErrorCode.ARTICLE_NOT_FOUND);
    }

    List<CommentDto> comments = new ArrayList<>(
        commentRepository.findCommentsByArticleWithCursor(
            articleId, userId, cursor, after, orderBy, direction, limit + 1
        )
    );

    boolean hasNext = comments.size() > limit;
    if (hasNext) {
      comments.remove(limit);
    }

    String nextCursor = null;
    LocalDateTime nextAfter = null;

    if (hasNext && !comments.isEmpty()) {
      CommentDto lastComment = comments.get(comments.size() - 1);
      nextAfter = lastComment.createdAt();

      if ("likeCount".equals(orderBy)) {
        nextCursor = String.format("%d_%s", lastComment.likeCount(), lastComment.id());
      } else {
        nextCursor = lastComment.id().toString();
      }
    }

    log.info("댓글 목록 조회 완료: 조회된 건수={}, hasNext={}", comments.size(), hasNext);
    return new CursorPageResponseCommentDto(comments, nextCursor, nextAfter, comments.size(), null, hasNext);
  }
}
