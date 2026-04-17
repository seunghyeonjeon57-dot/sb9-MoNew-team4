package com.example.monew.domain.interest.dto;

import java.util.List;

public record CursorPageResponse<T>(
    List<T> content,
    String nextCursor,
    int size,
    long totalElements,
    boolean hasNext
) {
}
