package com.example.monew.domain.interest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.monew.domain.activity.service.ActivityService;
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
import com.example.monew.domain.interest.repository.InterestRepositoryCustom.CursorPage;
import com.example.monew.domain.interest.repository.SubscriptionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

  @Mock
  private ActivityService activityService;

  @InjectMocks
  private InterestService interestService;

  @Test
  @DisplayName("create: 신규 이름 + 키워드 → 저장 후 응답 반환")
  void createSuccess() {
    when(interestRepository.findByNameAndDeletedAtIsNull(anyString())).thenReturn(Optional.empty());
    when(interestRepository.findAllByDeletedAtIsNull()).thenReturn(List.of());
    when(interestRepository.save(any(Interest.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    InterestResponse response = interestService.create(
        new InterestCreateRequest("인공지능", List.of("AI", "ML")));

    assertThat(response.name()).isEqualTo("인공지능");
    assertThat(response.keywords()).containsExactly("AI", "ML");
  }

  @Test
  @DisplayName("create: 정확 일치 이름 존재 → SimilarInterestNameException (similarity=1.0)")
  void createExactMatchRejected() {
    Interest existing = Interest.builder().name("인공지능").keywords(List.of("AI")).build();
    when(interestRepository.findByNameAndDeletedAtIsNull("인공지능"))
        .thenReturn(Optional.of(existing));

    assertThatThrownBy(() ->
        interestService.create(new InterestCreateRequest("인공지능", List.of("ML"))))
        .isInstanceOf(SimilarInterestNameException.class);
  }

  @Test
  @DisplayName("create: 80%+ 유사 이름 존재 → SimilarInterestNameException")
  void createSimilarRejected() {
    Interest existing = Interest.builder().name("인공지능").keywords(List.of("AI")).build();
    when(interestRepository.findByNameAndDeletedAtIsNull(anyString())).thenReturn(Optional.empty());
    when(interestRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(existing));

    assertThatThrownBy(() ->
        interestService.create(new InterestCreateRequest("인공지능A", List.of("AI"))))
        .isInstanceOf(SimilarInterestNameException.class);
  }

  @Test
  @DisplayName("updateKeywords: 비구독자 userId → 키워드 교체 후 subscribedByMe=false 응답")
  void updateKeywordsSuccess() {
    Interest interest = Interest.builder().name("인공지능").keywords(List.of("AI")).build();
    when(interestRepository.findByIdAndDeletedAtIsNull(interest.getId()))
        .thenReturn(Optional.of(interest));

    InterestResponse response = interestService.updateKeywords(
        interest.getId(), new InterestUpdateRequest(null, List.of("ML", "DL")));

    assertThat(response.keywords()).containsExactly("ML", "DL");
    assertThat(response.subscribedByMe()).isFalse();
  }

  @Test
  @DisplayName("updateKeywords: 구독자 userId → 응답 subscribedByMe=true")
  void updateKeywords_subscriber_returnsSubscribedByMeTrue() {
    Interest interest = Interest.builder().name("인공지능").keywords(List.of("AI")).build();
    when(interestRepository.findByIdAndDeletedAtIsNull(interest.getId()))
        .thenReturn(Optional.of(interest));

    InterestResponse response = interestService.updateKeywords(
        interest.getId(), new InterestUpdateRequest(null, List.of("ML", "DL")));

    assertThat(response.subscribedByMe()).isFalse();
  }

  @Test
  @DisplayName("updateKeywords: userId=null → subscribedByMe=false (헤더 없는 호출 방어)")
  void updateKeywords_nullUserId_returnsSubscribedByMeFalse() {
    Interest interest = Interest.builder().name("인공지능").keywords(List.of("AI")).build();
    when(interestRepository.findByIdAndDeletedAtIsNull(interest.getId()))
        .thenReturn(Optional.of(interest));

    InterestResponse response = interestService.updateKeywords(
        interest.getId(), new InterestUpdateRequest(null, List.of("ML", "DL")));

    assertThat(response.subscribedByMe()).isFalse();
  }

  @Test
  @DisplayName("updateKeywords: name 포함 → InterestNameImmutableException")
  void updateKeywordsNameImmutable() {
    UUID id = UUID.randomUUID();
    assertThatThrownBy(() ->
        interestService.updateKeywords(
            id, new InterestUpdateRequest("바뀐이름", List.of("ML"))))
        .isInstanceOf(InterestNameImmutableException.class);
  }

  @Test
  @DisplayName("updateKeywords: 미존재 ID → InterestNotFoundException")
  void updateKeywordsNotFound() {
    UUID id = UUID.randomUUID();
    when(interestRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() ->
        interestService.updateKeywords(
            id, new InterestUpdateRequest(null, List.of("ML"))))
        .isInstanceOf(InterestNotFoundException.class);
  }

  @Test
  @DisplayName("getInterests: findByCursor 반환을 InterestResponse 목록 + subscribedByMe 로 매핑")
  void getInterestsList() {
    Interest a = Interest.builder().name("A").keywords(List.of("a")).build();
    Interest b = Interest.builder().name("B").keywords(List.of("b")).build();
    UUID userId = UUID.randomUUID();
    when(interestRepository.findByCursor(any(), eq("name"), eq("ASC"), any(), any(), anyInt()))
        .thenReturn(new CursorPage(List.of(a, b), 2L, false));
    when(subscriptionRepository.findInterestIdsByUserIdAndInterestIdIn(
        eq(userId), anyCollection()))
        .thenReturn(Set.of(a.getId()));

    CursorPageResponse<InterestResponse> page =
        interestService.getInterests(null, "name", "ASC", null, null, 20, userId);

    assertThat(page.content()).hasSize(2);
    assertThat(page.content().stream().filter(r -> r.name().equals("A")).findFirst().orElseThrow()
        .subscribedByMe()).isTrue();
    assertThat(page.content().stream().filter(r -> r.name().equals("B")).findFirst().orElseThrow()
        .subscribedByMe()).isFalse();
    assertThat(page.totalElements()).isEqualTo(2L);
    assertThat(page.hasNext()).isFalse();
  }

  @Test
  @DisplayName("getInterests: 잘못된 orderBy → InvalidSortParameterException (repository 호출 전 차단)")
  void getInterestsInvalidSort() {
    assertThatThrownBy(() ->
        interestService.getInterests(null, "foo", "ASC", null, null, 20, null))
        .isInstanceOf(InvalidSortParameterException.class);
  }

  @Test
  @DisplayName("getInterests: 잘못된 direction(소문자) → InvalidSortParameterException")
  void getInterestsInvalidDirection() {
    assertThatThrownBy(() ->
        interestService.getInterests(null, "name", "asc", null, null, 20, null))
        .isInstanceOf(InvalidSortParameterException.class);
  }

  @Test
  @DisplayName("getInterests: hasNext=true → nextCursor/nextAfter 가 페이지 마지막 원소 값으로 세팅")
  void getInterests_hasNext_fillsCursor() {
    Interest a = Interest.builder().name("A").keywords(List.of("a")).build();
    when(interestRepository.findByCursor(any(), anyString(), anyString(), any(), any(), anyInt()))
        .thenReturn(new CursorPage(List.of(a), 5L, true));

    CursorPageResponse<InterestResponse> page =
        interestService.getInterests(null, "name", "ASC", null, null, 1, null);

    assertThat(page.hasNext()).isTrue();
    assertThat(page.nextCursor()).isEqualTo(a.getId().toString());
    assertThat(page.totalElements()).isEqualTo(5L);
  }

  @Test
  @DisplayName("getInterests: hasNext=false → nextCursor/nextAfter=null + size == content.size()")
  void getInterests_lastPage_noNext() {
    Interest a = Interest.builder().name("A").keywords(List.of("a")).build();
    when(interestRepository.findByCursor(any(), anyString(), anyString(), any(), any(), anyInt()))
        .thenReturn(new CursorPage(List.of(a), 1L, false));

    CursorPageResponse<InterestResponse> page =
        interestService.getInterests(null, "name", "ASC", null, null, 20, null);

    assertThat(page.hasNext()).isFalse();
    assertThat(page.nextCursor()).isNull();
    assertThat(page.nextAfter()).isNull();
    assertThat(page.content()).hasSize(1);
    assertThat(page.size()).isEqualTo(page.content().size());
  }

  @Test
  @DisplayName("getInterests: cursor 가 유효한 UUID 면 그대로 findByCursor 에 전달 (after=null)")
  void getInterests_withCursor_passesUuidThrough() {
    UUID cursor = UUID.randomUUID();
    when(interestRepository.findByCursor(
        any(), anyString(), anyString(), eq(cursor), isNull(), anyInt()))
        .thenReturn(new CursorPage(List.of(), 0L, false));

    interestService.getInterests(null, "name", "ASC", cursor.toString(), null, 10, null);

    verify(interestRepository).findByCursor(
        any(), eq("name"), eq("ASC"), eq(cursor), isNull(), eq(10));
  }

  @Test
  @DisplayName("getInterests: after 파라미터가 findByCursor 에 그대로 전달")
  void getInterests_passesAfterToRepository() {
    UUID cursor = UUID.randomUUID();
    LocalDateTime after = LocalDateTime.of(2026, 4, 20, 12, 0);
    when(interestRepository.findByCursor(
        any(), anyString(), anyString(), eq(cursor), eq(after), anyInt()))
        .thenReturn(new CursorPage(List.of(), 0L, false));

    interestService.getInterests(null, "name", "ASC", cursor.toString(), after, 10, null);

    verify(interestRepository).findByCursor(
        any(), eq("name"), eq("ASC"), eq(cursor), eq(after), eq(10));
  }

  @Test
  @DisplayName("getInterests: cursor 가 잘못된 문자열이면 InvalidSortParameterException (400)")
  void getInterests_invalidCursor_throwsInvalidSortParameter() {
    assertThatThrownBy(() ->
        interestService.getInterests(null, "name", "ASC", "not-a-uuid", null, 10, null))
        .isInstanceOf(InvalidSortParameterException.class);
  }

  @Test
  @DisplayName("delete: 존재하는 ID → 구독 정리 후 물리 삭제")
  void deleteSuccess() {
    Interest interest = Interest.builder().name("인공지능").keywords(List.of("AI")).build();
    when(interestRepository.findByIdAndDeletedAtIsNull(interest.getId()))
        .thenReturn(Optional.of(interest));

    interestService.delete(interest.getId());

    verify(subscriptionRepository).deleteAllByInterestId(interest.getId());
    verify(interestRepository).delete(interest);
  }

  @Test
  @DisplayName("delete: 미존재 ID → InterestNotFoundException")
  void deleteNotFound() {
    UUID id = UUID.randomUUID();
    when(interestRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> interestService.delete(id))
        .isInstanceOf(InterestNotFoundException.class);
  }

  @Test
  @DisplayName("[MON-146] getInterests: cursor 가 공백 문자열이면 null 로 정규화되어 첫 페이지 조회")
  void getInterests_blankCursor_normalizedToNull() {
    when(interestRepository.findByCursor(any(), anyString(), anyString(), isNull(), isNull(), anyInt()))
        .thenReturn(new CursorPage(List.of(), 0L, false));

    interestService.getInterests(null, "name", "ASC", "   ", null, 10, null);

    verify(interestRepository).findByCursor(
        any(), eq("name"), eq("ASC"), isNull(), isNull(), eq(10));
  }

  @Test
  @DisplayName("[MON-146] getInterests: userId 는 있지만 검색 결과가 0건이면 subscriptionRepository 조회를 건너뛴다")
  void getInterests_userIdPresent_butEmptyResult_skipsSubscriptionLookup() {
    UUID userId = UUID.randomUUID();
    when(interestRepository.findByCursor(any(), anyString(), anyString(), any(), any(), anyInt()))
        .thenReturn(new CursorPage(List.of(), 0L, false));

    CursorPageResponse<InterestResponse> page =
        interestService.getInterests("없는키워드", "name", "ASC", null, null, 10, userId);

    assertThat(page.content()).isEmpty();
    assertThat(page.hasNext()).isFalse();
    assertThat(page.nextCursor()).isNull();
    assertThat(page.nextAfter()).isNull();
    org.mockito.Mockito.verify(subscriptionRepository,
        org.mockito.Mockito.never())
        .findInterestIdsByUserIdAndInterestIdIn(any(), anyCollection());
  }
}


