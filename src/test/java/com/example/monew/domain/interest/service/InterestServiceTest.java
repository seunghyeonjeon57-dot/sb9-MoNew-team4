package com.example.monew.domain.interest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

  @InjectMocks
  private InterestService interestService;

  @Test
  @DisplayName("create: 신규 이름 + 키워드 → 저장 후 응답 반환")
  void createSuccess() {
    when(interestRepository.findAllByDeletedAtIsNull()).thenReturn(List.of());
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
    when(interestRepository.findByIdAndDeletedAtIsNull(interest.getId()))
        .thenReturn(Optional.of(interest));

    InterestResponse response = interestService.updateKeywords(
        interest.getId(), new InterestUpdateRequest(List.of("ML", "DL")));

    assertThat(response.keywords()).containsExactly("ML", "DL");
  }

  @Test
  @DisplayName("updateKeywords: 미존재 ID → InterestNotFoundException")
  void updateKeywordsNotFound() {
    UUID id = UUID.randomUUID();
    when(interestRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() ->
        interestService.updateKeywords(id, new InterestUpdateRequest(List.of("ML"))))
        .isInstanceOf(InterestNotFoundException.class);
  }

  @Test
  @DisplayName("getInterests: 활성 인터레스트 목록 + subscribedByMe 매핑 (bulk 쿼리)")
  void getInterestsList() {
    Interest a = new Interest("A", List.of("a"));
    Interest b = new Interest("B", List.of("b"));
    UUID userId = UUID.randomUUID();
    when(interestRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(a, b));
    when(subscriptionRepository.findInterestIdsByUserIdAndInterestIdIn(
        org.mockito.ArgumentMatchers.eq(userId),
        org.mockito.ArgumentMatchers.anyCollection()))
        .thenReturn(Set.of(a.getId()));

    CursorPageResponse<InterestResponse> page =
        interestService.getInterests(null, null, null, null, 20, userId);

    assertThat(page.content()).hasSize(2);
    assertThat(page.content().stream().filter(r -> r.name().equals("A")).findFirst().orElseThrow().subscribedByMe()).isTrue();
    assertThat(page.content().stream().filter(r -> r.name().equals("B")).findFirst().orElseThrow().subscribedByMe()).isFalse();
  }

  @Test
  @DisplayName("getInterests: 잘못된 sortBy → InvalidSortParameterException")
  void getInterestsInvalidSort() {
    assertThatThrownBy(() -> interestService.getInterests(null, "foo", "asc", null, 20, null))
        .isInstanceOf(InvalidSortParameterException.class);
  }

  @Test
  @DisplayName("getInterests: sortBy=name, direction=desc → 이름 내림차순")
  void getInterestsSortName() {
    Interest a = new Interest("A", List.of("a"));
    Interest b = new Interest("B", List.of("b"));
    when(interestRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(a, b));

    CursorPageResponse<InterestResponse> page =
        interestService.getInterests(null, "name", "desc", null, 20, null);

    assertThat(page.content()).extracting(InterestResponse::name).containsExactly("B", "A");
  }

  @Test
  @DisplayName("delete: 존재하는 ID → markDeleted + 구독 일괄 정리")
  void deleteSuccess() {
    Interest interest = new Interest("인공지능", List.of("AI"));
    when(interestRepository.findByIdAndDeletedAtIsNull(interest.getId()))
        .thenReturn(Optional.of(interest));

    interestService.delete(interest.getId());

    assertThat(interest.isDeleted()).isTrue();
    verify(subscriptionRepository).deleteAllByInterestId(interest.getId());
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
  @DisplayName("create: 80%+ 유사 이름 존재 → SimilarInterestNameException")
  void createSimilarRejected() {
    Interest existing = new Interest("인공지능", List.of("AI"));
    when(interestRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(existing));

    assertThatThrownBy(() ->
        interestService.create(new InterestCreateRequest("인공지능A", List.of("AI"))))
        .isInstanceOf(SimilarInterestNameException.class);
  }

  @Test
  @DisplayName("getInterests: keyword 지정 시 name 부분일치만 반환")
  void getInterestsKeywordNameMatch() {
    Interest ai = new Interest("인공지능", List.of("ML"));
    Interest bc = new Interest("블록체인", List.of("BTC"));
    when(interestRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(ai, bc));

    CursorPageResponse<InterestResponse> page =
        interestService.getInterests("인공", null, null, null, 20, null);

    assertThat(page.content()).extracting(InterestResponse::name).containsExactly("인공지능");
  }

  @Test
  @DisplayName("getInterests: keyword가 keywords 중 하나와 부분일치 시 포함")
  void getInterestsKeywordKeywordsMatch() {
    Interest ai = new Interest("인공지능", List.of("ML"));
    Interest bc = new Interest("블록체인", List.of("BTC"));
    when(interestRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(ai, bc));

    CursorPageResponse<InterestResponse> page =
        interestService.getInterests("BTC", null, null, null, 20, null);

    assertThat(page.content()).extracting(InterestResponse::name).containsExactly("블록체인");
  }

  @Test
  @DisplayName("getInterests: keyword 대소문자 무시 - 이름 매칭")
  void getInterestsKeywordCaseInsensitiveName() {
    Interest spring = new Interest("Spring Boot", List.of("Java"));
    when(interestRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(spring));

    CursorPageResponse<InterestResponse> page =
        interestService.getInterests("spring", null, null, null, 20, null);

    assertThat(page.content()).extracting(InterestResponse::name).containsExactly("Spring Boot");
  }

  @Test
  @DisplayName("getInterests: keyword 대소문자 무시 - keywords 매칭")
  void getInterestsKeywordCaseInsensitiveKeywords() {
    Interest spring = new Interest("Spring Boot", List.of("Java"));
    when(interestRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(spring));

    CursorPageResponse<InterestResponse> page = interestService.getInterests("JAVA", null, null, null, 20, null);

    assertThat(page.content()).extracting(InterestResponse::name).containsExactly("Spring Boot");
  }

  @Test
  @DisplayName("getInterests: size=1 → 1건만 반환 + hasNext=true")
  void getInterests_size1_hasNext() {
    Interest a = new Interest("A", List.of("a"));
    Interest b = new Interest("B", List.of("b"));
    when(interestRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(a, b));

    CursorPageResponse<InterestResponse> page =
        interestService.getInterests(null, "name", "asc", null, 1, null);

    assertThat(page.content()).hasSize(1);
    assertThat(page.hasNext()).isTrue();
    assertThat(page.nextCursor()).isNotNull();
  }

  @Test
  @DisplayName("getInterests: cursor로 다음 페이지 조회 → 나머지 항목 반환")
  void getInterests_withCursor_returnsRemainingItems() {
    Interest a = new Interest("A", List.of("a"));
    Interest b = new Interest("B", List.of("b"));
    when(interestRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(a, b));

    CursorPageResponse<InterestResponse> firstPage =
        interestService.getInterests(null, "name", "asc", null, 1, null);
    String cursor = firstPage.nextCursor();

    when(interestRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(a, b));
    CursorPageResponse<InterestResponse> secondPage =
        interestService.getInterests(null, "name", "asc", cursor, 1, null);

    assertThat(secondPage.content()).hasSize(1);
    assertThat(secondPage.content().get(0).name()).isEqualTo("B");
    assertThat(secondPage.hasNext()).isFalse();
  }

  @Test
  @DisplayName("getInterests: 전체 항목이 size 이하 → hasNext=false + nextCursor=null")
  void getInterests_lastPage_noNext() {
    Interest a = new Interest("A", List.of("a"));
    when(interestRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(a));

    CursorPageResponse<InterestResponse> page =
        interestService.getInterests(null, null, null, null, 20, null);

    assertThat(page.hasNext()).isFalse();
    assertThat(page.nextCursor()).isNull();
    assertThat(page.content()).hasSize(1);
  }
}
