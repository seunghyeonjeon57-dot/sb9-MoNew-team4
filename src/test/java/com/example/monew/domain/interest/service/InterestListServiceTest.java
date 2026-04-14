package com.example.monew.domain.interest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.example.monew.domain.interest.dto.CursorSlice;
import com.example.monew.domain.interest.dto.InterestResponse;
import com.example.monew.domain.interest.entity.Interest;
import com.example.monew.domain.interest.mapper.InterestMapper;
import com.example.monew.domain.interest.repository.InterestRepository;
import com.example.monew.domain.interest.repository.InterestSubscriptionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class InterestListServiceTest {

  @Mock
  private InterestRepository interestRepository;

  @Mock
  private InterestSubscriptionRepository subscriptionRepository;

  @Mock
  private InterestMapper interestMapper;

  @InjectMocks
  private InterestService interestService;

  @Test
  @DisplayName("데이터가 없으면 빈 CursorSlice(hasNext=false, nextCursor=null)를 반환한다")
  void getInterests_returnsEmptySliceWhenNoData() {
    given(interestRepository.findAll(any(Specification.class), any(Pageable.class)))
        .willReturn(new PageImpl<>(List.of()));
    given(interestRepository.count(any(Specification.class))).willReturn(0L);

    CursorSlice<InterestResponse> slice =
        interestService.getInterests(null, "name", "asc", null, 20, null);

    assertThat(slice.content()).isEmpty();
    assertThat(slice.hasNext()).isFalse();
    assertThat(slice.nextCursor()).isNull();
    assertThat(slice.totalElements()).isZero();
  }

  @Test
  @DisplayName("size+1개 조회되면 hasNext=true, nextCursor가 마지막 content 항목 기준으로 세팅된다")
  void getInterests_buildsCursorSliceWithHasNextTrue() {
    Interest i1 = new Interest("관심사A", List.of("k"));
    Interest i2 = new Interest("관심사B", List.of("k"));
    Interest i3 = new Interest("관심사C", List.of("k"));
    given(interestRepository.findAll(any(Specification.class), any(Pageable.class)))
        .willReturn(new PageImpl<>(List.of(i1, i2, i3)));
    given(interestRepository.count(any(Specification.class))).willReturn(10L);
    given(interestMapper.toResponse(any(Interest.class), anyBoolean()))
        .willAnswer(inv -> {
          Interest i = inv.getArgument(0);
          boolean sub = inv.getArgument(1);
          return new InterestResponse(
              i.getId(), i.getName(), new ArrayList<>(i.getKeywords()), 0L, sub);
        });

    CursorSlice<InterestResponse> slice =
        interestService.getInterests(null, "name", "asc", null, 2, null);

    assertThat(slice.content()).hasSize(2);
    assertThat(slice.content()).extracting(InterestResponse::name)
        .containsExactly("관심사A", "관심사B");
    assertThat(slice.hasNext()).isTrue();
    assertThat(slice.nextCursor()).contains("관심사B");
    assertThat(slice.totalElements()).isEqualTo(10L);
  }

  @Test
  @DisplayName("userId가 주어지면 해당 유저가 구독 중인 관심사에 subscribed=true 세팅")
  void getInterests_setsSubscribedFlagWhenUserProvided() {
    Interest i1 = new Interest("관심사A", List.of("k"));
    Interest i2 = new Interest("관심사B", List.of("k"));
    UUID userId = UUID.randomUUID();
    given(interestRepository.findAll(any(Specification.class), any(Pageable.class)))
        .willReturn(new PageImpl<>(List.of(i1, i2)));
    given(interestRepository.count(any(Specification.class))).willReturn(2L);
    given(subscriptionRepository.findInterestIdsByUserIdAndInterestIdIn(
        eq(userId), any()))
        .willReturn(List.of(i1.getId()));
    given(interestMapper.toResponse(any(Interest.class), anyBoolean()))
        .willAnswer(inv -> {
          Interest i = inv.getArgument(0);
          boolean sub = inv.getArgument(1);
          return new InterestResponse(
              i.getId(), i.getName(), new ArrayList<>(i.getKeywords()), 0L, sub);
        });

    CursorSlice<InterestResponse> slice =
        interestService.getInterests(null, "name", "asc", null, 20, userId);

    assertThat(slice.content()).hasSize(2);
    assertThat(slice.content())
        .filteredOn(InterestResponse::subscribed, true)
        .extracting(InterestResponse::name)
        .containsExactly("관심사A");
  }

  @Test
  @DisplayName("userId=null이면 구독 조회 없이 모두 subscribed=false")
  void getInterests_nullUserIdReturnsAllFalseSubscribed() {
    Interest i1 = new Interest("관심사A", List.of("k"));
    given(interestRepository.findAll(any(Specification.class), any(Pageable.class)))
        .willReturn(new PageImpl<>(List.of(i1)));
    given(interestRepository.count(any(Specification.class))).willReturn(1L);
    given(interestMapper.toResponse(any(Interest.class), anyBoolean()))
        .willAnswer(inv -> {
          Interest i = inv.getArgument(0);
          boolean sub = inv.getArgument(1);
          return new InterestResponse(
              i.getId(), i.getName(), new ArrayList<>(i.getKeywords()), 0L, sub);
        });

    CursorSlice<InterestResponse> slice =
        interestService.getInterests(null, "name", "asc", null, 20, null);

    assertThat(slice.content()).hasSize(1);
    assertThat(slice.content().get(0).subscribed()).isFalse();
  }
}
