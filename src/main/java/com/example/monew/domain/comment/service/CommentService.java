package com.example.monew.domain.comment.service;

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
import com.example.monew.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class  CommentService {

  private final CommentRepository commentRepository;
  private final CommentMapper commentMapper;
  private final ArticleRepository articleRepository;
  private final CommentLikeRepository commentLikeRepository;

  @Transactional
  public CommentDto registerComment(CommentRegisterRequest request) {
    log.info("댓글 등록 시도: articleId={}", request.articleId());
    articleRepository.findById(request.articleId())
        .orElseThrow(() -> {
          log.warn("댓글 등록 실패: 존재하지 않은 기사 articleId={}", request.articleId());
          return new ArticleNotFoundException(ErrorCode.ARTICLE_NOT_FOUND);
        });

    CommentEntity comment = commentRepository.save(request.toEntity());

    log.info("댓글 등록 완료: commentId={}", comment.getId());
    return commentMapper.toDto(comment, null, false);
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

    comment.incrementLikeCount();

    CommentLikeEntity commentLike = CommentLikeEntity.builder()
        .commentId(commentId)
        .userId(userId)
        .build();
    commentLikeRepository.save(commentLike);

    log.info("좋아요 추가 완료: userId={}, commentId={}", userId, commentId);
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

    commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);

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
    return new CursorPageResponseCommentDto(comments, nextCursor, nextAfter, limit, null, hasNext);
  }
}
