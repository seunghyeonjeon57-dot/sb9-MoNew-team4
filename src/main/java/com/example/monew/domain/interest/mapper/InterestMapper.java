package com.example.monew.domain.interest.mapper;

import com.example.monew.domain.interest.dto.InterestResponse;
import com.example.monew.domain.interest.dto.SubscriptionDto;
import com.example.monew.domain.interest.entity.Interest;
import com.example.monew.domain.interest.entity.InterestSubscription;
import java.util.ArrayList;
import org.springframework.stereotype.Component;

@Component
public class InterestMapper {

  public InterestResponse toResponse(Interest interest, boolean subscribed) {
    return new InterestResponse(
        interest.getId(),
        interest.getName(),
        new ArrayList<>(interest.getKeywords()),
        interest.getSubscriberCount(),
        subscribed
    );
  }

  public SubscriptionDto toDto(InterestSubscription subscription) {
    return new SubscriptionDto(
        subscription.getId(),
        subscription.getInterestId(),
        subscription.getUserId(),
        subscription.getCreatedAt()
    );
  }
}
