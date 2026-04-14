package com.example.monew.domain.interest.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SubscriptionDto(
    UUID id,
    UUID interestId,
    UUID userId,
    LocalDateTime createdAt
) {
}
