package com.example.monew.global.infra.s3.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LogBackupSchedulerTest {

  @Mock
  private LogS3Service logS3Service;

  @InjectMocks
  private LogBackupScheduler logBackupScheduler;

  @TempDir
  Path tempDir; 

  private Path targetLogFile;
  private LocalDate targetDate;

  @BeforeEach
  void setUp() {
    targetDate = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
    
    targetLogFile = tempDir.resolve("app-" + targetDate + ".log");

    
    ReflectionTestUtils.setField(logBackupScheduler, "logDirectory", tempDir.toString());
  }

  @Test
  @DisplayName("성공: S3 업로드 성공 시 로컬 로그 파일이 삭제된다")
  void executeLogBackup_Success() throws IOException {
    Files.createFile(targetLogFile);
    given(logS3Service.uploadLogFile(any(), eq(targetDate))).willReturn(true);

    logBackupScheduler.executeLogBackup();

    assertThat(Files.exists(targetLogFile)).isFalse();
    verify(logS3Service).uploadLogFile(any(), eq(targetDate));
  }

  @Test
  @DisplayName("실패: S3 업로드 실패 시 로컬 로그 파일은 유지된다")
  void executeLogBackup_UploadFailed_KeepFile() throws IOException {
    Files.createFile(targetLogFile);
    given(logS3Service.uploadLogFile(any(), eq(targetDate))).willReturn(false);

    logBackupScheduler.executeLogBackup();

    assertThat(Files.exists(targetLogFile)).isTrue();
  }

  @Test
  @DisplayName("파일 없음: 백업할 로그 파일이 없으면 S3 업로드를 시도하지 않는다")
  void executeLogBackup_FileNotFound() {
    logBackupScheduler.executeLogBackup();
    verify(logS3Service, never()).uploadLogFile(any(), any());
  }
}