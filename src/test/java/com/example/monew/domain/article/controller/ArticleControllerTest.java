package com.example.monew.domain.article.controller;

import com.example.monew.domain.article.service.ArticleService;
import com.example.monew.domain.article.service.ArticleViewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
  @MockitoBean // 👈 이거 추가!
  private ArticleService articleService;

  @MockitoBean // 👈 이거 추가!
  private ArticleViewService articleViewService;

  @MockitoBean //BackupBatch를 가짜 빈으로 등록합니다.
  private com.example.monew.batch.BackupBatch backupBatch;

  @MockitoBean // 혹시 모르니 NewsRss도 같이 넣어두면 안전합니다.
  private com.example.monew.batch.NewsRss newsRss;

  @Test
  @DisplayName("기사 뷰 등록 테스트")
  void incrementArticleViewTest() throws Exception {
    UUID articleId = UUID.randomUUID();

    mockMvc.perform(post("/api/articles/{articleId}/article-views", articleId))
        .andExpect(status().isOk());

    verify(articleViewService).logView(eq(articleId), any(UUID.class), any(String.class));
  }

  @Test
  @DisplayName("기사 목록 조회 테스트")
  void getArticleListTest() throws Exception {
    mockMvc.perform(get("/api/articles")
            .param("size", "10"))
        .andExpect(status().isOk());

    verify(articleService).getArticles(any(), any(), eq(10));
  }

  @Test
  @DisplayName("기사 상세 조회 테스트")
  void getArticleDetailTest() throws Exception {
    UUID articleId = UUID.randomUUID();

    mockMvc.perform(get("/api/articles/{articleId}", articleId))
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
}