package com.example.monew.domain.article.batch.exception;

import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;
import java.util.Map;

public class S3DownloadException extends MonewException {
  public S3DownloadException(String key, Throwable cause) {
    super(ErrorCode.S3_DOWNLOAD_FAILED, Map.of(
        "key", key,
        "reason", cause.getMessage()
    ));
  }
}