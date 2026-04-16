package com.example.monew.domain.comment.controller;

import com.example.monew.domain.comment.dto.CommentRegisterRequest;
import com.example.monew.domain.comment.service.CommentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// RED 원인: CommentController 클래스가 없어서 컴파일 에러 발생
@WebMvcTest(CommentController.class)
class CommentControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private CommentService commentService;

  @Test
  @DisplayName("올바른 데이터로 댓글 등록 요청 시 200 OK를 반환한다.")
  void registerComment_HttpOk() throws Exception {
    CommentRegisterRequest request = new CommentRegisterRequest(
        UUID.randomUUID(), UUID.randomUUID(), "컨트롤러 테스트 댓글"
    );
    String jsonRequest = objectMapper.writeValueAsString(request);

    // 매핑이 없다면 404 Not Found 로 실패합니다.
    mockMvc.perform(post("/api/v1/comments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonRequest))
        .andExpect(status().isOk());
  }
}