package com.example.monew.domain.activity.dto;

import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.comment.entity.CommentEntity;
import com.example.monew.domain.comment.entity.CommentLikeEntity;
import com.example.monew.domain.user.entity.User;
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

  public static CommentLikeActivityDto of(CommentLikeEntity commentLike, CommentEntity comment, ArticleEntity article, User user) {
    return new CommentLikeActivityDto(
        commentLike.getId(),
        commentLike.getCreatedAt(),
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