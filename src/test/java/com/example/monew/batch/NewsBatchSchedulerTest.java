package com.example.monew.batch;


import com.example.monew.domain.article.batch.service.BackupService;
import com.example.monew.domain.article.batch.service.S3Service;
import com.example.monew.domain.article.repository.ArticleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.time.LocalDate;
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

@ExtendWith(MockitoExtension.class)
class NewsBatchSchedulerTest {

  @Mock private S3Service s3Service;
  @Mock private JobLauncher jobLauncher;
  @Mock private Job restoreJob;
  @Mock private ArticleRepository articleRepository;
  @Mock private ObjectMapper objectMapper;

  @InjectMocks
  private BackupService backupService;

  @Test
  @DisplayName("복구 실행 시 S3 다운로드& 배치 호출 확인")
  void restoreNewsSimpleTest() throws Exception {
    LocalDate targetDate = LocalDate.now();
    File fakeFile = File.createTempFile("test", ".json");

    given(s3Service.download(anyString())).willReturn(fakeFile);

    backupService.restoreNews(targetDate);


    verify(s3Service).download(contains(targetDate.toString()));

    verify(jobLauncher).run(eq(restoreJob), any(JobParameters.class));

    fakeFile.delete();
  }
}