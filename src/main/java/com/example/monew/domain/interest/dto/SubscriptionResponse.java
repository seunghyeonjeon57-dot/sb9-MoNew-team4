package com.example.monew.domain.interest.dto;

import com.example.monew.domain.interest.entity.Interest;
import com.example.monew.domain.interest.entity.InterestKeyword;
import com.example.monew.domain.interest.entity.Subscription;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SubscriptionResponse(
    UUID id,
    UUID interestId,
    String interestName,
    List<String> interestKeywords,
    long interestSubscriberCount,
    LocalDateTime createdAt
) {

  public static SubscriptionResponse of(Subscription sub, Interest interest) {
    return new SubscriptionResponse(
        sub.getId(),
        interest.getId(),
        interest.getName(),
        interest.getKeywords().stream().map(InterestKeyword::getValue).toList(),
        interest.getSubscriberCount(),
        sub.getCreatedAt()
    );
  }
}
