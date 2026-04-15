package com.example.monew.domain.interest.service;

import com.example.monew.domain.interest.dto.CursorSlice;
import com.example.monew.domain.interest.dto.InterestCreateRequest;
import com.example.monew.domain.interest.dto.InterestResponse;
import com.example.monew.domain.interest.dto.InterestUpdateRequest;
import com.example.monew.domain.interest.entity.Interest;
import com.example.monew.domain.interest.exception.InterestNotFoundException;
import com.example.monew.domain.interest.exception.InvalidSortParameterException;
import com.example.monew.domain.interest.exception.SimilarInterestNameException;
import com.example.monew.domain.interest.mapper.InterestMapper;
import com.example.monew.domain.interest.repository.InterestCursor;
import com.example.monew.domain.interest.repository.InterestRepository;
import com.example.monew.domain.interest.repository.InterestSpecifications;
import com.example.monew.domain.interest.repository.InterestSubscriptionRepository;
import com.example.monew.global.util.SimilarityUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterestService {

  private static final Set<String> ALLOWED_SORTS =
      Set.of(InterestCursor.NAME, InterestCursor.SUBSCRIBER_COUNT);
  private static final Set<String> ALLOWED_DIRECTIONS = Set.of("asc", "desc");
  private static final int MAX_PAGE_SIZE = 100;
  private static final int DEFAULT_PAGE_SIZE = 20;

  private final InterestRepository interestRepository;
  private final InterestSubscriptionRepository subscriptionRepository;
  private final InterestMapper interestMapper;

  @Transactional
  public InterestResponse create(InterestCreateRequest request) {
    List<Interest> existing = interestRepository.findAllByIsDeletedFalse();
    for (Interest e : existing) {
      double sim = SimilarityUtils.similarity(e.getName(), request.name());
      if (sim >= SimilarityUtils.SIMILAR_THRESHOLD) {
        throw new SimilarInterestNameException(request.name(), e.getName(), sim);
      }
    }
    Interest saved = interestRepository.save(new Interest(request.name(), request.keywords()));
    return interestMapper.toResponse(saved, false);
  }

  @Transactional
  public InterestResponse updateKeywords(UUID interestId, InterestUpdateRequest request) {
    Interest interest = interestRepository.findById(interestId)
        .orElseThrow(() -> new InterestNotFoundException(interestId));
    interest.replaceKeywords(request.keywords());
    return interestMapper.toResponse(interest, false);
  }

  @Transactional
  public void delete(UUID interestId) {
    Interest interest = interestRepository.findById(interestId)
        .orElseThrow(() -> new InterestNotFoundException(interestId));
    interest.markDeleted();
    subscriptionRepository.deleteAllByInterestId(interestId);
  }

  public CursorSlice<InterestResponse> getInterests(
      String keyword, String sortBy, String direction,
      String cursor, int size, UUID userId) {

    if (sortBy != null && !ALLOWED_SORTS.contains(sortBy)) {
      throw new InvalidSortParameterException("sortBy", sortBy, ALLOWED_SORTS);
    }
    if (direction != null && !ALLOWED_DIRECTIONS.contains(direction.toLowerCase())) {
      throw new InvalidSortParameterException("direction", direction, ALLOWED_DIRECTIONS);
    }

    String field = InterestCursor.SUBSCRIBER_COUNT.equals(sortBy)
        ? InterestCursor.SUBSCRIBER_COUNT : InterestCursor.NAME;
    Sort.Direction dir = "desc".equalsIgnoreCase(direction)
        ? Sort.Direction.DESC : Sort.Direction.ASC;
    Sort sort = Sort.by(dir, field).and(Sort.by(Sort.Direction.ASC, "id"));

    Specification<Interest> baseSpec = Specification.allOf(
        InterestSpecifications.notDeleted(),
        InterestSpecifications.keywordContains(keyword));
    Specification<Interest> querySpec = baseSpec
        .and(InterestSpecifications.cursorAfter(field, direction, cursor));

    int pageSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
    PageRequest pageable = PageRequest.of(0, pageSize + 1, sort);
    List<Interest> rows = new ArrayList<>(
        interestRepository.findAll(querySpec, pageable).getContent());

    boolean hasNext = rows.size() > pageSize;
    if (hasNext) {
      rows = new ArrayList<>(rows.subList(0, pageSize));
    }

    long total = interestRepository.count(baseSpec);

    Set<UUID> subscribedIds = (userId == null || rows.isEmpty())
        ? Set.of()
        : new HashSet<>(subscriptionRepository.findInterestIdsByUserIdAndInterestIdIn(
            userId, rows.stream().map(Interest::getId).toList()));

    List<InterestResponse> content = rows.stream()
        .map(i -> interestMapper.toResponse(i, subscribedIds.contains(i.getId())))
        .toList();

    String nextCursor = (hasNext && !rows.isEmpty())
        ? InterestCursor.encode(rows.get(rows.size() - 1), field)
        : null;

    return new CursorSlice<>(content, nextCursor, hasNext, total);
  }
}
