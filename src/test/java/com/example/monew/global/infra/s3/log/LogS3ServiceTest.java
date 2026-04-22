package com.example.monew.global.infra.s3.log;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class LogS3ServiceTest {

  @Autowired
  private LogS3Service logS3Service;

  @Test
  @DisplayName("로그 파일 S3 업로드 테스트 (KST 타임존 반영)")
  void uploadLogFileTest() throws IOException {

    
    Path testFilePath = Paths.get("./logs/test-app.log");

    
    if (!Files.exists(testFilePath.getParent())) {
      Files.createDirectories(testFilePath.getParent());
    }

    
    Files.writeString(testFilePath, "This is a test log content for S3 backup verification.");

    
    LocalDate targetDate = LocalDate.now(ZoneId.of("Asia/Seoul"));

    
    boolean isUploaded = logS3Service.uploadLogFile(testFilePath, targetDate);

    
    
    assertThat(isUploaded).isTrue();
    System.out.println(">>> [성공] S3 업로드 완료 (날짜 기준: " + targetDate + ")");

    
    if (isUploaded) {
      Files.deleteIfExists(testFilePath); 
      assertThat(Files.exists(testFilePath)).isFalse();
      System.out.println(">>> [성공] 로컬 테스트 파일 삭제 및 정제 완료!");
    }
  }
}