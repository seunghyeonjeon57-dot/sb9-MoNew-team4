package com.example.monew.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.monew.domain.user.dto.UserDto;
import com.example.monew.domain.user.dto.request.UserLoginRequest;
import com.example.monew.domain.user.dto.request.UserRegisterRequest;
import com.example.monew.domain.user.dto.request.UserUpdateRequest;
import com.example.monew.domain.user.exception.DuplicateEmailException;
import com.example.monew.domain.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean // 최신 스펙 적용
  private UserService userService;

  @Autowired
  private ObjectMapper objectMapper;

  private UUID userId;
  private UserRegisterRequest registerRequest;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    registerRequest = new UserRegisterRequest("test@email.com", "password123", "nickname");
  }

  @Nested
  @DisplayName("회원가입(@PostMapping)")
  class Create {

    @Test
    @DisplayName("성공: 새로운 사용자를 등록하고 201을 반환한다")
    void create_success() throws Exception {
      mockMvc.perform(post("/api/users")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(registerRequest)))
          .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("실패: 이메일이 중복되면 409 Conflict를 반환한다")
    void create_fail_duplicate() throws Exception {
      // UserService가 예외를 던지도록 모킹
      doThrow(new DuplicateEmailException("이미 존재하는 이메일입니다."))
          .when(userService).create(any());

      mockMvc.perform(post("/api/users")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(registerRequest)))
          .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("실패: 입력값이 누락되면(@Valid) 400 Bad Request를 반환한다")
    void create_fail_validation() throws Exception {
      UserRegisterRequest invalidRequest = new UserRegisterRequest("", "", ""); // 빈 값 전달

      mockMvc.perform(post("/api/users")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(invalidRequest)))
          .andExpect(status().isBadRequest());
    }
  }
  @Nested
  @DisplayName("사용자 수정(@PatchMapping)")
  class Update {
    @Test
    @DisplayName("성공: 사용자 정보를 수정하고 200 OK를 반환한다")
    void update_success() throws Exception {
      UserUpdateRequest updateRequest = new UserUpdateRequest("newNickname");
      UserDto responseDto = new UserDto(userId, "test@email.com", "newNickname", null);

      given(userService.updateUser(any(), any())).willReturn(responseDto);

      mockMvc.perform(patch("/api/users/{userId}", userId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(updateRequest)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.nickname").value("newNickname"));
    }
  }

  @Nested
  @DisplayName("로그인(@PostMapping/login)")
  class Login {

    @Test
    @DisplayName("성공: 로그인 성공 시 200 OK와 사용자 정보를 반환한다")
    void login_success() throws Exception {
      UserLoginRequest loginRequest = new UserLoginRequest("test@email.com", "password123");
      UserDto responseDto = new UserDto(userId,"test@email.com", "nickname",null);

      given(userService.login(any())).willReturn(responseDto);

      mockMvc.perform(post("/api/users/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(loginRequest)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.email").value("test@email.com"))
          .andExpect(jsonPath("$.nickname").value("nickname"));
    }
  }

  @Nested
  @DisplayName("사용자 삭제(@DeleteMapping)")
  class Delete {

    @Test
    @DisplayName("성공: 논리 삭제 시 204 No Content를 반환한다")
    void softDelete_success() throws Exception {
      mockMvc.perform(delete("/api/users/{userId}", userId))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("성공: 물리 삭제 시 204 No Content를 반환한다")
    void hardDelete_success() throws Exception {
      mockMvc.perform(delete("/api/users/{userId}/hard", userId))
          .andExpect(status().isNoContent());
    }
  }
}