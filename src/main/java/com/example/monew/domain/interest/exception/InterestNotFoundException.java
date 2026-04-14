package com.example.monew.domain.interest.exception;

import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;
import java.util.Map;
import java.util.UUID;

public class InterestNotFoundException extends MonewException {

  public InterestNotFoundException(UUID interestId) {
    super(ErrorCode.INTEREST_NOT_FOUND, Map.of("interestId", interestId));
  }
}
