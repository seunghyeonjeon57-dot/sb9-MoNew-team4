package com.example.monew.domain.comment.service;

import com.example.monew.domain.article.repository.ArticleRepository;
import com.example.monew.domain.comment.dto.CommentDto;
import com.example.monew.domain.comment.dto.CommentRegisterRequest;
import com.example.monew.domain.comment.dto.CommentUpdateRequest;
import com.example.monew.domain.comment.entity.CommentEntity;
import com.example.monew.domain.comment.mapper.CommentMapper;
import com.example.monew.domain.comment.repository.CommentRepository;
import com.example.monew.domain.notification.event.CommentLikedEvent;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

  private final CommentRepository commentRepository;
  private final CommentMapper commentMapper;
  private final ArticleRepository articleRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public CommentDto registerComment(CommentRegisterRequest request) {
    articleRepository.findById(request.articleId())
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기사입니다."));
    CommentEntity comment = commentRepository.save(request.toEntity());
    return commentMapper.toDto(comment, null, false);
  }

  @Transactional
  public CommentDto updateComment(UUID commentId, UUID userId, CommentUpdateRequest request){
    CommentEntity comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

    if(!comment.getUserId().equals(userId)) {
      throw new IllegalArgumentException("댓글 수정 권한이 없습니다.");
    }

    comment.updateContent(request.content());
    return commentMapper.toDto(comment, null, false);
  }

  @Transactional
  public void softDeleteComment(UUID commentId){
    CommentEntity comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

    comment.delete();
  }

  @Transactional
  public void hardDeleteComment(UUID commentId){
    CommentEntity comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

    commentRepository.delete(comment);
  }

  @Transactional
  public void softDeleteAllByUserId(UUID userId){
    commentRepository.softDeleteAllByUserId(userId, LocalDateTime.now());
  }

  @Transactional
  public void hardDeleteAllByUserId(UUID userId){
    commentRepository.deleteAllByUserId(userId);
  }

  @Transactional
  public void likeComment(UUID commentId, UUID likerId, String likerName) {
    // 1. 좋아요가 눌린 댓글 조회
    CommentEntity comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

    // 2. 좋아요 수 증가 (CommentEntity에 likeCount를 1 증가시키는 메서드가 필요합니다)
    // 예: comment.incrementLikeCount();
    // (만약 Like 정보를 별도의 테이블로 관리하신다면 해당 레포지토리에 save 하는 로직이 들어갑니다.)

    // 3. 본인이 본인 댓글에 좋아요를 누른 경우 알림을 보내지 않음
    if (comment.getUserId().equals(likerId)) {
      return;
    }

    // 4. "[사용자]님이 나의 댓글을 좋아합니다." 문구를 완성
    eventPublisher.publishEvent(new CommentLikedEvent(
        comment.getUserId(), // 알림을 받을 사람 (댓글 작성자)
        comment.getId(),     // 관련 리소스 정보 (댓글 ID)
        likerName            // 좋아요를 누른 사람의 이름
    ));
  }
}
