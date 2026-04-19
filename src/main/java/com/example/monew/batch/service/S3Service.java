package com.example.monew.batch.service;

import com.amazonaws.services.s3.AmazonS3;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class S3Service {

  private final S3Client s3Client; // AmazonS3 대신 S3Client 사용

  @Value("${cloud.aws.s3.bucket}")
  private String bucket;

  // 파일 업로드 (v2 방식)
  public void upload(String key, String content) {
    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .contentType("application/json")
        .build();

    s3Client.putObject(putObjectRequest, RequestBody.fromString(content, StandardCharsets.UTF_8));
  }

  // 파일 다운로드 (v2 방식)
  public String download(String key) {
    try {
      GetObjectRequest getObjectRequest = GetObjectRequest.builder()
          .bucket(bucket)
          .key(key)
          .build();

      return s3Client.getObjectAsBytes(getObjectRequest).asUtf8String();
    } catch (Exception e) {
      return null; // 파일이 없을 경우
    }
  }
}