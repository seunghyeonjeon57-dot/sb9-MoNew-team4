package com.example.monew.domain.article.dto;

import com.example.monew.domain.article.entity.ArticleEntity;
import java.time.LocalDateTime;
import java.util.UUID;

public record ArticleDto(
    UUID id,
    String source,
    String sourceUrl,
    String title,
    LocalDateTime publishDate,
    String summary,
    Long commentCount,
    Long viewCount,
    Boolean viewedByMe
) {
  public static ArticleDto from(ArticleEntity entity, boolean viewedByMe) {
    return new ArticleDto(
        entity.getId(),
        entity.getSource(),
        entity.getSourceUrl(),
        entity.getTitle(),
        entity.getPublishDate(),
        entity.getSummary(),
        entity.getCommentCount(),
        entity.getViewCount(),
        viewedByMe
    );
  }
}