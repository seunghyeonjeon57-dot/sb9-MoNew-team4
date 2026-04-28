package com.example.monew.batch;


import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.monew.domain.article.batch.BackupBatch;
import com.example.monew.domain.article.batch.service.BackupService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class BackupBatchTest {

  @Mock
  private BackupService backupService;

  @InjectMocks
  private BackupBatch backupBatch;

  @Test
  @DisplayName("백업 정상 호출 성공")
  void runBackupTest() {
    backupBatch.runBackup();

    verify(backupService, times(1)).backupDailyNews();
  }
}