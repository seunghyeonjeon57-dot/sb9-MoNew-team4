package com.example.monew.domain.article.batch;

import com.example.monew.domain.article.batch.service.BackupService;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class NewsBatchScheduler {

  private final BackupService backupService;

  @Scheduled(cron = "0 0 * * * *")
  public void runNewsBatch() {
    log.info("뉴스 수집 시작");
      backupService.backupDailyNews();
    log.info("뉴스 수집 및 백업 예약 완료");
  }

}