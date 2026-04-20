package com.example.monew.domain.article.batch.service;

import com.amazonaws.SdkClientException;
import com.example.monew.domain.article.batch.exception.S3DownloadException;
import com.example.monew.domain.article.batch.exception.S3FileNotFoundException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

  private final S3Client s3Client; // AmazonS3 대신 S3Client 사용

  @Value("${cloud.aws.s3.bucket}")
  private String bucket;

  // 파일 업로드 v2 방식
  public void upload(String key, String content) {
    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .contentType("application/json")
        .build();

    s3Client.putObject(putObjectRequest, RequestBody.fromString(content, StandardCharsets.UTF_8));
  }

  public String download(String key) {
    try {
      GetObjectRequest getObjectRequest = GetObjectRequest.builder()
          .bucket(bucket)
          .key(key)
          .build();

      return s3Client.getObjectAsBytes(getObjectRequest).asUtf8String();
    } catch (NoSuchKeyException e) {
      log.warn("S3 파일 없음 key={}", key);
      throw new S3FileNotFoundException(key);

    } catch (S3Exception e) {
      log.error("S3 서비스 에러 key={}, statusCode={}", key, e.statusCode(), e);
      throw new S3DownloadException(key, e);

    } catch (SdkClientException e) {
      log.error("네트워크 에러 key={}", key, e);
      throw new S3DownloadException(key, e);
    }
  }
}