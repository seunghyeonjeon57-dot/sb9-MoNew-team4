package com.example.monew.domain.interest.service;

import com.example.monew.domain.interest.dto.InterestCreateRequest;
import com.example.monew.domain.interest.dto.InterestResponse;
import com.example.monew.domain.interest.dto.InterestUpdateRequest;
import com.example.monew.domain.interest.entity.Interest;
import com.example.monew.domain.interest.exception.InterestNotFoundException;
import com.example.monew.domain.interest.exception.InvalidSortParameterException;
import com.example.monew.domain.interest.exception.SimilarInterestNameException;
import com.example.monew.domain.interest.repository.InterestRepository;
import com.example.monew.domain.interest.repository.SubscriptionRepository;
import com.example.monew.global.util.SimilarityUtils;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterestService {

  private static final double SIMILARITY_THRESHOLD = 0.8;
  private static final Set<String> ALLOWED_SORT_BY = Set.of("name", "subscriberCount");
  private static final Set<String> ALLOWED_DIRECTION = Set.of("asc", "desc");

  private final InterestRepository interestRepository;
  private final SubscriptionRepository subscriptionRepository;

  @Transactional
  public InterestResponse create(InterestCreateRequest request) {
    List<Interest> actives = interestRepository.findAllByDeletedAtIsNull();
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

  @Transactional(readOnly = true)
  public List<InterestResponse> getInterests(
      String keyword, String sortBy, String direction, UUID userId) {
    if (sortBy != null && !ALLOWED_SORT_BY.contains(sortBy)) {
      throw new InvalidSortParameterException(Map.of("sortBy", sortBy));
    }
    if (direction != null && !ALLOWED_DIRECTION.contains(direction)) {
      throw new InvalidSortParameterException(Map.of("direction", direction));
    }

    List<Interest> interests = interestRepository.findAllByDeletedAtIsNull();

    Comparator<Interest> comparator = switch (sortBy == null ? "name" : sortBy) {
      case "subscriberCount" -> Comparator.comparingLong(Interest::getSubscriberCount);
      default -> Comparator.comparing(Interest::getName);
    };
    if ("desc".equals(direction)) {
      comparator = comparator.reversed();
    }

    Set<UUID> subscribedIds = userId == null ? Set.of()
        : subscriptionRepository.findAllByUserId(userId).stream()
            .map(s -> s.getInterestId())
            .collect(Collectors.toSet());

    return interests.stream()
        .filter(i -> matchesKeyword(i, keyword))
        .sorted(comparator)
        .map(i -> InterestResponse.from(i, subscribedIds.contains(i.getId())))
        .toList();
  }

  private boolean matchesKeyword(Interest interest, String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return true;
    }
    if (interest.getName().contains(keyword)) {
      return true;
    }
    return interest.getKeywords().stream()
        .anyMatch(k -> k.getValue().contains(keyword));
  }

  @Transactional
  public void delete(UUID interestId) {
    Interest interest = interestRepository.findByIdAndDeletedAtIsNull(interestId)
        .orElseThrow(() -> new InterestNotFoundException(Map.of("interestId", interestId.toString())));
    interest.markDeleted();
    subscriptionRepository.deleteAllByInterestId(interestId);
  }

  @Transactional
  public InterestResponse updateKeywords(UUID interestId, InterestUpdateRequest request) {
    Interest interest = interestRepository.findByIdAndDeletedAtIsNull(interestId)
        .orElseThrow(() -> new InterestNotFoundException(Map.of("interestId", interestId.toString())));
    interest.replaceKeywords(request.keywords());
    return InterestResponse.from(interest, false);
  }
}
