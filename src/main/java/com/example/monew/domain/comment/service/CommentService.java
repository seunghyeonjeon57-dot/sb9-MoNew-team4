package com.example.monew.domain.comment.service;

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

  @Transactional
  public CommentDto registerComment(CommentRegisterRequest request) {
    CommentEntity comment = commentRepository.save(request.toEntity());
    return commentMapper.toDto(comment, null, false);
  }
}
