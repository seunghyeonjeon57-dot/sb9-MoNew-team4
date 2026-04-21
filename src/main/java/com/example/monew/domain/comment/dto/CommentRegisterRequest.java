package com.example.monew.domain.comment.dto;

import com.example.monew.domain.comment.entity.CommentEntity;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CommentRegisterRequest(
    UUID articleId,
    UUID userId,
    @Size(min = 1, max = 500)
    String content
) {
  public CommentEntity toEntity() {
    return new CommentEntity(
        this.articleId,
        this.userId,
        this.content
    );
  }
}
