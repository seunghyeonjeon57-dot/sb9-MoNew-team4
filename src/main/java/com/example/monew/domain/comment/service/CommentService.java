package com.example.monew.domain.comment.service;

import com.example.monew.domain.article.repository.ArticleRepository;
import com.example.monew.domain.comment.dto.CommentDto;
import com.example.monew.domain.comment.dto.CommentRegisterRequest;
import com.example.monew.domain.comment.dto.CommentUpdateRequest;
import com.example.monew.domain.comment.entity.CommentEntity;
import com.example.monew.domain.comment.mapper.CommentMapper;
import com.example.monew.domain.comment.repository.CommentRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

  private final CommentRepository commentRepository;
  private final CommentMapper commentMapper;
  private final ArticleRepository articleRepository;

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
}
