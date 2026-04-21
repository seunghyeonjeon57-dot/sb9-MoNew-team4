package com.example.monew.domain.article.dto;

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
}