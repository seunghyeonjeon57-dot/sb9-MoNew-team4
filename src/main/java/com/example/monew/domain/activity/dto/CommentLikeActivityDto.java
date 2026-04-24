package com.example.monew.domain.activityManagement.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CommentLikeActivityDto(
    UUID id,
    LocalDateTime createdAt,
    UUID commentId,
    UUID articleId,
    String articleTitle,
    UUID commentUserId,
    String commentUserNickname,
    String commentContent,
    Long commentLikeCount,
    LocalDateTime commentCreatedAt
) {
}