package com.example.monew.domain.interest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record InterestCreateRequest(
    @NotBlank(message = "관심사 이름은 필수입니다.")
    @Size(max = 100, message = "관심사 이름은 100자 이하여야 합니다.")
    String name,

    @NotEmpty(message = "키워드는 최소 1개 이상이어야 합니다.")
    List<@NotBlank(message = "빈 키워드는 허용되지 않습니다.") String> keywords
) {
}
