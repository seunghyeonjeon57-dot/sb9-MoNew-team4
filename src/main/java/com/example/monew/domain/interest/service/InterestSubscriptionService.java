package com.example.monew.domain.interest.service;

import com.example.monew.domain.interest.dto.SubscriptionDto;
import com.example.monew.domain.interest.entity.InterestSubscription;
import com.example.monew.domain.interest.exception.DuplicateSubscriptionException;
import com.example.monew.domain.interest.exception.InterestNotFoundException;
import com.example.monew.domain.interest.exception.SubscriptionNotFoundException;
import com.example.monew.domain.interest.mapper.InterestMapper;
import com.example.monew.domain.interest.repository.InterestRepository;
import com.example.monew.domain.interest.repository.InterestSubscriptionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
    if (!interestRepository.existsById(interestId)) {
      throw new InterestNotFoundException(interestId);
    }
    if (subscriptionRepository.existsByInterestIdAndUserId(interestId, userId)) {
      throw new DuplicateSubscriptionException(interestId, userId);
    }
    InterestSubscription saved;
    try {
      saved = subscriptionRepository.saveAndFlush(
          new InterestSubscription(interestId, userId));
    } catch (DataIntegrityViolationException e) {
      throw new DuplicateSubscriptionException(interestId, userId);
    }
    interestRepository.incrementSubscriberCount(interestId);
    return interestMapper.toDto(saved);
  }

  @Transactional
  public void unsubscribe(UUID interestId, UUID userId) {
    if (!interestRepository.existsById(interestId)) {
      throw new InterestNotFoundException(interestId);
    }
    long deleted = subscriptionRepository.deleteByInterestIdAndUserId(interestId, userId);
    if (deleted == 0L) {
      throw new SubscriptionNotFoundException(interestId, userId);
    }
    interestRepository.decrementSubscriberCount(interestId);
  }
}
