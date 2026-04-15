package com.example.monew.domain.interest.dto;

import com.example.monew.domain.interest.entity.Subscription;
import java.util.UUID;

public record SubscriptionResponse(
    UUID id,
    UUID interestId,
    UUID userId
) {

  public static SubscriptionResponse from(Subscription sub) {
    return new SubscriptionResponse(sub.getId(), sub.getInterestId(), sub.getUserId());
  }
}
