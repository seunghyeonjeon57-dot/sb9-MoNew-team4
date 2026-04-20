package com.example.monew.domain.article.batch;

import com.example.monew.domain.article.batch.exception.RestoreFailedException;
import com.example.monew.domain.article.batch.exception.S3FileNotFoundException;
import com.example.monew.domain.article.batch.service.BackupService;
import com.example.monew.domain.article.batch.service.S3Service;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.repository.ArticleRepository;
import com.example.monew.domain.article.service.ArticleService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

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