package com.example.monew.domain.article.batch.dto;

import com.example.monew.domain.article.entity.ArticleEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleBackupDto {

  private UUID id;
  private String source;
  private String sourceUrl;
  private String title;
  private LocalDateTime publishDate;
  private String summary;
  private long commentCount;
  private long viewCount;

  private UUID interestId;
  private String interestName;
  private List<String> interestKeywords;

  // ArticleBackupDto.java 수정
  public static ArticleBackupDto from(ArticleEntity entity) {
    ArticleBackupDtoBuilder builder = ArticleBackupDto.builder()
        .id(entity.getId())
        .source(entity.getSource())
        .sourceUrl(entity.getSourceUrl())
        .title(entity.getTitle())
        .publishDate(entity.getPublishDate())
        .summary(entity.getSummary())
        .commentCount(entity.getCommentCount())
        .viewCount(entity.getViewCount());

    if (entity.getInterest() != null) {
      builder.interestId(entity.getInterest().getId())
          .interestName(entity.getInterest().getName())
          .interestKeywords(entity.getInterest().getKeywords().stream()
              .map(k -> k.getValue())
              .toList());
    } else {
      builder.interestKeywords(List.of());
    }

    return builder.build();
  }
}