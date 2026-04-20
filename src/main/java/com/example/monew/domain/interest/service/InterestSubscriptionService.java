package com.example.monew.domain.interest.service;

import com.example.monew.domain.interest.dto.SubscriptionResponse;
import com.example.monew.domain.interest.entity.Interest;
import com.example.monew.domain.interest.entity.Subscription;
import com.example.monew.domain.interest.exception.DuplicateSubscriptionException;
import com.example.monew.domain.interest.exception.InterestNotFoundException;
import com.example.monew.domain.interest.exception.SubscriberNotFoundException;
import com.example.monew.domain.interest.exception.SubscriptionNotFoundException;
import com.example.monew.domain.interest.repository.InterestRepository;
import com.example.monew.domain.interest.repository.SubscriptionRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
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

    Subscription saved;
    try {
      saved = subscriptionRepository.saveAndFlush(new Subscription(interestId, userId));
    } catch (DataIntegrityViolationException e) {
      throw translateIntegrityViolation(e, interestId, userId);
    }

    interestRepository.incrementSubscriberCount(interest.getId());
    Interest refreshed = interestRepository.findByIdAndDeletedAtIsNull(interestId).orElse(interest);
    return SubscriptionResponse.of(saved, refreshed);
  }

  @Transactional
  public void unsubscribe(UUID interestId, UUID userId) {
    Subscription sub = subscriptionRepository.findByInterestIdAndUserId(interestId, userId)
        .orElseThrow(() -> new SubscriptionNotFoundException(
            Map.of("interestId", interestId.toString(), "userId", userId.toString())));

    subscriptionRepository.delete(sub);
    interestRepository.decrementSubscriberCount(interestId);
  }

  private RuntimeException translateIntegrityViolation(
      DataIntegrityViolationException e, UUID interestId, UUID userId) {
    String constraintName = extractConstraintName(e);
    if ("fk_sub_user".equalsIgnoreCase(constraintName)) {
      return new SubscriberNotFoundException(Map.of("userId", userId.toString()));
    }
    if ("fk_sub_interest".equalsIgnoreCase(constraintName)) {
      return new InterestNotFoundException(Map.of("interestId", interestId.toString()));
    }
    return new DuplicateSubscriptionException(
        Map.of("interestId", interestId.toString(), "userId", userId.toString()));
  }

  private String extractConstraintName(DataIntegrityViolationException e) {
    if (e.getCause() instanceof ConstraintViolationException cve) {
      return cve.getConstraintName();
    }
    return null;
  }

}
