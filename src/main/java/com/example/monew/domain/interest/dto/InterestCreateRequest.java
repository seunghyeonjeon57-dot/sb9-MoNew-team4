package com.example.monew.domain.interest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record InterestCreateRequest(
    @NotBlank String name,
    @NotEmpty List<String> keywords
) {}
