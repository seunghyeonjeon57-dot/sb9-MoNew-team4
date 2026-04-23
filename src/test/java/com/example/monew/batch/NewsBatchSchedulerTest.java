package com.example.monew.batch;


import com.example.monew.domain.article.batch.NewsBatchScheduler;
import com.example.monew.domain.article.batch.NewsCollector;
import com.example.monew.domain.article.batch.NewsRss;
import com.example.monew.domain.article.batch.service.BackupService;
import com.example.monew.domain.article.batch.service.S3Service;
import com.example.monew.domain.article.repository.ArticleRepository;
import com.example.monew.domain.article.service.ArticleService;
import com.example.monew.domain.interest.entity.Interest;
import com.example.monew.domain.interest.entity.InterestKeyword;
import com.example.monew.domain.interest.repository.InterestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class NewsBatchSchedulerTest {

  @Mock private NewsCollector newsCollector;
  @Mock private NewsRss newsRss;
  @Mock private ArticleRepository articleRepository;
  @Mock private BackupService backupService;
  @Mock private InterestRepository interestRepository;
  @Mock private ArticleService articleService;

  @InjectMocks
  private NewsBatchScheduler newsBatchScheduler;

  @Test
  @DisplayName("뉴스 배치 실행 테스트")
  void runNewsBatchTest() {
    Interest mockInterest = mock(Interest.class);
    InterestKeyword mockKeyword = mock(InterestKeyword.class);

    given(mockKeyword.getValue()).willReturn("경제");
    given(mockInterest.getKeywords()).willReturn(List.of(mockKeyword));
    given(interestRepository.findAll()).willReturn(List.of(mockInterest));

    given(newsRss.getRss()).willReturn(Map.of("테스트언론사", "https://test.com/rss"));

    given(newsCollector.fetchNaver(anyString())).willReturn(Collections.emptyList());
    given(newsCollector.fetchRss(anyString(), anyString(), anyString())).willReturn(Collections.emptyList());

    newsBatchScheduler.runNewsBatch();

    verify(interestRepository).findAll();
    verify(newsCollector, atLeastOnce()).fetchNaver("경제");
    verify(newsCollector, atLeastOnce()).fetchRss(anyString(), anyString(), eq("경제"));
    verify(articleService, atLeastOnce()).saveInChunks(any());
  }

  @Test
  @DisplayName("키워드가 없을 때 조기 종료")
  void runNewsBatchEmptyKeywordsTest() {
    given(interestRepository.findAll()).willReturn(Collections.emptyList());

    newsBatchScheduler.runNewsBatch();

    verify(newsCollector, never()).fetchNaver(anyString());
    log.info("조기 종료");
  }
}