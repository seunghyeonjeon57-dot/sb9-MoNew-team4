package com.example.monew.domain.comment.dto;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CommentRegisterRequest(
    UUID articleId,
    UUID userId,
    @Size(min = 1, max = 500) String content
) {

}
