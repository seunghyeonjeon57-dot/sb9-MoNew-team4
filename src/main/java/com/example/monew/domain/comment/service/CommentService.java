package com.example.monew.domain.comment.service;

import com.example.monew.domain.comment.dto.CommentRegisterRequest;
import com.example.monew.domain.comment.entity.CommentEntity;
import com.example.monew.domain.comment.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

  private final CommentRepository commentRepository;

  @Transactional
  public void registerComment(CommentRegisterRequest request) {
    CommentEntity comment = new CommentEntity(
        request.articleId(),
        request.userId(),
        request.content()
    );
    commentRepository.save(comment);
  }
}
