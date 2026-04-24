package com.example.monew.batch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.example.monew.domain.article.batch.service.BackupService;
import com.example.monew.domain.article.batch.service.S3Service;
import java.io.File;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

@ExtendWith(MockitoExtension.class)
class BackupServiceTest {

  @Mock private S3Service s3Service;
  @Mock private JobLauncher jobLauncher;
  @Mock private Job restoreJob;
  @Mock private Job backupJob;

  @InjectMocks
  private BackupService backupService;

  @Test
  @DisplayName("매일 백업 배치 정상작동")
  void backupDailyNews_Success() throws Exception {
    backupService.backupDailyNews();

    verify(jobLauncher, times(1)).run(eq(backupJob), any(JobParameters.class));
  }

  @Test
  @DisplayName("s3로 복구 성공")
  void restoreNews_Success() throws Exception {
    LocalDate targetDate = LocalDate.of(2026, 4, 24);
    File mockFile = mock(File.class);
    given(mockFile.getAbsolutePath()).willReturn("/tmp/test.json");
    given(s3Service.download(anyString())).willReturn(mockFile);

    backupService.restoreNews(targetDate);

    verify(jobLauncher, times(1)).run(eq(restoreJob), any(JobParameters.class));
  }

  @Test
  @DisplayName("복구 중 에러 발생 시 catch")
  void restoreNews_Fail_Catch() throws Exception {
    given(s3Service.download(anyString())).willThrow(new RuntimeException("S3 에러"));

    backupService.restoreNews(LocalDate.now());

    verify(jobLauncher, never()).run(any(), any());
  }
}