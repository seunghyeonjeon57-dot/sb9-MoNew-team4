package com.example.monew.domain.interest.service;

import com.example.monew.domain.interest.dto.SubscriptionDto;
import com.example.monew.domain.interest.entity.Interest;
import com.example.monew.domain.interest.entity.InterestSubscription;
import com.example.monew.domain.interest.exception.DuplicateSubscriptionException;
import com.example.monew.domain.interest.exception.InterestNotFoundException;
import com.example.monew.domain.interest.exception.SubscriptionNotFoundException;
import com.example.monew.domain.interest.mapper.InterestMapper;
import com.example.monew.domain.interest.repository.InterestRepository;
import com.example.monew.domain.interest.repository.InterestSubscriptionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterestSubscriptionService {

  private final InterestRepository interestRepository;
  private final InterestSubscriptionRepository subscriptionRepository;
  private final InterestMapper interestMapper;

  @Transactional
  public SubscriptionDto subscribe(UUID interestId, UUID userId) {
    Interest interest = interestRepository.findById(interestId)
        .orElseThrow(() -> new InterestNotFoundException(interestId));
    if (subscriptionRepository.existsByInterestIdAndUserId(interestId, userId)) {
      throw new DuplicateSubscriptionException(interestId, userId);
    }
    InterestSubscription saved = subscriptionRepository.save(
        new InterestSubscription(interestId, userId));
    interest.increaseSubscriberCount();
    return interestMapper.toDto(saved);
  }

  @Transactional
  public void unsubscribe(UUID interestId, UUID userId) {
    Interest interest = interestRepository.findById(interestId)
        .orElseThrow(() -> new InterestNotFoundException(interestId));
    long deleted = subscriptionRepository.deleteByInterestIdAndUserId(interestId, userId);
    if (deleted == 0L) {
      throw new SubscriptionNotFoundException(interestId, userId);
    }
    interest.decreaseSubscriberCount();
  }
}
