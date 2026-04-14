package com.example.monew.domain.interest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.monew.domain.interest.dto.InterestCreateRequest;
import com.example.monew.domain.interest.dto.InterestResponse;
import com.example.monew.domain.interest.entity.Interest;
import com.example.monew.domain.interest.exception.SimilarInterestNameException;
import com.example.monew.domain.interest.mapper.InterestMapper;
import com.example.monew.domain.interest.repository.InterestRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InterestServiceTest {

  @Mock
  private InterestRepository interestRepository;

  @Mock
  private InterestMapper interestMapper;

  @InjectMocks
  private InterestService interestService;

  @Test
  @DisplayName("유사 이름(80% 이상)이 존재하면 SimilarInterestNameException을 던진다")
  void createThrowsWhenSimilar() {
    Interest existing = new Interest("인공지능", List.of("AI"));
    given(interestRepository.findAllByIsDeletedFalse()).willReturn(List.of(existing));

    assertThatThrownBy(() ->
        interestService.create(new InterestCreateRequest("인공지능!", List.of("AI")))
    ).isInstanceOf(SimilarInterestNameException.class);
  }

  @Test
  @DisplayName("유사 이름이 없으면 저장 후 InterestResponse를 반환한다")
  void createSavesWhenNotSimilar() {
    given(interestRepository.findAllByIsDeletedFalse()).willReturn(List.of(
        new Interest("완전히다른이름", List.of("X"))
    ));
    ArgumentCaptor<Interest> captor = ArgumentCaptor.forClass(Interest.class);
    Interest saved = new Interest("인공지능", List.of("AI"));
    given(interestRepository.save(captor.capture())).willReturn(saved);
    given(interestMapper.toResponse(saved, false))
        .willReturn(new InterestResponse(saved.getId(), "인공지능", List.of("AI"), 0L, false));

    InterestResponse response = interestService.create(
        new InterestCreateRequest("인공지능", List.of("AI")));

    assertThat(response.name()).isEqualTo("인공지능");
    assertThat(captor.getValue().getName()).isEqualTo("인공지능");
    verify(interestRepository).save(captor.getValue());
  }
}
