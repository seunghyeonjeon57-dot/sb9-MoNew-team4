package com.example.monew.domain.interest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.monew.domain.interest.dto.InterestResponse;
import com.example.monew.domain.interest.dto.InterestUpdateRequest;
import com.example.monew.domain.interest.entity.Interest;
import com.example.monew.domain.interest.exception.InterestNotFoundException;
import com.example.monew.domain.interest.mapper.InterestMapper;
import com.example.monew.domain.interest.repository.InterestRepository;
import com.example.monew.domain.interest.repository.InterestSubscriptionRepository;
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
class InterestUpdateDeleteServiceTest {

  @Mock
  private InterestRepository interestRepository;

  @Mock
  private InterestSubscriptionRepository subscriptionRepository;

  @Mock
  private InterestMapper interestMapper;

  @InjectMocks
  private InterestService interestService;

  @Test
  @DisplayName("updateKeywords는 키워드를 교체하고 이름은 그대로 유지한다")
  void updateKeywordsReplacesAndKeepsName() {
    Interest interest = new Interest("인공지능", List.of("AI"));
    given(interestRepository.findById(interest.getId())).willReturn(Optional.of(interest));
    given(interestMapper.toResponse(interest, false))
        .willReturn(new InterestResponse(interest.getId(), "인공지능",
            List.of("ML", "DL"), 0L, false));

    InterestResponse response = interestService.updateKeywords(
        interest.getId(), new InterestUpdateRequest(List.of("ML", "DL")));

    assertThat(response.keywords()).containsExactly("ML", "DL");
    assertThat(interest.getName()).isEqualTo("인공지능");
    assertThat(interest.getKeywords()).containsExactly("ML", "DL");
  }

  @Test
  @DisplayName("updateKeywords: 존재하지 않는 ID는 InterestNotFoundException")
  void updateKeywordsThrowsWhenNotFound() {
    UUID missing = UUID.randomUUID();
    given(interestRepository.findById(missing)).willReturn(Optional.empty());

    assertThatThrownBy(() -> interestService.updateKeywords(
        missing, new InterestUpdateRequest(List.of("X"))))
        .isInstanceOf(InterestNotFoundException.class);
  }

  @Test
  @DisplayName("delete는 soft delete하고 구독도 함께 제거한다")
  void deleteSoftDeletesAndRemovesSubscriptions() {
    Interest interest = new Interest("인공지능", List.of("AI"));
    given(interestRepository.findById(interest.getId())).willReturn(Optional.of(interest));

    interestService.delete(interest.getId());

    assertThat(interest.isDeleted()).isTrue();
    verify(subscriptionRepository).deleteAllByInterestId(interest.getId());
  }

  @Test
  @DisplayName("delete: 존재하지 않는 ID는 InterestNotFoundException")
  void deleteThrowsWhenNotFound() {
    UUID missing = UUID.randomUUID();
    given(interestRepository.findById(missing)).willReturn(Optional.empty());

    assertThatThrownBy(() -> interestService.delete(missing))
        .isInstanceOf(InterestNotFoundException.class);
  }
}
