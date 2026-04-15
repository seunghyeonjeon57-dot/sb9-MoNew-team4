package com.example.monew.domain.interest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.monew.domain.interest.dto.InterestCreateRequest;
import com.example.monew.domain.interest.dto.InterestResponse;
import com.example.monew.domain.interest.dto.InterestUpdateRequest;
import com.example.monew.domain.interest.entity.Interest;
import com.example.monew.domain.interest.exception.InterestNotFoundException;
import com.example.monew.domain.interest.exception.SimilarInterestNameException;
import com.example.monew.domain.interest.repository.InterestRepository;
import com.example.monew.domain.interest.repository.SubscriptionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InterestServiceTest {

  @Mock
  private InterestRepository interestRepository;

  @Mock
  private SubscriptionRepository subscriptionRepository;

  @InjectMocks
  private InterestService interestService;

  @Test
  @DisplayName("create: 신규 이름 + 키워드 → 저장 후 응답 반환")
  void createSuccess() {
    when(interestRepository.findAllByIsDeletedFalse()).thenReturn(List.of());
    when(interestRepository.save(any(Interest.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    InterestResponse response = interestService.create(
        new InterestCreateRequest("인공지능", List.of("AI", "ML")));

    assertThat(response.name()).isEqualTo("인공지능");
    assertThat(response.keywords()).containsExactly("AI", "ML");
  }

  @Test
  @DisplayName("updateKeywords: 존재하는 ID → 키워드 교체 후 응답")
  void updateKeywordsSuccess() {
    Interest interest = new Interest("인공지능", List.of("AI"));
    when(interestRepository.findByIdAndIsDeletedFalse(interest.getId()))
        .thenReturn(Optional.of(interest));

    InterestResponse response = interestService.updateKeywords(
        interest.getId(), new InterestUpdateRequest(List.of("ML", "DL")));

    assertThat(response.keywords()).containsExactly("ML", "DL");
  }

  @Test
  @DisplayName("updateKeywords: 미존재 ID → InterestNotFoundException")
  void updateKeywordsNotFound() {
    UUID id = UUID.randomUUID();
    when(interestRepository.findByIdAndIsDeletedFalse(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() ->
        interestService.updateKeywords(id, new InterestUpdateRequest(List.of("ML"))))
        .isInstanceOf(InterestNotFoundException.class);
  }

  @Test
  @DisplayName("delete: 존재하는 ID → markDeleted + 구독 일괄 정리")
  void deleteSuccess() {
    Interest interest = new Interest("인공지능", List.of("AI"));
    when(interestRepository.findByIdAndIsDeletedFalse(interest.getId()))
        .thenReturn(Optional.of(interest));

    interestService.delete(interest.getId());

    assertThat(interest.isDeleted()).isTrue();
    verify(subscriptionRepository).deleteAllByInterestId(interest.getId());
  }

  @Test
  @DisplayName("delete: 미존재 ID → InterestNotFoundException")
  void deleteNotFound() {
    UUID id = UUID.randomUUID();
    when(interestRepository.findByIdAndIsDeletedFalse(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> interestService.delete(id))
        .isInstanceOf(InterestNotFoundException.class);
  }

  @Test
  @DisplayName("create: 80%+ 유사 이름 존재 → SimilarInterestNameException")
  void createSimilarRejected() {
    Interest existing = new Interest("인공지능", List.of("AI"));
    when(interestRepository.findAllByIsDeletedFalse()).thenReturn(List.of(existing));

    assertThatThrownBy(() ->
        interestService.create(new InterestCreateRequest("인공지능A", List.of("AI"))))
        .isInstanceOf(SimilarInterestNameException.class);
  }
}
