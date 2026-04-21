package com.example.monew.domain.article.batch.exception;

public class RestoreFailedException extends RuntimeException {

  public RestoreFailedException(String message) {
    super(message);
  }

  public RestoreFailedException(String message, Throwable cause) {
    super(message, cause);
  }
}