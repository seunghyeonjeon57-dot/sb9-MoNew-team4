package com.example.monew.domain.interest.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.monew.config.JpaAuditConfig;
import com.example.monew.domain.interest.dto.CursorSlice;
import com.example.monew.domain.interest.dto.InterestResponse;
import com.example.monew.domain.interest.entity.Interest;
import com.example.monew.domain.interest.entity.InterestSubscription;
import com.example.monew.domain.interest.mapper.InterestMapper;
import com.example.monew.domain.interest.repository.InterestRepository;
import com.example.monew.domain.interest.repository.InterestSubscriptionRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase
@Import({JpaAuditConfig.class, InterestMapper.class, InterestService.class})
@ActiveProfiles("test")
class InterestListIntegrationTest {

  @Autowired
  private InterestRepository interestRepository;

  @Autowired
  private InterestSubscriptionRepository subscriptionRepository;

  @Autowired
  private InterestService interestService;

  @Test
  @DisplayName("name/keyword 부분일치 검색이 동작한다 (대소문자 무시, keywords 조인)")
  void keywordContainsMatchesNameOrKeyword() {
    interestRepository.saveAndFlush(new Interest("인공지능", List.of("AI", "ML")));
    interestRepository.saveAndFlush(new Interest("스포츠", List.of("축구", "야구")));
    interestRepository.saveAndFlush(new Interest("요리", List.of("ai-cooking")));

    CursorSlice<InterestResponse> byName =
        interestService.getInterests("인공", "name", "asc", null, 20, null);
    CursorSlice<InterestResponse> byKeyword =
        interestService.getInterests("ai", "name", "asc", null, 20, null);

    assertThat(byName.content()).extracting(InterestResponse::name)
        .containsExactly("인공지능");
    assertThat(byKeyword.content()).extracting(InterestResponse::name)
        .containsExactlyInAnyOrder("인공지능", "요리");
  }

  @Test
  @DisplayName("subscriberCount desc 정렬 + 커서로 이어서 조회하면 후속 페이지가 정상 반환된다")
  void sortBySubscriberCountDescWithCursor() {
    Interest a = interestRepository.saveAndFlush(new Interest("A관심사", List.of("k")));
    Interest b = interestRepository.saveAndFlush(new Interest("B관심사", List.of("k")));
    Interest c = interestRepository.saveAndFlush(new Interest("C관심사", List.of("k")));
    interestRepository.incrementSubscriberCount(a.getId());
    interestRepository.incrementSubscriberCount(a.getId());
    interestRepository.incrementSubscriberCount(a.getId()); // a=3
    interestRepository.incrementSubscriberCount(b.getId());
    interestRepository.incrementSubscriberCount(b.getId()); // b=2
    interestRepository.incrementSubscriberCount(c.getId()); // c=1
    interestRepository.flush();

    CursorSlice<InterestResponse> first =
        interestService.getInterests(null, "subscriberCount", "desc", null, 2, null);
    assertThat(first.content()).extracting(InterestResponse::name)
        .containsExactly("A관심사", "B관심사");
    assertThat(first.hasNext()).isTrue();
    assertThat(first.nextCursor()).isNotNull();

    CursorSlice<InterestResponse> second =
        interestService.getInterests(
            null, "subscriberCount", "desc", first.nextCursor(), 2, null);
    assertThat(second.content()).extracting(InterestResponse::name)
        .containsExactly("C관심사");
    assertThat(second.hasNext()).isFalse();
  }

  @Test
  @DisplayName("userId가 주어지면 구독 중인 관심사만 subscribed=true 로 세팅된다")
  void subscribedFlagSetFromBulkQuery() {
    Interest subscribed = interestRepository.saveAndFlush(new Interest("구독중", List.of("k")));
    Interest notSubscribed = interestRepository.saveAndFlush(new Interest("미구독", List.of("k")));
    UUID userId = UUID.randomUUID();
    subscriptionRepository.saveAndFlush(new InterestSubscription(subscribed.getId(), userId));

    CursorSlice<InterestResponse> slice =
        interestService.getInterests(null, "name", "asc", null, 20, userId);

    assertThat(slice.content())
        .filteredOn(InterestResponse::subscribed, true)
        .extracting(InterestResponse::name)
        .containsExactly("구독중");
    assertThat(slice.content())
        .filteredOn(InterestResponse::subscribed, false)
        .extracting(InterestResponse::name)
        .containsExactly("미구독");
  }

  @Test
  @DisplayName("soft-deleted(isDeleted=true) 관심사는 목록에서 제외된다")
  void notDeletedFilterExcludesSoftDeleted() {
    Interest live = interestRepository.saveAndFlush(new Interest("살아있음", List.of("k")));
    Interest gone = interestRepository.saveAndFlush(new Interest("삭제됨", List.of("k")));
    gone.markDeleted();
    interestRepository.saveAndFlush(gone);

    CursorSlice<InterestResponse> slice =
        interestService.getInterests(null, "name", "asc", null, 20, null);

    assertThat(slice.content()).extracting(InterestResponse::name)
        .containsExactly("살아있음");
    assertThat(slice.content()).extracting(InterestResponse::id)
        .contains(live.getId())
        .doesNotContain(gone.getId());
  }
}
