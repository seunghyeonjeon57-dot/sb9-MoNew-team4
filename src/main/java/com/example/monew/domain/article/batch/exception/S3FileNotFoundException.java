package com.example.monew.domain.article.batch.exception;

public class S3FileNotFoundException extends RuntimeException {
  public S3FileNotFoundException(String key) {
    super("S3 파일을 찾을 수 없습니다. key=" + key);
  }
//  public S3FileNotFoundException(String key) {
//    // MonewException(ErrorCode, DetailsMap) 호출
//    super(ErrorCode.S3_FILE_NOT_FOUND, Map.of("key", key));
//  }


}