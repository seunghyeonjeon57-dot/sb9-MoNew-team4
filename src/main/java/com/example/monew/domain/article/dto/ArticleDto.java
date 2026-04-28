package com.example.monew.domain.article.dto;

import java.time.LocalDateTime;
import java.util.List;
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
    Boolean viewedByMe,
    List<String> keywords
) {
}