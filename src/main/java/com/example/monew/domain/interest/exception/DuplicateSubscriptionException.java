package com.example.monew.domain.interest.exception;

import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;
import java.util.Map;

public class DuplicateSubscriptionException extends MonewException {
  public DuplicateSubscriptionException(Map<String, Object> details) {
    super(ErrorCode.DUPLICATE_SUBSCRIPTION, details);
  }
}
