package com.example.monew.domain.interest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.monew.domain.interest.repository.SubscriptionRepository;
import com.example.monew.domain.user.entity.User;
import com.example.monew.domain.user.repository.UserRepository;
import com.example.monew.domain.user.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class InterestApiIntegrationTest {

  private static final String USER_HEADER = "MoNew-Request-User-ID";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserService userService;

  @Autowired
  private SubscriptionRepository subscriptionRepository;

  @Test
  @DisplayName("I1→I5→I2→I6→I2→I3→I4 전체 플로우 (구독 200, 취소 200, 삭제 204)")
  void fullFlow() throws Exception {
    UUID userId = UUID.randomUUID();

    MvcResult created = mockMvc.perform(post("/api/interests")
            .header(USER_HEADER, userId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                Map.of("name", "풀플로우관심사", "keywords", List.of("A", "B")))))
        .andExpect(status().isCreated())
        .andReturn();
    JsonNode createdJson = objectMapper.readTree(created.getResponse().getContentAsString());
    UUID interestId = UUID.fromString(createdJson.get("id").asText());

    mockMvc.perform(post("/api/interests/" + interestId + "/subscriptions")
            .header(USER_HEADER, userId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.interestName").value("풀플로우관심사"))
        .andExpect(jsonPath("$.interestKeywords[0]").value("A"));

    mockMvc.perform(get("/api/interests")
            .header(USER_HEADER, userId.toString())
            .param("orderBy", "name")
            .param("direction", "ASC")
            .param("limit", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[?(@.id=='" + interestId + "')].subscribedByMe").value(true));

    mockMvc.perform(delete("/api/interests/" + interestId + "/subscriptions")
            .header(USER_HEADER, userId.toString()))
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/interests")
            .header(USER_HEADER, userId.toString())
            .param("orderBy", "name")
            .param("direction", "ASC")
            .param("limit", "20"))
        .andExpect(jsonPath("$.content[?(@.id=='" + interestId + "')].subscribedByMe").value(false));

    mockMvc.perform(patch("/api/interests/" + interestId)
            .header(USER_HEADER, userId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("keywords", List.of("C", "D")))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.keywords[0]").value("C"));

    mockMvc.perform(delete("/api/interests/" + interestId)
            .header(USER_HEADER, userId.toString()))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("80% 유사 이름 → 409 SIMILAR_INTEREST_NAME")
  void similarNameRejected() throws Exception {
    UUID userId = UUID.randomUUID();
    mockMvc.perform(post("/api/interests")
            .header(USER_HEADER, userId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                Map.of("name", "유사한이름관심사", "keywords", List.of("X")))))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/interests")
            .header(USER_HEADER, userId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                Map.of("name", "유사한이름관심사A", "keywords", List.of("Y")))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("SIMILAR_INTEREST_NAME"));
  }

  @Test
  @DisplayName("정확 일치 이름 → 409 SIMILAR_INTEREST_NAME (similarity=1.0)")
  void exactMatchRejected() throws Exception {
    UUID userId = UUID.randomUUID();
    mockMvc.perform(post("/api/interests")
            .header(USER_HEADER, userId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                Map.of("name", "정확일치테스트", "keywords", List.of("A")))))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/interests")
            .header(USER_HEADER, userId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                Map.of("name", "정확일치테스트", "keywords", List.of("B")))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("SIMILAR_INTEREST_NAME"));
  }

  @Test
  @DisplayName("PATCH에 name 포함 → 400 INTEREST_NAME_IMMUTABLE")
  void patchWithNameRejected() throws Exception {
    UUID userId = UUID.randomUUID();
    MvcResult created = mockMvc.perform(post("/api/interests")
            .header(USER_HEADER, userId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                Map.of("name", "이름불변테스트", "keywords", List.of("A")))))
        .andExpect(status().isCreated())
        .andReturn();
    UUID interestId = UUID.fromString(
        objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText());

    java.util.Map<String, Object> body = new java.util.HashMap<>();
    body.put("name", "바뀐이름");
    body.put("keywords", List.of("ML"));

    mockMvc.perform(patch("/api/interests/" + interestId)
            .header(USER_HEADER, userId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INTEREST_NAME_IMMUTABLE"));
  }

  @Test
  @DisplayName("잘못된 orderBy → 400 INVALID_SORT_PARAMETER")
  void invalidSort() throws Exception {
    mockMvc.perform(get("/api/interests")
            .header(USER_HEADER, UUID.randomUUID().toString())
            .param("orderBy", "foo")
            .param("direction", "ASC")
            .param("limit", "20"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_SORT_PARAMETER"));
  }

  @Test
  @DisplayName("헤더 누락 구독 시도 → 400 MISSING_REQUEST_HEADER")
  void missingHeader() throws Exception {
    mockMvc.perform(post("/api/interests/" + UUID.randomUUID() + "/subscriptions"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MISSING_REQUEST_HEADER"));
  }

  @Test
  @DisplayName("미존재 인터레스트 구독 → 404 INTEREST_NOT_FOUND")
  void subscribeNotFound() throws Exception {
    mockMvc.perform(post("/api/interests/" + UUID.randomUUID() + "/subscriptions")
            .header(USER_HEADER, UUID.randomUUID().toString()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("INTEREST_NOT_FOUND"));
  }

  @Test
  @DisplayName("유저 물리 삭제 → 해당 유저 구독 전량 정리 (MON-101 cascade)")
  void userHardDelete_cleansSubscriptions() throws Exception {
    User user = userRepository.save(
        new User("cascadeUser", "cascade_" + UUID.randomUUID() + "@monew.com", "pw123!"));
    UUID userId = user.getId();

    String nameA = "cascadeAlpha" + System.nanoTime();
    MvcResult a = mockMvc.perform(post("/api/interests")
            .header(USER_HEADER, userId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                Map.of("name", nameA, "keywords", List.of("a")))))
        .andExpect(status().isCreated())
        .andReturn();
    UUID interestA = UUID.fromString(
        objectMapper.readTree(a.getResponse().getContentAsString()).get("id").asText());

    String nameB = "별도도메인" + System.nanoTime();
    MvcResult b = mockMvc.perform(post("/api/interests")
            .header(USER_HEADER, userId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                Map.of("name", nameB, "keywords", List.of("b")))))
        .andExpect(status().isCreated())
        .andReturn();
    UUID interestB = UUID.fromString(
        objectMapper.readTree(b.getResponse().getContentAsString()).get("id").asText());

    mockMvc.perform(post("/api/interests/" + interestA + "/subscriptions")
            .header(USER_HEADER, userId.toString()))
        .andExpect(status().isOk());
    mockMvc.perform(post("/api/interests/" + interestB + "/subscriptions")
            .header(USER_HEADER, userId.toString()))
        .andExpect(status().isOk());

    assertThat(subscriptionRepository.findAllByUserId(userId)).hasSize(2);

    userService.hardDeleteUser(userId);

    assertThat(subscriptionRepository.findAllByUserId(userId)).isEmpty();
  }
}
