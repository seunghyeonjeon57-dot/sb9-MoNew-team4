package com.example.monew.domain.activityManagement.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CommentActivityDto(
    UUID id,
    UUID articleId,
    String articleTitle,
    UUID userId,
    String userNickname,
    String content,
    Long likeCount,
    LocalDateTime createdAt
) {
}