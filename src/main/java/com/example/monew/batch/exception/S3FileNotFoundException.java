package com.example.monew.batch.exception;

public class S3FileNotFoundException extends RuntimeException {
  public S3FileNotFoundException(String key) {
    super("S3 파일을 찾을 수 없습니다. key=" + key);
  }
}