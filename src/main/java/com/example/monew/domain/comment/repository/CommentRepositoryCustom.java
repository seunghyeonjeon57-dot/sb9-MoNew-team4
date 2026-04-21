package com.example.monew.domain.comment.repository;

import com.example.monew.domain.comment.dto.CommentActivityDto;
import com.example.monew.domain.comment.dto.CommentDto;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


public interface CommentRepositoryCustom {
  List<CommentDto> findCommentsByArticleWithCursor(
      UUID articleId,
      UUID currentUserId,
      UUID cursorId,
      LocalDateTime cursorCreatedAt,
      Long cursorLikeCount,
      String sort,
      int size
  );
  long softDeleteAllByUserId(UUID userId);
  long deleteAllByUserId(UUID userId);
}
