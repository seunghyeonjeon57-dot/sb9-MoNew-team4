package com.example.monew.domain.article.dto;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ArticleSearchCondition {
  private final String keyword;
  private final UUID interestId;
  private final List<String> sourceIn;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private final LocalDateTime publishDateFrom;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private final LocalDateTime publishDateTo;

  @Builder.Default
  private final String orderBy = "publishedAt";

  @Builder.Default
  private final String direction = "DESC";

  private final UUID cursor;

  @Builder.Default
  private final int size = 10;
}