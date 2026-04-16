package com.example.monew.domain.comment.controller;

import com.example.monew.domain.comment.dto.CommentRegisterRequest;
import com.example.monew.domain.comment.entity.CommentEntity;
import com.example.monew.domain.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController {
  private final CommentService commentService;

  @PostMapping
  public ResponseEntity<CommentRegisterRequest> responseEntity(
      @RequestBody @Valid CommentRegisterRequest request
  ) {
    commentService.registerComment(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(request);
  }
}
