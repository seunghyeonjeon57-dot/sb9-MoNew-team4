package com.example.monew.domain.interest.repository;

import com.example.monew.domain.interest.entity.Interest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface InterestRepositoryCustom {

  CursorPage findByCursor(
      String keyword,
      String orderBy,
      String direction,
      UUID cursorId,
      LocalDateTime after,
      int limit);

  record CursorPage(List<Interest> content, long totalElements, boolean hasNext) {}
}
