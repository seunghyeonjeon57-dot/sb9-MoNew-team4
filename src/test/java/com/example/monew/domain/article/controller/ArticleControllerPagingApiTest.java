package com.example.monew.domain.article.controller;

import com.example.monew.domain.article.dto.CursorPageResponseArticleDto;
import com.example.monew.domain.article.service.ArticleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ArticleController.class)
@Import(ArticleController.class) // 빈 등록 유도
@AutoConfigureMockMvc(addFilters = false)
public class ArticleControllerPagingApiTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ArticleService articleService;

  @Configuration
  static class TestConfig {
    @Bean(name = "jpaMappingContext")
    public Object jpaMappingContext() {
      return new Object();
    }
  }

  @Test
  @DisplayName("커서 페이징 성공(API요청)")
  void pagingApiTest() throws Exception {
    UUID nextId = UUID.randomUUID();
    LocalDateTime nextTime = LocalDateTime.now();

    CursorPageResponseArticleDto mockResponse = new CursorPageResponseArticleDto(
        List.of(),
        nextId.toString(),
        nextTime,
        2,
        null,
        true
    );

    given(articleService.getArticles(any(), any(), anyInt()))
        .willReturn(mockResponse);

    mockMvc.perform(get("/api/articles")// 주소 주의
            .param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hasNext").value(true))
        .andExpect(jsonPath("$.nextCursor").value(nextId.toString()))
        .andDo(print());
  }
}