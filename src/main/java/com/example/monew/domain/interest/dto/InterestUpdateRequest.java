package com.example.monew.domain.interest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record InterestUpdateRequest(
    @NotEmpty(message = "키워드는 최소 1개 이상이어야 합니다.")
    List<@NotBlank(message = "빈 키워드는 허용되지 않습니다.") String> keywords
) {
}
