package com.example.monew.batch;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.hibernate.validator.internal.util.Contracts.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.example.monew.domain.article.batch.exception.RestoreFailedException;
import com.example.monew.domain.article.batch.service.BackupService;
import com.example.monew.domain.article.batch.service.S3Service;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

  @Mock
  private S3Service s3Service;
  @Mock
  private JobLauncher jobLauncher;
  @Mock
  private Job restoreJob;
  @Mock
  private Job backupJob;

  @InjectMocks
  private BackupService backupService;

  @Test
  @DisplayName("매일 백업 배치 정상작동")
  void backupDailyNews_Success() throws Exception {
    backupService.backupDailyNews();

    verify(jobLauncher, times(1)).run(any(Job.class), any(JobParameters.class));
  }

  @Test
  @DisplayName("s3로 복구 성공")
  void restoreNews_Success() throws Exception {
    LocalDateTime targetTime = LocalDateTime.of(2026, 4, 24, 1, 0, 0);
    File mockFile = mock(File.class);
    given(mockFile.getAbsolutePath()).willReturn("/tmp/test.json");
    given(s3Service.download(anyString())).willReturn(mockFile);

    backupService.restoreNews(targetTime);

    verify(jobLauncher, times(1)).run(any(Job.class), any(JobParameters.class));
  }

  @Test
  @DisplayName("복구 중 에러 발생 시 catch")
  void restoreNews_Fail_Catch() throws Exception {
    given(s3Service.download(anyString())).willThrow(new RuntimeException("S3 에러"));

    assertThatThrownBy(() -> backupService.restoreNews(LocalDateTime.now()))
        .isInstanceOf(
            com.example.monew.domain.article.batch.exception.RestoreFailedException.class);

    verify(jobLauncher, never()).run(any(), any());
  }

  @Test
  @DisplayName("범위 복구(restoreNewsRange) 성공 - 여러 날짜 정상 호출 확인")
  void restoreNewsRange_Success() throws Exception {
    LocalDateTime from = LocalDateTime.of(2026, 4, 24, 0, 0);
    LocalDateTime to = LocalDateTime.of(2026, 4, 25, 23, 59);

    File mockFile = mock(File.class);
    given(mockFile.getAbsolutePath()).willReturn("/tmp/test.json");
    given(s3Service.download(anyString())).willReturn(mockFile);

    backupService.restoreNewsRange(from, to);

    verify(jobLauncher, times(2)).run(any(Job.class), any(JobParameters.class));
    verify(s3Service, times(2)).download(anyString());
  }

  @Test
  @DisplayName("범위 복구 중 특정 날짜 실패 시에도 다음 날짜 계속 진행 확인")
  void restoreNewsRange_PartialFail_SuccessTest() throws Exception {
    LocalDateTime from = LocalDateTime.of(2026, 4, 24, 0, 0);
    LocalDateTime to = LocalDateTime.of(2026, 4, 25, 0, 0);

    File mockFile = mock(File.class);
    given(mockFile.getAbsolutePath()).willReturn("/tmp/test.json");

    given(s3Service.download(anyString()))
        .willThrow(new RuntimeException("S3 연결 실패"))
        .willReturn(mockFile);

    assertThrows(RestoreFailedException.class, () -> {
      backupService.restoreNewsRange(from, to);
    });

    verify(s3Service, times(2)).download(anyString());

    verify(jobLauncher, times(1)).run(any(Job.class), any(JobParameters.class));
  }
}