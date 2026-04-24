package com.example.monew.domain.article.batch.exception;

public class S3DownloadException extends RuntimeException {
  public S3DownloadException(String key, Throwable cause) {
    super("S3 파일 다운로드 실패. key=" + key, cause);
  }
//  public S3DownloadException(String key, Throwable cause) {
//    super(ErrorCode.S3_DOWNLOAD_FAILED, Map.of(
//        "key", key,
//        "reason", cause.getMessage()
//    ));
//  }

}