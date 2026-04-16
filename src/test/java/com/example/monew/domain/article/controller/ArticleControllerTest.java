package com.example.monew.domain.article.controller;

import com.example.monew.domain.article.controller.ArticleController;
import com.example.monew.domain.article.dto.ArticleDto;
import com.example.monew.domain.article.dto.CursorPageResponseArticleDto;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.service.ArticleService;
import com.example.monew.global.exception.MonewException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ArticleController.class)
class ArticleControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ArticleService articleService;

  @Nested
  @DisplayName("상세 조회 테스트")
  class GetDetail {


    @Test
    @DisplayName("성공")
    void success() throws Exception {
      UUID id = UUID.randomUUID();

      given(articleService.getArticleDetail(id)).willReturn(mock(ArticleDto.class));

      mockMvc.perform(get("/api/articles/{id}", id))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("실패")
    void fail() throws Exception {
      UUID id = UUID.randomUUID();
      given(articleService.getArticleDetail(id)).willThrow(RuntimeException.class);
      mockMvc.perform(get("/api/articles/{id}", id)).andExpect(status().isInternalServerError());
    }
  }

  @Nested
  @DisplayName("조회수 증가 테스트")
  class IncrementView {


    @Test
    @DisplayName("성공: 200 OK")

    void success() throws Exception {
      UUID id = UUID.randomUUID();
      mockMvc.perform(post("/api/articles/{id}/article-views", id)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("실패: 서비스 예외 발생 시 500")
    void fail() throws Exception {
      UUID id = UUID.randomUUID();
      willThrow(RuntimeException.class).given(articleService).incrementViewCount(id);
      mockMvc.perform(post("/api/articles/{id}/article-views", id)).andExpect(status().isInternalServerError());
    }
  }

  @Nested
  @DisplayName("논리 삭제 테스트")
  class SoftDelete {
    @Test
    @DisplayName("성공 204")
    void success() throws Exception {
      UUID id = UUID.randomUUID();
      mockMvc.perform(delete("/api/articles/{id}", id)).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("실패")
    void fail() throws Exception {
      UUID id = UUID.randomUUID();
      willThrow(RuntimeException.class).given(articleService).isDeleted(id);
      mockMvc.perform(delete("/api/articles/{id}", id)).andExpect(status().isInternalServerError());
    }
  }


  @Nested
  @DisplayName("기사 복구 테스트")
  class Restore {
    UUID id = UUID.randomUUID();

    @Test
    @DisplayName("성공")
    void success() throws Exception {
      mockMvc.perform(get("/api/articles/restore").param("articleId", id.toString())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("실패")
    void fail() throws Exception {
      willThrow(RuntimeException.class).given(articleService).restore(id);
      mockMvc.perform(get("/api/articles/restore").param("articleId", id.toString())).andExpect(status().isInternalServerError());
    }
  }

  @Test
  @DisplayName("성공")
  void getList_Success() throws Exception {
    CursorPageResponseArticleDto mockResponse = new CursorPageResponseArticleDto(
        List.of(), // content
        null,      // nextCursor
        null,      // nextAfter
        10,        // size
        0L,        // totalElements
        false      // hasNext
    );

    given(articleService.getArticleList(any(), any(), any(), any(), any(), any()))
        .willReturn(mockResponse);

    mockMvc.perform(get("/api/articles"))
        .andExpect(status().isOk());
  }
}