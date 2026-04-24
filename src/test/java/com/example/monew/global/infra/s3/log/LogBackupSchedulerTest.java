package com.example.monew.global.infra.s3.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogBackupSchedulerTest {

  @Mock
  private LogS3Service logS3Service;

  @InjectMocks
  private LogBackupScheduler logBackupScheduler;

  private Path logDirectory;
  private Path targetLogFile;
  private LocalDate targetDate;

  @BeforeEach
  void setUp() throws IOException {
    
    logDirectory = Paths.get("./logs");
    if (!Files.exists(logDirectory)) {
      Files.createDirectories(logDirectory);
    }

    targetDate = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
    targetLogFile = logDirectory.resolve("app-" + targetDate + ".log");

    
    Files.deleteIfExists(targetLogFile);
  }

  @AfterEach
  void tearDown() throws IOException {
    
    Files.deleteIfExists(targetLogFile);
  }

  @Test
  @DisplayName("성공: S3 업로드 성공 시 로컬 로그 파일이 삭제된다")
  void executeLogBackup_Success() throws IOException {
    
    Files.createFile(targetLogFile); 
    given(logS3Service.uploadLogFile(any(), eq(targetDate))).willReturn(true);

    
    logBackupScheduler.executeLogBackup();

    
    assertThat(Files.exists(targetLogFile))
        .as("업로드 성공 시 로컬 파일은 삭제되어야 함")
        .isFalse();
    verify(logS3Service).uploadLogFile(any(), eq(targetDate));
  }

  @Test
  @DisplayName("실패: S3 업로드 실패 시 로컬 로그 파일은 유지된다")
  void executeLogBackup_UploadFailed_KeepFile() throws IOException {
    
    Files.createFile(targetLogFile);
    given(logS3Service.uploadLogFile(any(), eq(targetDate))).willReturn(false);

    
    logBackupScheduler.executeLogBackup();

    
    assertThat(Files.exists(targetLogFile))
        .as("업로드 실패 시 로컬 파일은 보존되어야 함")
        .isTrue();
  }

  @Test
  @DisplayName("파일 없음: 백업할 로그 파일이 없으면 S3 업로드를 시도하지 않는다")
  void executeLogBackup_FileNotFound() {
    

    
    logBackupScheduler.executeLogBackup();

    
    verify(logS3Service, never()).uploadLogFile(any(), any());
  }
}