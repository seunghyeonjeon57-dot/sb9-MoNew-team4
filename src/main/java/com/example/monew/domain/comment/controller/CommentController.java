package com.example.monew.domain.comment.controller;

import com.example.monew.domain.comment.dto.CommentDto;
import com.example.monew.domain.comment.dto.CommentRegisterRequest;
import com.example.monew.domain.comment.dto.CommentUpdateRequest;
import com.example.monew.domain.comment.entity.CommentEntity;
import com.example.monew.domain.comment.service.CommentService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController {
  private final CommentService commentService;

  @PostMapping
  public ResponseEntity<CommentDto> registerComment(
      @RequestBody @Valid CommentRegisterRequest request
  ) {
    CommentDto comment = commentService.registerComment(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(comment);
  }

  @PatchMapping("/{commentId}")
  public ResponseEntity<CommentDto> updateComment(
      @PathVariable UUID commentId,
      @RequestHeader("Monew-Request-User-Id") UUID userId,
      @RequestBody CommentUpdateRequest request
  ){
    CommentDto comment = commentService.updateComment(commentId, userId, request);
    return ResponseEntity.ok(comment);
  }
}
