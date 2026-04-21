package com.example.monew.domain.comment.controller;

import com.example.monew.domain.comment.dto.CommentDto;
import com.example.monew.domain.comment.dto.CommentRegisterRequest;
import com.example.monew.domain.comment.dto.CommentUpdateRequest;
import com.example.monew.domain.comment.dto.CursorPageResponseCommentDto;
import com.example.monew.domain.comment.service.CommentService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
      @RequestHeader("Monew-Request-User-ID") UUID userId,
      @RequestBody CommentUpdateRequest request
  ){
    CommentDto comment = commentService.updateComment(commentId, userId, request);
    return ResponseEntity.ok(comment);
  }

  @DeleteMapping("/{commentId}")
  public ResponseEntity<Void> softDeleteComment(
      @PathVariable UUID commentId
  ){
    commentService.softDeleteComment(commentId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{commentId}/hard")
  public ResponseEntity<Void> hardDeleteComment(
      @PathVariable UUID commentId
  ){
    commentService.hardDeleteComment(commentId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{commentId}/comment-likes")
  public ResponseEntity<Void> addCommentLike(
      @PathVariable UUID commentId,
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {
    commentService.addLike(commentId, userId);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{commentId}/comment-likes")
  public ResponseEntity<Void> removeCommentLike(
      @PathVariable UUID commentId,
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {
    commentService.removeLike(commentId, userId);
    return ResponseEntity.ok().build();
  }

  @GetMapping
  public ResponseEntity<CursorPageResponseCommentDto> getArticleComments(
      @RequestParam UUID articleId,
      @RequestParam String orderBy,
      @RequestParam String direction,
      @RequestParam(required = false) UUID cursor,
      @RequestParam(required = false) LocalDateTime after,
      @RequestParam(required = false) Long cursorLikeCount,
      @RequestParam int limit,
      @RequestHeader(value = "Monew-Request-User-ID", required = false) UUID userId
  ) {
    CursorPageResponseCommentDto request = commentService.getArticleComments(
        articleId,
        userId,
        cursor,
        after,
        cursorLikeCount,
        orderBy,
        limit
    );

    return ResponseEntity.ok(request);
  }
}
