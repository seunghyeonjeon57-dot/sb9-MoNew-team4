package com.example.monew.domain.interest.dto;

import com.example.monew.domain.interest.entity.Interest;
import com.example.monew.domain.interest.entity.InterestKeyword;
import java.util.List;
import java.util.UUID;

public record InterestResponse(
    UUID id,
    String name,
    List<String> keywords,
    long subscriberCount,
    boolean subscribedByMe
) {

  public static InterestResponse from(Interest interest, boolean subscribedByMe) {
    return new InterestResponse(
        interest.getId(),
        interest.getName(),
        interest.getKeywords().stream().map(InterestKeyword::getValue).toList(),
        interest.getSubscriberCount(),
        subscribedByMe
    );
  }
}
