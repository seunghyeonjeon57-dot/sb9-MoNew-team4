package com.example.monew.domain.interest.service;

import com.example.monew.domain.interest.dto.InterestCreateRequest;
import com.example.monew.domain.interest.dto.InterestResponse;
import com.example.monew.domain.interest.dto.InterestUpdateRequest;
import com.example.monew.domain.interest.entity.Interest;
import com.example.monew.domain.interest.exception.InterestNotFoundException;
import com.example.monew.domain.interest.exception.SimilarInterestNameException;
import com.example.monew.domain.interest.repository.InterestRepository;
import com.example.monew.domain.interest.repository.SubscriptionRepository;
import com.example.monew.global.util.SimilarityUtils;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterestService {

  private static final double SIMILARITY_THRESHOLD = 0.8;

  private final InterestRepository interestRepository;
  private final SubscriptionRepository subscriptionRepository;

  @Transactional
  public InterestResponse create(InterestCreateRequest request) {
    List<Interest> actives = interestRepository.findAllByIsDeletedFalse();
    for (Interest existing : actives) {
      double similarity = SimilarityUtils.similarity(existing.getName(), request.name());
      if (similarity >= SIMILARITY_THRESHOLD) {
        throw new SimilarInterestNameException(
            Map.of("existing", existing.getName(), "similarity", similarity));
      }
    }
    Interest saved = interestRepository.save(new Interest(request.name(), request.keywords()));
    return InterestResponse.from(saved, false);
  }

  @Transactional
  public void delete(UUID interestId) {
    Interest interest = interestRepository.findByIdAndIsDeletedFalse(interestId)
        .orElseThrow(() -> new InterestNotFoundException(Map.of("interestId", interestId.toString())));
    interest.markDeleted();
    subscriptionRepository.deleteAllByInterestId(interestId);
  }

  @Transactional
  public InterestResponse updateKeywords(UUID interestId, InterestUpdateRequest request) {
    Interest interest = interestRepository.findByIdAndIsDeletedFalse(interestId)
        .orElseThrow(() -> new InterestNotFoundException(Map.of("interestId", interestId.toString())));
    interest.replaceKeywords(request.keywords());
    return InterestResponse.from(interest, false);
  }
}
