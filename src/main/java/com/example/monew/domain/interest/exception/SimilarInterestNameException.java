package com.example.monew.domain.interest.exception;

import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;
import java.util.Map;

public class SimilarInterestNameException extends MonewException {
  public SimilarInterestNameException(Map<String, Object> details) {
    super(ErrorCode.SIMILAR_INTEREST_NAME, details);
  }
}
