package com.example.monew.domain.article.batch;


import com.example.monew.domain.article.batch.service.BackupService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackupBatch {

  private final BackupService backupService;

  public void runBackup() {

    backupService.backupDailyNews();
  }
}
