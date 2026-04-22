package com.example.monew.domain.comment.controller;

import com.example.monew.domain.comment.dto.CommentDto;
import com.example.monew.domain.comment.dto.CommentRegisterRequest;
import com.example.monew.domain.comment.dto.CommentUpdateRequest;
import com.example.monew.domain.comment.dto.CursorPageResponseCommentDto;
import com.example.monew.domain.comment.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "댓글 관리", description = "댓글 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController {
  private final CommentService commentService;

  @Operation(summary = "댓글 등록", description = "새로운 댓글을 등록합니다.")
  @PostMapping
  public ResponseEntity<CommentDto> registerComment(
      @RequestBody @Valid CommentRegisterRequest request
  ) {
    CommentDto comment = commentService.registerComment(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(comment);
  }

  @Operation(summary = "댓글 수정", description = "댓글의 내용을 수정합니다.")
  @PatchMapping("/{commentId}")
  public ResponseEntity<CommentDto> updateComment(
      @PathVariable UUID commentId,
      @RequestHeader("Monew-Request-User-ID") UUID userId,
      @Valid @RequestBody CommentUpdateRequest request
  ){
    CommentDto comment = commentService.updateComment(commentId, userId, request);
    return ResponseEntity.ok(comment);
  }

  @Operation(summary = "댓글 논리 삭제", description = "댓글를 논리적으로 삭제합니다.")
  @DeleteMapping("/{commentId}")
  public ResponseEntity<Void> softDeleteComment(
      @PathVariable UUID commentId
  ){
    commentService.softDeleteComment(commentId);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "댓글 물리 삭제", description = "댓글를 물리적으로 삭제합니다.")
  @DeleteMapping("/{commentId}/hard")
  public ResponseEntity<Void> hardDeleteComment(
      @PathVariable UUID commentId
  ){
    commentService.hardDeleteComment(commentId);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "댓글 좋아요", description = "댓글 좋아요를 등록합니다.")
  @PostMapping("/{commentId}/comment-likes")
  public ResponseEntity<Void> addCommentLike(
      @PathVariable UUID commentId,
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {
    commentService.addLike(commentId, userId);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "댓글 좋아요 취소", description = "댓글 좋아요를 취소합니다.")
  @DeleteMapping("/{commentId}/comment-likes")
  public ResponseEntity<Void> removeCommentLike(
      @PathVariable UUID commentId,
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {
    commentService.removeLike(commentId, userId);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "댓글 목록 조회", description = "댓글 목록을 조회합니다.")
  @GetMapping
  public ResponseEntity<CursorPageResponseCommentDto> getArticleComments(
      @RequestParam UUID articleId,
      @RequestParam String orderBy,
      @RequestParam String direction,
      @RequestParam(required = true) UUID cursor,
      @RequestParam(required = false) LocalDateTime after,
      @RequestParam(required = false) Long cursorLikeCount,
      @RequestParam int limit,
      @RequestHeader(value = "Monew-Request-User-ID", required = false) UUID userId
  ) {
    CursorPageResponseCommentDto response = commentService.getArticleComments(
        articleId,
        userId,
        cursor,
        after,
        cursorLikeCount,
        orderBy,
        direction,
        limit
    );

    return ResponseEntity.ok(response);
  }
}
