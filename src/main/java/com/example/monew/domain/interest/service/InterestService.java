package com.example.monew.domain.interest.service;

import com.example.monew.domain.interest.dto.InterestCreateRequest;
import com.example.monew.domain.interest.dto.InterestResponse;
import com.example.monew.domain.interest.entity.Interest;
import com.example.monew.domain.interest.exception.SimilarInterestNameException;
import com.example.monew.domain.interest.mapper.InterestMapper;
import com.example.monew.domain.interest.repository.InterestRepository;
import com.example.monew.global.util.SimilarityUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterestService {

  private final InterestRepository interestRepository;
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
}
