package com.example.monew.domain.article.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ArticleSearchCondition {

  @Schema(description = "검색어")
  private String keyword;

  @Schema(description = "관심사 ID")
  private UUID interestId;

  @Schema(description = "출처(포함)", allowableValues = {"NAVER", "HANKYUNG", "CHOSUN", "YONHAP"})
  private List<String> sourceIn;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  @Schema(description = "발행일 시작범위")
  private LocalDateTime publishDateFrom;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  @Schema(description = "발행일 종료범위")
  private LocalDateTime publishDateTo;

  @Schema(description = "정렬 기준 필드", allowableValues = {"publishDate", "commentCount", "viewCount"})
  @Builder.Default
  private String orderBy = "publishDate";

  @Schema(description = "정렬 방향", allowableValues = {"ASC", "DESC"})
  @Builder.Default
  private String direction = "DESC";

  @Schema(description = "커서 값(ID 등)")
  private String cursor;

  @Schema(description = "보조 커서/정렬 기준 시간")
  private LocalDateTime after;

  @Schema(description = "페이지 크기", example = "10")
  @Builder.Default
  private int limit = 50;

  public int getSize() { return this.limit; }

  public UUID getCursorId() {
    try {
      return (cursor != null) ? UUID.fromString(cursor) : null;
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public static class ArticleSearchConditionBuilder {
    private String keyword;
    private UUID interestId;
    private List<String> sourceIn;
    private LocalDateTime publishDateFrom;
    private LocalDateTime publishDateTo;
    private String orderBy = "publishDate";
    private String direction = "DESC";
    private String cursor;
    private LocalDateTime after;
    private int limit = 50;

    public ArticleSearchConditionBuilder size(int size) {
      this.limit = (size > 0) ? size : 50;
      return this;
    }

    public ArticleSearchConditionBuilder cursor(Object cursor) {
      if (cursor instanceof UUID) {
        this.cursor = cursor.toString();
      } else if (cursor instanceof String) {
        this.cursor = (String) cursor;
      }
      return this;
    }

    public ArticleSearchConditionBuilder after(LocalDateTime after) {
      this.after = after;
      return this;
    }

    public ArticleSearchCondition build() {
      return new ArticleSearchCondition(
          keyword, interestId, sourceIn, publishDateFrom, publishDateTo,
          orderBy, direction, cursor, after, limit
      );
    }
  }
}