package com.example.monew.domain.interest.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record InterestUpdateRequest(
    String name,
    @NotEmpty @Size(min = 1, max = 10) List<String> keywords
) {}
