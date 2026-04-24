package com.example.monew.domain.comment.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CursorPageResponseCommentDto (
    List<CommentDto> content,
    String nextCursor,
    LocalDateTime nextAfter,
    Integer size,
    Long totalElements,
    Boolean hasNext
) { }
