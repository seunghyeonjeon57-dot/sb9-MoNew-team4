package com.example.monew.domain.comment.dto;

import com.example.monew.domain.activityManagement.dto.CommentActivityDto;
import java.time.LocalDateTime;
import java.util.List;

public record CursorPageResponseCommentDto (
    List<CommentActivityDto> content,
    String nextCursor,
    LocalDateTime nextAfter,
    Integer size,
    Long totalElements,
    Boolean hasNext
) { }
