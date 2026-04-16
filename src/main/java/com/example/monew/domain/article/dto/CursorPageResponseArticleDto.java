package com.example.monew.domain.article.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CursorPageResponseArticleDto(
    List<ArticleDto> content,
    String nextCursor,
    LocalDateTime nextAfter,
    Integer size,
    Long totalElements,
    Boolean hasNext
) {
}