package com.example.monew.domain.activity.dto;

import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.comment.entity.CommentEntity;
import com.example.monew.domain.user.entity.User;
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

  public static CommentActivityDto of(CommentEntity comment, ArticleEntity article, User user) {
    return new CommentActivityDto(
        comment.getId(),
        article.getId(),
        article.getTitle(),
        user.getId(),
        user.getNickname(),
        comment.getContent(),
        comment.getLikeCount(),
        comment.getCreatedAt()
    );
  }
}