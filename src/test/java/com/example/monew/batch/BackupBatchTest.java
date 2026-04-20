package com.example.monew.batch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.monew.domain.article.batch.service.BackupService;
import com.example.monew.domain.article.batch.service.S3Service;
import com.example.monew.domain.article.repository.ArticleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class BackupBatchTest {

  @MockitoBean private ArticleRepository articleRepository;
  @MockitoBean private S3Service s3Service;

  //JobLauncher만 Mock으로
  // Job은 진짜 빈 사용
  @MockitoBean private org.springframework.batch.core.launch.JobLauncher jobLauncher;

  @Autowired private BackupService backupService;
  @Autowired private ObjectMapper objectMapper;

  @Test
  @DisplayName("백업 테스트- JobLauncher 실행 확인")
  void backupDailyNewsTest() throws Exception {
    backupService.backupDailyNews();

    verify(jobLauncher).run(any(), any());
  }

  @Test
  @DisplayName("복구테스트 - S3 다운로드 후 배치")
  void restoreNewsTest() throws Exception {
    LocalDate targetDate = LocalDate.now().minusDays(1);
    // String X -> File 리턴
    java.io.File tempFile = java.io.File.createTempFile("test", ".json");
    given(s3Service.download(anyString())).willReturn(tempFile);

    backupService.restoreNews(targetDate);

    verify(s3Service).download(anyString());
    verify(jobLauncher).run(any(), any());

    tempFile.delete(); // 테스트용 파일 삭제
  }
}