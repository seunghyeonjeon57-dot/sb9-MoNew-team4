package com.example.monew.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import java.util.stream.Stream;

class ErrorCodeTest {

  static Stream<Arguments> codeStatusMatrix() {
    return Stream.of(
        Arguments.of(ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST),
        Arguments.of(ErrorCode.MALFORMED_REQUEST_BODY, HttpStatus.BAD_REQUEST),
        Arguments.of(ErrorCode.CONSTRAINT_VIOLATION, HttpStatus.BAD_REQUEST),
        Arguments.of(ErrorCode.MISSING_REQUEST_HEADER, HttpStatus.BAD_REQUEST),
        Arguments.of(ErrorCode.METHOD_NOT_ALLOWED_REQUEST, HttpStatus.METHOD_NOT_ALLOWED),
        Arguments.of(ErrorCode.UNSUPPORTED_MEDIA_TYPE_REQUEST, HttpStatus.UNSUPPORTED_MEDIA_TYPE),
        Arguments.of(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR),
        Arguments.of(ErrorCode.INTEREST_NOT_FOUND, HttpStatus.NOT_FOUND),
        Arguments.of(ErrorCode.SIMILAR_INTEREST_NAME, HttpStatus.CONFLICT),
        Arguments.of(ErrorCode.INTEREST_NAME_IMMUTABLE, HttpStatus.BAD_REQUEST),
        Arguments.of(ErrorCode.INVALID_SORT_PARAMETER, HttpStatus.BAD_REQUEST),
        Arguments.of(ErrorCode.DUPLICATE_SUBSCRIPTION, HttpStatus.CONFLICT),
        Arguments.of(ErrorCode.SUBSCRIPTION_NOT_FOUND, HttpStatus.NOT_FOUND),
        Arguments.of(ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND)
    );
  }

  @ParameterizedTest(name = "{0} → {1}")
  @MethodSource("codeStatusMatrix")
  void getStatusReturnsExpectedHttpStatus(ErrorCode code, HttpStatus expected) {
    assertThat(code.getStatus()).isEqualTo(expected);
  }

  @ParameterizedTest(name = "{0} 메시지는 비어있지 않다")
  @MethodSource("codeStatusMatrix")
  void getMessageIsNotBlank(ErrorCode code, HttpStatus ignored) {
    assertThat(code.getMessage()).isNotBlank();
  }
}
