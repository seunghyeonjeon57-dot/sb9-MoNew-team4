package com.example.monew.domain.activity.dto;

import com.example.monew.domain.interest.dto.SubscriptionResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.example.monew.domain.article.dto.ArticleViewDto;
import lombok.Builder;


@Builder
public record UserActivityDto(
    UUID id,
    String email,
    String nickname,
    LocalDateTime createdAt,
    List<SubscriptionResponse> subscribedInterests,
    List<CommentActivityDto> comments,
    List<CommentLikeActivityDto> commentLikes,
    List<ArticleViewDto> articleViews
) {
}