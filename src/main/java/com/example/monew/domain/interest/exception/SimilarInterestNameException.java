package com.example.monew.domain.interest.exception;

import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;
import java.util.Map;

public class SimilarInterestNameException extends MonewException {

  public SimilarInterestNameException(String requested, String existing, double similarity) {
    super(ErrorCode.SIMILAR_INTEREST_NAME, Map.of(
        "requested", requested,
        "existing", existing,
        "similarity", similarity
    ));
  }
}
