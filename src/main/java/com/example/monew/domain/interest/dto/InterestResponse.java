package com.example.monew.domain.interest.dto;

import java.util.List;
import java.util.UUID;

public record InterestResponse(
    UUID id,
    String name,
    List<String> keywords,
    long subscriberCount,
    boolean subscribed
) {
}
