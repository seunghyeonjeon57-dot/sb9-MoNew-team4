package com.example.monew.domain.interest.dto;

import java.util.List;

public record CursorSlice<T>(
    List<T> content,
    String nextCursor,
    boolean hasNext,
    long totalElements
) {
}
