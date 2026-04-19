package com.example.monew.batch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import com.example.monew.batch.service.S3Service;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.repository.ArticleRepository;
import com.fasterxml.jackson.databind.ObjectMapper; // ObjectMapper 주입 확인
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class BackupBatchTest {

  @Autowired
  private BackupBatch backupBatch;

  @Autowired
  private ObjectMapper objectMapper; // 실제 JSON 변환을 위해 실제 빈 사용

  @MockitoBean
  private ArticleRepository articleRepository;

  @MockitoBean
  private S3Service s3Service;

  @Test
  @DisplayName("매일 뉴스 백업 스케줄러 로직 테스트")
  void backupDailyNewsTest() throws Exception {
    // 1. 준비: 어제 날짜 데이터 생성
    ArticleEntity article = ArticleEntity.builder()
        .title("테스트 뉴스")
        .sourceUrl("http://test.com")
        .build();
    List<ArticleEntity> mockArticles = List.of(article);

    // 날짜 범위 조회를 Mocking (어제 날짜)
    given(articleRepository.findByPublishDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
        .willReturn(mockArticles);

    // 2. 실행
    backupBatch.backupDailyNews();

    // 3. 검증
    // S3Service의 upload 메서드가 "backup/" 경로로 호출되었는지 확인
    verify(s3Service, atLeastOnce()).upload(contains("backup/"), anyString());
    System.out.println("✅ 백업 로직 호출 및 S3 업로드 요청 확인 완료");
  }

  @Test
  @DisplayName("S3 데이터를 DB로 복구하는 로직 테스트")
  void restoreNewsTest() throws Exception {
    // 1. 준비: S3에서 내려받을 가짜 JSON 데이터 (엔티티 구조에 맞춰 생성)
    ArticleEntity article = ArticleEntity.builder()
        .title("복구된 뉴스")
        .sourceUrl("http://restore.com")
        .build();
    String mockJson = objectMapper.writeValueAsString(List.of(article));

    LocalDate targetDate = LocalDate.now().minusDays(1);

    given(s3Service.download(anyString())).willReturn(mockJson);

    // DB에 존재하지 않는다고 가정
    given(articleRepository.existsBySourceUrl("http://restore.com")).willReturn(false);

    // 2. 실행
    backupBatch.restoreNews(targetDate);

    // 3. 검증
    // save 메서드가 호출되었는지 확인
    verify(articleRepository, atLeastOnce()).save(any(ArticleEntity.class));
    System.out.println("✅ S3 다운로드 및 DB 복구 저장 로직 확인 완료");
  }
}