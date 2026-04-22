package com.example.monew.batch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.monew.config.NewsBackupBatchConfig;
import com.example.monew.domain.article.batch.service.BackupService;
import com.example.monew.domain.article.batch.service.S3Service;
import com.example.monew.domain.article.repository.ArticleRepository;
import com.example.monew.domain.article.repository.ArticleViewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;

@ExtendWith(MockitoExtension.class)
class BackupBatchTest {

  @Mock private ArticleRepository articleRepository;
  @Mock private ArticleViewRepository articleViewRepository;
  @Mock private S3Service s3Service;
  @Mock private JobLauncher jobLauncher;
  @Mock private Job newsBackupJob;

  @InjectMocks
  private BackupService backupService;

  @Test
  @DisplayName("백업 테스트: 서비스 호출 시 JobLauncher가 돌아가는지 확인")
  void backupTest() throws Exception {
    backupService.backupDailyNews();

    verify(jobLauncher, times(1)).run(any(), any());
  }

  @Test
  @DisplayName("복구 테스트: S3 다운로드 후 JobLauncher가 실행되는지 확인")
  void restoreTest() throws Exception {
    LocalDate targetDate = LocalDate.of(2026, 4, 21);
    File mockFile = File.createTempFile("restore-test", ".json");
    given(s3Service.download(any())).willReturn(mockFile);

    backupService.restoreNews(targetDate);

    verify(s3Service).download(any());
    verify(jobLauncher).run(any(), any());

    mockFile.delete();
  }
}