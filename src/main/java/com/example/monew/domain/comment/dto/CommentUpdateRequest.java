package com.example.monew.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentUpdateRequest(
    @NotBlank(message = "댓글 내용을 입력해주세요.")
    @Size(min = 1, max = 500, message = "댓글은 1자 이상 500자 이하로 작성 가능합니다.")
    String content
) {
}