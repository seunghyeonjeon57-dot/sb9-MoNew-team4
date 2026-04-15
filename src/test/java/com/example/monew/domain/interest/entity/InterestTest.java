package com.example.monew.domain.interest.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InterestTest {

  @Test
  @DisplayName("name + keywords로 생성: 키워드 리스트가 보존되고, isDeleted=false, subscriberCount=0")
  void createWithNameAndKeywords() {
    Interest interest = new Interest("인공지능", List.of("AI", "ML"));

    assertThat(interest.getName()).isEqualTo("인공지능");
    assertThat(interest.getKeywords()).extracting(InterestKeyword::getValue)
        .containsExactly("AI", "ML");
    assertThat(interest.isDeleted()).isFalse();
    assertThat(interest.getSubscriberCount()).isZero();
  }

  @Test
  @DisplayName("replaceKeywords: 기존 키워드를 비우고 새 리스트로 교체")
  void replaceKeywordsClearsAndAppends() {
    Interest interest = new Interest("인공지능", List.of("AI"));

    interest.replaceKeywords(List.of("ML", "DL"));

    assertThat(interest.getKeywords()).extracting(InterestKeyword::getValue)
        .containsExactly("ML", "DL");
  }

  @Test
  @DisplayName("markDeleted: isDeleted=true 전환")
  void markDeletedFlipsFlag() {
    Interest interest = new Interest("인공지능", List.of("AI"));

    interest.markDeleted();

    assertThat(interest.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("subscriberCount 증가/감소 (음수 금지)")
  void subscriberCountIncrementsAndDecrementsButNotBelowZero() {
    Interest interest = new Interest("인공지능", List.of("AI"));

    interest.incrementSubscriberCount();
    interest.incrementSubscriberCount();
    assertThat(interest.getSubscriberCount()).isEqualTo(2);

    interest.decrementSubscriberCount();
    assertThat(interest.getSubscriberCount()).isEqualTo(1);

    interest.decrementSubscriberCount();
    interest.decrementSubscriberCount();
    assertThat(interest.getSubscriberCount()).isZero();
  }

  @Test
  @DisplayName("name이 blank이면 IllegalArgumentException")
  void blankNameRejected() {
    assertThatThrownBy(() -> new Interest(" ", List.of("AI")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("keywords가 비어 있으면 IllegalArgumentException")
  void emptyKeywordsRejected() {
    assertThatThrownBy(() -> new Interest("인공지능", List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
