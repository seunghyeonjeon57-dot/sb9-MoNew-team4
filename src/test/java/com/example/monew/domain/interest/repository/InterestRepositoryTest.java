package com.example.monew.domain.interest.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.monew.domain.interest.entity.Interest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class InterestRepositoryTest {

  @Autowired
  private InterestRepository interestRepository;

  @Test
  @DisplayName("findByNameAndIsDeletedFalse: 이름 중복 확인")
  void findByNameAndIsDeletedFalse() {
    Interest interest = new Interest("인공지능", List.of("AI"));
    interestRepository.save(interest);

    Optional<Interest> found = interestRepository.findByNameAndIsDeletedFalse("인공지능");

    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("인공지능");
  }

  @Test
  @DisplayName("findByIdAndIsDeletedFalse: 삭제되지 않은 인터레스트만 조회")
  void findByIdAndIsDeletedFalse() {
    Interest interest = new Interest("인공지능", List.of("AI"));
    interestRepository.save(interest);

    Optional<Interest> found = interestRepository.findByIdAndIsDeletedFalse(interest.getId());
    assertThat(found).isPresent();

    interest.markDeleted();
    interestRepository.save(interest);

    Optional<Interest> gone = interestRepository.findByIdAndIsDeletedFalse(interest.getId());
    assertThat(gone).isEmpty();
  }

  @Test
  @DisplayName("findAllByIsDeletedFalse: 삭제되지 않은 전체 리스트")
  void findAllByIsDeletedFalse() {
    interestRepository.save(new Interest("A1", List.of("A")));
    Interest deleted = new Interest("B1", List.of("B"));
    deleted.markDeleted();
    interestRepository.save(deleted);

    List<Interest> actives = interestRepository.findAllByIsDeletedFalse();

    assertThat(actives).extracting(Interest::getName).containsExactlyInAnyOrder("A1");
  }
}
