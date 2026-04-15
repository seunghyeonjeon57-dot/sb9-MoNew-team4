package com.example.monew.domain.interest.exception;

import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;
import java.util.Map;

public class SubscriptionNotFoundException extends MonewException {
  public SubscriptionNotFoundException(Map<String, Object> details) {
    super(ErrorCode.SUBSCRIPTION_NOT_FOUND, details);
  }
}
