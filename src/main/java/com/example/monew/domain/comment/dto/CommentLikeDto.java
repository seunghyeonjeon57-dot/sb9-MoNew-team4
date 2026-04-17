package com.example.monew.domain.comment.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentLikeDto(
    UUID id,
    UUID likedBy,
    LocalDateTime createdAt,
    UUID commentId,
    UUID articleId,
    UUID commentUserId,
    String commentUserNickname,
    String commentContent,
    Long commentLikeCount,
    LocalDateTime commentCreatedAt
) {
}