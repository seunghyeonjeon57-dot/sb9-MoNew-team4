package com.example.monew.global.infra.s3.log;

import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogBackupScheduler {

  private final LogS3Service logS3Service;
  private String logDirectory = "./logs";


  @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
  public void executeLogBackup() {
    log.info(">>> [Log Backup Start]");


    LocalDate targetDate = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);


    Path logFilePath = Paths.get(logDirectory,"app-" + targetDate + ".log");

    if (Files.exists(logFilePath)) {

      boolean isUploaded = logS3Service.uploadLogFile(logFilePath, targetDate);

      if (isUploaded) {
        try {
          Files.delete(logFilePath);
          log.info(">>> [Log Backup & Cleanup Success] File: {}", logFilePath.getFileName());
        } catch (IOException e) {
          log.error("<<< [Local File Delete Failed] Error: {}", e.getMessage());
        }
      }
    } else {
      log.warn("<<< [Log Backup Skip] File not found: {}", logFilePath);
    }
  }
}