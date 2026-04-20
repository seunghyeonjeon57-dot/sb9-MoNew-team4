package com.example.monew.batch;


import com.example.monew.batch.service.BackupService;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackupBatch {

  private final BackupService backupService;

  @Scheduled(cron = "0 0 1 * * *")
  public void runBackup() {
    backupService.backupDailyNews(); //외부 호출
  }
}
