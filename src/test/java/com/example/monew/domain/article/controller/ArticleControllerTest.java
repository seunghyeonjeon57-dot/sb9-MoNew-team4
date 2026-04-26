package com.example.monew.domain.article.controller;

import com.example.monew.domain.article.batch.BackupBatch;
import com.example.monew.domain.article.batch.NewsRss;
import com.example.monew.domain.article.batch.service.BackupService;
import com.example.monew.domain.article.dto.ArticleSearchCondition;
import com.example.monew.domain.article.service.ArticleService;
import com.example.monew.domain.article.service.ArticleViewService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArticleController.class)
class ArticleControllerTest {

  @Autowired
  private MockMvc mockMvc;
  @MockitoBean
  private BackupService backupService;
  @MockitoBean
  private ArticleService articleService;

  @MockitoBean
  private ArticleViewService articleViewService;

  @MockitoBean
  private BackupBatch backupBatch;

  @MockitoBean
  private NewsRss newsRss;

  private final String USER_ID_HEADER = "Monew-Request-User-ID";
  private final String TEST_USER_ID = UUID.randomUUID().toString();

  @Test
  @DisplayName("기사 뷰 등록 테스트")
  void incrementArticleViewTest() throws Exception {
    UUID articleId = UUID.randomUUID();

    mockMvc.perform(post("/api/articles/{articleId}/article-views", articleId)
            .header(USER_ID_HEADER, TEST_USER_ID)) // 헤더 추가
        .andExpect(status().isOk());

    verify(articleViewService).logView(eq(articleId), any(UUID.class), any(String.class));
  }

  @Test
  @DisplayName("기사 목록 조회 테스트")
  void getArticleListTest() throws Exception {
    mockMvc.perform(get("/api/articles")
            .header(USER_ID_HEADER, TEST_USER_ID)
            .param("limit", "10"))
        .andExpect(status().isOk());

    verify(articleService).getArticles(any(ArticleSearchCondition.class));
  }
  @Test
  @DisplayName("기사 상세 조회 테스트")
  void getArticleDetailTest() throws Exception {
    UUID articleId = UUID.randomUUID();

    mockMvc.perform(get("/api/articles/{articleId}", articleId)
            .header(USER_ID_HEADER, TEST_USER_ID))
        .andExpect(status().isOk());

    verify(articleService).getArticleDetail(articleId);
  }

  @Test
  @DisplayName("기사 논리 삭제 테스트")
  void deleteArticleTest() throws Exception {
    UUID articleId = UUID.randomUUID();

    mockMvc.perform(delete("/api/articles/{articleId}", articleId))
        .andExpect(status().isNoContent());

    verify(articleService).isDeleted(articleId);
  }

  @Test
  @DisplayName("기사 물리 삭제 테스트")
  void hardDeleteArticleTest() throws Exception {
    UUID articleId = UUID.randomUUID();

    mockMvc.perform(delete("/api/articles/{articleId}/hard", articleId))
        .andExpect(status().isNoContent());

    verify(articleService).hardDelete(articleId);
  }


  @Test
  @DisplayName("기사 출처 목록 조회 테스트")
  void getArticleSourcesTest() throws Exception {
    mockMvc.perform(get("/api/articles/sources"))
        .andExpect(status().isOk());

    verify(articleService).getAllSources();
  }

  @Test
  @DisplayName("목록 조회 실패")
  void getArticleList_Fail_SizeExceeded() throws Exception {
    mockMvc.perform(get("/api/articles")
            .param("size", "101")) // 100 초과 에러
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("뉴스 복구 성공")
  void restoreFromS3_Success() throws Exception {
    String from = "2026-04-24T00:00:00";
    String to = "2026-04-25T00:00:00";

    mockMvc.perform(get("/api/articles/restore")
            .param("from", from)
            .param("to", to))
        .andExpect(status().isOk());

    verify(backupService).restoreNewsRange(any(LocalDateTime.class), any(LocalDateTime.class));
  }

  @Test
  @DisplayName("뉴스 복구 실패 - 미래 날짜")
  void restoreFromS3_Fail_FutureDate() throws Exception {
    String futureDate = LocalDateTime.now().plusDays(1).withNano(0).toString();


    mockMvc.perform(get("/api/articles/restore")
            .param("from", futureDate)
            .param("to", LocalDateTime.now().withNano(0).toString()))
        .andExpect(status().isBadRequest());
  }
}