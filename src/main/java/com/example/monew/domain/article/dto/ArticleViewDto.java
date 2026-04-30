package com.example.monew.domain.article.dto;

import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.entity.ArticleViewEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ArticleViewDto {
  private UUID id;
  private UUID viewedBy;
  private LocalDateTime createdAt;

  private UUID articleId;
  private String source;
  private String sourceUrl;
  private String articleTitle;
  private LocalDateTime articlePublishedDate;
  private String articleSummary;

  private Long articleCommentCount;
  private Long articleViewCount;

  public static ArticleViewDto of(ArticleViewEntity articleView, ArticleEntity article) {
    return ArticleViewDto.builder()
        .id(articleView.getId())
        .viewedBy(articleView.getViewedBy().getId())
        .createdAt(articleView.getCreatedAt())
        .articleId(article.getId())
        .articleTitle(article.getTitle())
        .articlePublishedDate(article.getPublishDate())
        .articleSummary(article.getSummary())
        .articleCommentCount(article.getCommentCount())
        .articleViewCount(article.getViewCount())
        .build();
  }
}