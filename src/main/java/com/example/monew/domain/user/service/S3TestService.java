package com.example.monew.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3TestService {

  private final S3Client s3Client;

  @Value("${cloud.aws.s3.bucket}")
  private String bucket;

  public void uploadTestFile() {
    try {
      log.info("S3 업로드 테스트 시작... 버킷명: {}", bucket);

      PutObjectRequest putObjectRequest = PutObjectRequest.builder()
          .bucket(bucket)
          .key("test-connection.txt") // S3에 저장될 파일명
          .contentType("text/plain")
          .build();

      s3Client.putObject(putObjectRequest, RequestBody.fromString("S3 연결 테스트 성공! - 승현"));

      log.info("✅ S3 업로드 성공! AWS 콘솔에서 'test-connection.txt'를 확인하세요.");
    } catch (Exception e) {
      log.error("❌ S3 업로드 실패! 에러 메시지: {}", e.getMessage());
      e.printStackTrace();
    }
  }
}