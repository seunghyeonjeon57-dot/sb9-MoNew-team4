package com.example.monew.domain.interest.service;

import com.example.monew.domain.interest.dto.SubscriptionResponse;
import com.example.monew.domain.interest.entity.Interest;
import com.example.monew.domain.interest.entity.Subscription;
import com.example.monew.domain.interest.exception.DuplicateSubscriptionException;
import com.example.monew.domain.interest.exception.InterestNotFoundException;
import com.example.monew.domain.interest.exception.SubscriptionNotFoundException;
import com.example.monew.domain.interest.repository.InterestRepository;
import com.example.monew.domain.interest.repository.SubscriptionRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterestSubscriptionService {

  private final InterestRepository interestRepository;
  private final SubscriptionRepository subscriptionRepository;

  @Transactional
  public SubscriptionResponse subscribe(UUID interestId, UUID userId) {
    Interest interest = interestRepository.findByIdAndDeletedAtIsNull(interestId)
        .orElseThrow(() -> new InterestNotFoundException(Map.of("interestId", interestId.toString())));

    if (subscriptionRepository.existsByInterestIdAndUserId(interestId, userId)) {
      throw new DuplicateSubscriptionException(
          Map.of("interestId", interestId.toString(), "userId", userId.toString()));
    }

    Subscription saved = subscriptionRepository.save(new Subscription(interestId, userId));
    interest.incrementSubscriberCount();
    return SubscriptionResponse.from(saved);
  }

  @Transactional
  public void unsubscribe(UUID interestId, UUID userId) {
    Subscription sub = subscriptionRepository.findByInterestIdAndUserId(interestId, userId)
        .orElseThrow(() -> new SubscriptionNotFoundException(
            Map.of("interestId", interestId.toString(), "userId", userId.toString())));

    subscriptionRepository.delete(sub);
    interestRepository.findByIdAndDeletedAtIsNull(interestId)
        .ifPresent(Interest::decrementSubscriberCount);
  }

  @Transactional
  public void deleteAllSubscriptionsByUserId(UUID userId) {
    subscriptionRepository.deleteAllByUserId(userId);
  }
}
