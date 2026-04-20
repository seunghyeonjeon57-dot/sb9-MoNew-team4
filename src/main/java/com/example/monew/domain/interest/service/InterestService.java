package com.example.monew.domain.interest.service;

import com.example.monew.domain.interest.dto.CursorPageResponse;
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
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterestService {

  private static final double SIMILARITY_THRESHOLD = 0.8;
  private static final Set<String> ALLOWED_ORDER_BY = Set.of("name", "subscriberCount");
  private static final Set<String> ALLOWED_DIRECTION = Set.of("ASC", "DESC");

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
  public CursorPageResponse<InterestResponse> getInterests(
      String keyword, String orderBy, String direction, String cursor,
      LocalDateTime after, int limit, UUID userId) {
    validateSortParams(orderBy, direction);

    List<Interest> filtered = interestRepository.findAllByDeletedAtIsNull().stream()
        .filter(i -> matchesKeyword(i, keyword))
        .sorted(buildComparator(orderBy, direction))
        .toList();

    long totalElements = filtered.size();
    int startIndex = resolveCursorOffset(filtered, cursor, after);

    List<Interest> sliced = filtered.stream().skip(startIndex).limit((long) limit + 1).toList();
    boolean hasNext = sliced.size() > limit;
    List<Interest> page = hasNext ? sliced.subList(0, limit) : sliced;

    Set<UUID> subscribedIds = subscribedIdsFor(userId, page);
    List<InterestResponse> content = page.stream()
        .map(i -> InterestResponse.from(i, subscribedIds.contains(i.getId())))
        .toList();

    String nextCursor = hasNext ? page.get(page.size() - 1).getId().toString() : null;
    LocalDateTime nextAfter = hasNext ? page.get(page.size() - 1).getCreatedAt() : null;
    return new CursorPageResponse<>(content, nextCursor, nextAfter, limit, totalElements, hasNext);
  }

  private void validateSortParams(String orderBy, String direction) {
    if (!ALLOWED_ORDER_BY.contains(orderBy)) {
      throw new InvalidSortParameterException(Map.of("orderBy", String.valueOf(orderBy)));
    }
    if (!ALLOWED_DIRECTION.contains(direction)) {
      throw new InvalidSortParameterException(Map.of("direction", String.valueOf(direction)));
    }
  }

  private Comparator<Interest> buildComparator(String orderBy, String direction) {
    Comparator<Interest> comparator = "subscriberCount".equals(orderBy)
        ? Comparator.comparingLong(Interest::getSubscriberCount)
        : Comparator.comparing(Interest::getName);
    return "DESC".equals(direction) ? comparator.reversed() : comparator;
  }

  private int resolveCursorOffset(List<Interest> sorted, String cursor, LocalDateTime after) {
    if (cursor == null || cursor.isBlank()) {
      return 0;
    }
    UUID cursorId;
    try {
      cursorId = UUID.fromString(cursor);
    } catch (IllegalArgumentException e) {
      return 0;
    }
    for (int i = 0; i < sorted.size(); i++) {
      Interest item = sorted.get(i);
      if (!item.getId().equals(cursorId)) {
        continue;
      }
      if (after != null && item.getCreatedAt() != null
          && !item.getCreatedAt().equals(after)) {
        continue;
      }
      return i + 1;
    }
    return 0;
  }

  private Set<UUID> subscribedIdsFor(UUID userId, List<Interest> filtered) {
    if (userId == null || filtered.isEmpty()) {
      return Set.of();
    }
    Collection<UUID> ids = filtered.stream().map(Interest::getId).toList();
    return subscriptionRepository.findInterestIdsByUserIdAndInterestIdIn(userId, ids);
  }

  private boolean matchesKeyword(Interest interest, String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return true;
    }
    String needle = keyword.toLowerCase();
    if (interest.getName().toLowerCase().contains(needle)) {
      return true;
    }
    return interest.getKeywords().stream()
        .anyMatch(k -> k.getValue().toLowerCase().contains(needle));
  }

  @Transactional
  public void delete(UUID interestId) {
    Interest interest = interestRepository.findByIdAndDeletedAtIsNull(interestId)
        .orElseThrow(() -> new InterestNotFoundException(Map.of("interestId", interestId.toString())));
    subscriptionRepository.deleteAllByInterestId(interestId);
    interestRepository.delete(interest);
  }

  @Transactional
  public InterestResponse updateKeywords(UUID interestId, InterestUpdateRequest request) {
    Interest interest = interestRepository.findByIdAndDeletedAtIsNull(interestId)
        .orElseThrow(() -> new InterestNotFoundException(Map.of("interestId", interestId.toString())));
    interest.replaceKeywords(request.keywords());
    return InterestResponse.from(interest, false);
  }
}
