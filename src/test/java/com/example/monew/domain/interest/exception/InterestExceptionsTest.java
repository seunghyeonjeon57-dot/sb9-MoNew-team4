package com.example.monew.domain.interest.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InterestExceptionsTest {

  @Test
  @DisplayName("InterestNotFoundException → INTEREST_NOT_FOUND + details.interestId")
  void interestNotFound() {
    InterestNotFoundException ex = new InterestNotFoundException(Map.of("interestId", "abc"));
    assertThat(ex).isInstanceOf(MonewException.class);
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INTEREST_NOT_FOUND);
    assertThat(ex.getDetails()).containsEntry("interestId", "abc");
  }

  @Test
  @DisplayName("SimilarInterestNameException → SIMILAR_INTEREST_NAME + details.existing")
  void similarInterestName() {
    SimilarInterestNameException ex = new SimilarInterestNameException(Map.of("existing", "AI"));
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SIMILAR_INTEREST_NAME);
    assertThat(ex.getDetails()).containsEntry("existing", "AI");
  }

  @Test
  @DisplayName("InterestNameImmutableException → INTEREST_NAME_IMMUTABLE")
  void nameImmutable() {
    InterestNameImmutableException ex = new InterestNameImmutableException();
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INTEREST_NAME_IMMUTABLE);
  }

  @Test
  @DisplayName("InvalidSortParameterException → INVALID_SORT_PARAMETER + details.sortBy")
  void invalidSort() {
    InvalidSortParameterException ex = new InvalidSortParameterException(Map.of("sortBy", "foo"));
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_SORT_PARAMETER);
    assertThat(ex.getDetails()).containsEntry("sortBy", "foo");
  }

  @Test
  @DisplayName("DuplicateSubscriptionException → DUPLICATE_SUBSCRIPTION")
  void duplicateSubscription() {
    DuplicateSubscriptionException ex = new DuplicateSubscriptionException(Map.of("userId", "u1"));
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_SUBSCRIPTION);
    assertThat(ex.getDetails()).containsEntry("userId", "u1");
  }

  @Test
  @DisplayName("SubscriptionNotFoundException → SUBSCRIPTION_NOT_FOUND")
  void subscriptionNotFound() {
    SubscriptionNotFoundException ex = new SubscriptionNotFoundException(Map.of("userId", "u1"));
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SUBSCRIPTION_NOT_FOUND);
    assertThat(ex.getDetails()).containsEntry("userId", "u1");
  }
}
