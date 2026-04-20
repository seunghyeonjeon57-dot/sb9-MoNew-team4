package com.example.monew.domain.comment.controller;

import com.example.monew.domain.comment.dto.CommentRegisterRequest;
import com.example.monew.domain.comment.dto.CommentUpdateRequest;
import com.example.monew.domain.comment.service.CommentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

// RED 원인: CommentController 클래스가 없어서 컴파일 에러 발생
@WebMvcTest(CommentController.class)
class CommentControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockitoBean private CommentService commentService;

  @Test
  @DisplayName("올바른 데이터로 댓글 등록 요청 시 200 OK를 반환한다.")
  void registerComment_HttpOk() throws Exception {
    CommentRegisterRequest request = new CommentRegisterRequest(
        UUID.randomUUID(), UUID.randomUUID(), "컨트롤러 테스트 댓글"
    );
    String jsonRequest = objectMapper.writeValueAsString(request);

    // 매핑이 없다면 404 Not Found 로 실패합니다.
    mockMvc.perform(post("/api/comments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonRequest))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("댓글 수정 시 작성자 ID와 함께 요청하면 200 OK를 반환한다.")
  void updateComment_HttpOk() throws Exception {
    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    CommentUpdateRequest request = new CommentUpdateRequest("수정된 댓글입니다.");
    String jsonRequest = objectMapper.writeValueAsString(request);

    mockMvc.perform(patch("/api/comments/{commentId}", commentId)
            .header("MoNew-Request-User-ID", userId.toString()) // 요구사항 헤더 추가
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonRequest))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("댓글 삭제 요청 시 204 No Content를 반환한다.")
  void deleteComment_HttpNoContent() throws Exception {
    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    mockMvc.perform(delete("/api/comments/{commentId}", commentId)
            .header("MoNew-Request-User-ID", userId.toString()))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("댓글 좋아요를 등록하면 200 OK를 반환한다.")
  void addCommentLike_Success() throws Exception {
    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    mockMvc.perform(post("/api/comments/{commentId}/comment-likes", commentId)
        .header("MoNew-Request-User-ID", userId.toString()))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("댓글 좋아요를 취소하면 200 OK를 반환한다.")
  void removeCommentLike_Success() throws Exception {
    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    mockMvc.perform(delete("/api/comments/{commentId}/comment-likes", commentId)
            .header("MoNew-Request-User-ID", userId.toString()))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("기사별 댓글 조회하면 200 OK를 반환한다.")
  void getArticleComments_Success() throws Exception {
    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    mockMvc.perform(get("/api/comments")
            .header("Monew-Request-User-ID", userId.toString())
            .param("articleId", articleId.toString())
            .param("orderBy", "likeCount")
            .param("direction", "DESC")
            .param("limit", "50"))
        .andExpect(status().isOk());
  }
    mockMvc.perform(post("/api/comments/{commentId}/comment-likes", commentId)
            .header("MoNew-Request-User-ID", userId.toString()))
        .andExpect(status().isOk());
  }
}