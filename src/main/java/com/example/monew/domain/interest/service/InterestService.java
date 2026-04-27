package com.example.monew.domain.interest.service;

import com.example.monew.domain.activity.service.ActivityService;
import com.example.monew.domain.article.service.ArticleService;
import com.example.monew.domain.interest.dto.CursorPageResponse;
import com.example.monew.domain.interest.dto.InterestCreateRequest;
import com.example.monew.domain.interest.dto.InterestResponse;
import com.example.monew.domain.interest.dto.InterestUpdateRequest;
import com.example.monew.domain.interest.entity.Interest;
import com.example.monew.domain.interest.exception.InterestNameImmutableException;
import com.example.monew.domain.interest.exception.InterestNotFoundException;
import com.example.monew.domain.interest.exception.InvalidSortParameterException;
import com.example.monew.domain.interest.exception.SimilarInterestNameException;
import com.example.monew.domain.interest.repository.InterestRepository;
import com.example.monew.domain.interest.repository.InterestRepositoryCustom;
import com.example.monew.domain.interest.repository.SubscriptionRepository;
import com.example.monew.global.util.SimilarityUtils;
import java.time.LocalDateTime;
import java.util.Collection;
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
  private final ActivityService activityService;

  @Transactional
  public InterestResponse create(InterestCreateRequest request) {
    interestRepository.findByNameAndDeletedAtIsNull(request.name())
        .ifPresent(existing -> {
          throw new SimilarInterestNameException(
              Map.of("existing", existing.getName(), "similarity", 1.0));
        });

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

    UUID cursorId = parseCursorUuid(cursor);
    InterestRepositoryCustom.CursorPage page = interestRepository.findByCursor(
        keyword, orderBy, direction, cursorId, after, limit);

    Set<UUID> subscribedIds = subscribedIdsFor(userId, page.content());
    List<InterestResponse> content = page.content().stream()
        .map(i -> InterestResponse.from(i, subscribedIds.contains(i.getId())))
        .toList();

    Interest tail = page.content().isEmpty() ? null
        : page.content().get(page.content().size() - 1);
    String nextCursor = page.hasNext() && tail != null ? tail.getId().toString() : null;
    LocalDateTime nextAfter = page.hasNext() && tail != null ? tail.getCreatedAt() : null;

    return new CursorPageResponse<>(content, nextCursor, nextAfter, content.size(),
        page.totalElements(), page.hasNext());
  }

  private UUID parseCursorUuid(String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(cursor);
    } catch (IllegalArgumentException e) {
      throw new InvalidSortParameterException(Map.of("cursor", cursor));
    }
  }

  private void validateSortParams(String orderBy, String direction) {
    if (!ALLOWED_ORDER_BY.contains(orderBy)) {
      throw new InvalidSortParameterException(Map.of("orderBy", String.valueOf(orderBy)));
    }
    if (!ALLOWED_DIRECTION.contains(direction)) {
      throw new InvalidSortParameterException(Map.of("direction", String.valueOf(direction)));
    }
  }

  private Set<UUID> subscribedIdsFor(UUID userId, List<Interest> filtered) {
    if (userId == null || filtered.isEmpty()) {
      return Set.of();
    }
    Collection<UUID> ids = filtered.stream().map(Interest::getId).toList();
    return subscriptionRepository.findInterestIdsByUserIdAndInterestIdIn(userId, ids);
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
    if (request.name() != null) {
      throw new InterestNameImmutableException(
          Map.of("interestId", interestId.toString(), "rejectedName", request.name()));
    }
    Interest interest = interestRepository.findByIdAndDeletedAtIsNull(interestId)
        .orElseThrow(() -> new InterestNotFoundException(Map.of("interestId", interestId.toString())));
    interest.replaceKeywords(request.keywords());

    activityService.updateInterestKeywords(interestId, request.keywords());

    return InterestResponse.from(interest, false);
  }
}
