package com.example.monew.domain.comment.service;

import com.example.monew.domain.article.repository.ArticleRepository;
import com.example.monew.domain.comment.dto.CommentDto;
import com.example.monew.domain.comment.dto.CommentRegisterRequest;
import com.example.monew.domain.comment.entity.CommentEntity;
import com.example.monew.domain.comment.mapper.CommentMapper;
import com.example.monew.domain.comment.repository.CommentRepository;
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
}
