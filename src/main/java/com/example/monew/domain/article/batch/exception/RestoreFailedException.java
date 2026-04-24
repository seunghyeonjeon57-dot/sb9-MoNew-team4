package com.example.monew.domain.article.batch.exception;

public class RestoreFailedException extends RuntimeException {

  public RestoreFailedException(String message) {
    super(message);
  }

  public RestoreFailedException(String message, Throwable cause) {
    super(message, cause);
  }
//  public RestoreFailedException(String message) {
//    super(ErrorCode.BATCH_RESTORE_FAILED, Map.of("message", message));
//  }
//
//  public RestoreFailedException(String message, Throwable cause) {
//    super(ErrorCode.BATCH_RESTORE_FAILED, Map.of(
//        "message", message,
//        "cause", cause.getMessage()
//    ));
//  }


}