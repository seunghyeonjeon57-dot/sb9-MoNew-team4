package com.example.monew.domain.article.batch.exception;

import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;
import java.util.Map;

public class S3FileNotFoundException extends MonewException {
  public S3FileNotFoundException(String key) {
    super(ErrorCode.S3_FILE_NOT_FOUND, Map.of("key", key));
  }
}