package com.example.monew.global.infra.s3.log;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Path;
import java.time.LocalDate;

@Slf4j
@Service
public class LogS3Service {

  private final S3Client s3Client;

  @Value("${AWS_LOG_BUCKET_NAME}")
  private String bucketName;

  // @Qualifier를 통해 logS3Client 빈을 주입받음
  public LogS3Service(@Qualifier("logS3Client") S3Client s3Client) {
    this.s3Client = s3Client;
  }

  public boolean uploadLogFile(Path filePath, LocalDate targetDate) {
    String fileName = filePath.getFileName().toString();
    String s3Key = String.format("logs/%s/%s", targetDate, fileName);

    try {
      PutObjectRequest putObjectRequest = PutObjectRequest.builder()
          .bucket(bucketName)
          .key(s3Key)
          .contentType("text/plain")
          .build();

      s3Client.putObject(putObjectRequest, RequestBody.fromFile(filePath));
      log.info(">>> [Log S3 Upload Success] Key: {}", s3Key);
      return true;
    } catch (Exception e) {
      log.error("<<< [Log S3 Upload Failed] Error: {}", e.getMessage(),e);
      return false;
    }
  }
}