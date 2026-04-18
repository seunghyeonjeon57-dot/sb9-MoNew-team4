package com.example.monew.domain.article.controller;

import com.example.monew.domain.article.dto.CursorPageResponseArticleDto;
import com.example.monew.domain.article.service.ArticleService;
import com.example.monew.domain.article.service.ArticleViewService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock; // 추가
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ArticleController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ArticleControllerPagingApiTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ArticleService articleService;

  @MockBean // ⭐ 이거 추가
  private ArticleViewService articleViewService;

  @Test
  @DisplayName("커서 페이징 목록 조회 성공 테스트")
  void getArticleList_Success() throws Exception {

    CursorPageResponseArticleDto mockResponse =
        new CursorPageResponseArticleDto(
            List.of(),
            "cursor123",
            LocalDateTime.now(),
            10,
            null,
            true
        );

    given(articleService.getArticles(any(), any(), anyInt()))
        .willReturn(mockResponse);

    mockMvc.perform(get("/api/articles")
            .param("size", "10"))
        .andExpect(status().isOk());

    verify(articleService).getArticles(any(), any(), eq(10));
  }
}