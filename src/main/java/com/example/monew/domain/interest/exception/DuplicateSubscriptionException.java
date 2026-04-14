package com.example.monew.domain.interest.exception;

import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;
import java.util.Map;
import java.util.UUID;

public class DuplicateSubscriptionException extends MonewException {

  public DuplicateSubscriptionException(UUID interestId, UUID userId) {
    super(ErrorCode.DUPLICATE_SUBSCRIPTION, Map.of(
        "interestId", interestId,
        "userId", userId
    ));
  }
}
