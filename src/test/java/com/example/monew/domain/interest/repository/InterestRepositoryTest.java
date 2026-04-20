package com.example.monew.domain.interest.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.monew.config.QueryDslTestConfig;
import com.example.monew.domain.interest.entity.Interest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(QueryDslTestConfig.class)
class InterestRepositoryTest {

  @Autowired
  private InterestRepository interestRepository;

  @Autowired
  private TestEntityManager em;

  @Test
  @DisplayName("findByNameAndDeletedAtIsNull: 이름 중복 확인")
  void findByNameAndDeletedAtIsNull() {
    Interest interest = Interest.builder().name("인공지능").keywords(List.of("AI")).build();
    interestRepository.save(interest);

    Optional<Interest> found = interestRepository.findByNameAndDeletedAtIsNull("인공지능");

    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("인공지능");
  }

  @Test
  @DisplayName("findByIdAndDeletedAtIsNull: 삭제되지 않은 인터레스트만 조회")
  void findByIdAndDeletedAtIsNull() {
    Interest interest = Interest.builder().name("인공지능").keywords(List.of("AI")).build();
    interestRepository.save(interest);

    Optional<Interest> found = interestRepository.findByIdAndDeletedAtIsNull(interest.getId());
    assertThat(found).isPresent();

    interest.markDeleted();
    interestRepository.save(interest);

    Optional<Interest> gone = interestRepository.findByIdAndDeletedAtIsNull(interest.getId());
    assertThat(gone).isEmpty();
  }

  @Test
  @DisplayName("findAllByDeletedAtIsNull: 삭제되지 않은 전체 리스트")
  void findAllByDeletedAtIsNull() {
    interestRepository.save(Interest.builder().name("A1").keywords(List.of("A")).build());
    Interest deleted = Interest.builder().name("B1").keywords(List.of("B")).build();
    deleted.markDeleted();
    interestRepository.save(deleted);

    List<Interest> actives = interestRepository.findAllByDeletedAtIsNull();

    assertThat(actives).extracting(Interest::getName).containsExactlyInAnyOrder("A1");
  }

  @Test
  @DisplayName("decrementSubscriberCountAll: 주어진 ID 집합의 subscriberCount를 한 방 UPDATE로 1씩 감소")
  void decrementSubscriberCountAll_bulk() {
    Interest a = interestRepository.save(
        Interest.builder().name("관심사A").keywords(List.of("a")).build());
    Interest b = interestRepository.save(
        Interest.builder().name("관심사B").keywords(List.of("b")).build());

    interestRepository.incrementSubscriberCount(a.getId());
    interestRepository.incrementSubscriberCount(a.getId());
    interestRepository.incrementSubscriberCount(b.getId());
    em.flush();
    em.clear();

    int updated = interestRepository.decrementSubscriberCountAll(List.of(a.getId(), b.getId()));

    assertThat(updated).isEqualTo(2);

    Interest refreshedA = interestRepository.findById(a.getId()).orElseThrow();
    Interest refreshedB = interestRepository.findById(b.getId()).orElseThrow();
    assertThat(refreshedA.getSubscriberCount()).isEqualTo(1L);
    assertThat(refreshedB.getSubscriberCount()).isZero();
  }

  @Test
  @DisplayName("decrementSubscriberCountAll: subscriberCount가 0인 관심사는 음수로 내려가지 않는다")
  void decrementSubscriberCountAll_guardAgainstNegative() {
    Interest a = interestRepository.save(
        Interest.builder().name("관심사A").keywords(List.of("a")).build());
    em.flush();
    em.clear();

    int updated = interestRepository.decrementSubscriberCountAll(List.of(a.getId()));

    assertThat(updated).isZero();
    Interest refreshed = interestRepository.findById(a.getId()).orElseThrow();
    assertThat(refreshed.getSubscriberCount()).isZero();
  }
}
