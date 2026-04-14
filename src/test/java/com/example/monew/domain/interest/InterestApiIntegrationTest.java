package com.example.monew.domain.interest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.monew.domain.interest.repository.InterestRepository;
import com.example.monew.domain.interest.repository.InterestSubscriptionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InterestApiIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private InterestRepository interestRepository;

  @Autowired
  private InterestSubscriptionRepository subscriptionRepository;

  @BeforeEach
  void cleanUp() {
    subscriptionRepository.deleteAll();
    interestRepository.deleteAll();
  }

  @Test
  @DisplayName("full-flow: 생성 → 목록 조회 → 키워드 수정 → 구독 → 구독자수 반영 → 구독 취소 → 삭제")
  void fullFlow() throws Exception {
    UUID userId = UUID.randomUUID();

    MvcResult createResult = mockMvc.perform(post("/api/interests")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "name", "인공지능",
                "keywords", List.of("AI", "ML")))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("인공지능"))
        .andExpect(jsonPath("$.subscriberCount").value(0))
        .andExpect(jsonPath("$.subscribed").value(false))
        .andReturn();

    UUID interestId = UUID.fromString(
        objectMapper.readTree(createResult.getResponse().getContentAsString())
            .get("id").asText());

    mockMvc.perform(get("/api/interests").param("keyword", "AI"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(interestId.toString()))
        .andExpect(jsonPath("$.totalElements").value(1));

    mockMvc.perform(patch("/api/interests/{id}", interestId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "keywords", List.of("AI", "ML", "LLM")))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.keywords.length()").value(3));

    mockMvc.perform(post("/api/interests/{id}/subscriptions", interestId)
            .header("MoNew-Request-User-ID", userId.toString()))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/api/interests")
            .header("MoNew-Request-User-ID", userId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].subscriberCount").value(1))
        .andExpect(jsonPath("$.content[0].subscribed").value(true));

    mockMvc.perform(delete("/api/interests/{id}/subscriptions", interestId)
            .header("MoNew-Request-User-ID", userId.toString()))
        .andExpect(status().isNoContent());

    mockMvc.perform(delete("/api/interests/{id}", interestId))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/interests"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  @DisplayName("유사도 80% 이상 이름은 409 SIMILAR_INTEREST_NAME")
  void createSimilarNameReturns409() throws Exception {
    mockMvc.perform(post("/api/interests")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "name", "스포츠",
                "keywords", List.of("축구")))))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/interests")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "name", "스포츠",
                "keywords", List.of("농구")))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("SIMILAR_INTEREST_NAME"));
  }

  @Test
  @DisplayName("목록 조회: subscriberCount desc + 커서로 페이지네이션 전체 경로가 동작한다")
  void listPaginationBySubscriberCount() throws Exception {
    UUID a = createAndGetId("A관심사", List.of("k"));
    UUID b = createAndGetId("B관심사", List.of("k"));
    UUID c = createAndGetId("C관심사", List.of("k"));

    UUID u1 = UUID.randomUUID();
    UUID u2 = UUID.randomUUID();
    UUID u3 = UUID.randomUUID();
    subscribe(a, u1);
    subscribe(a, u2);
    subscribe(a, u3);
    subscribe(b, u1);
    subscribe(b, u2);
    subscribe(c, u1);

    MvcResult page1 = mockMvc.perform(get("/api/interests")
            .param("sortBy", "subscriberCount")
            .param("direction", "desc")
            .param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.hasNext").value(true))
        .andReturn();

    JsonNode body1 = objectMapper.readTree(page1.getResponse().getContentAsString());
    String cursor = body1.get("nextCursor").asText();
    assertThat(body1.get("content").get(0).get("id").asText()).isEqualTo(a.toString());
    assertThat(body1.get("content").get(1).get("id").asText()).isEqualTo(b.toString());

    mockMvc.perform(get("/api/interests")
            .param("sortBy", "subscriberCount")
            .param("direction", "desc")
            .param("size", "2")
            .param("cursor", cursor))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].id").value(c.toString()))
        .andExpect(jsonPath("$.hasNext").value(false));
  }

  @Test
  @DisplayName("잘못된 sortBy 값은 400 INVALID_REQUEST로 매핑된다")
  void invalidSortByReturns400() throws Exception {
    mockMvc.perform(get("/api/interests").param("sortBy", "bogus"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
  }

  @Test
  @DisplayName("같은 사용자 중복 구독은 409 DUPLICATE_SUBSCRIPTION")
  void duplicateSubscriptionReturns409() throws Exception {
    UUID interestId = createAndGetId("구독대상", List.of("k"));
    UUID userId = UUID.randomUUID();

    mockMvc.perform(post("/api/interests/{id}/subscriptions", interestId)
            .header("MoNew-Request-User-ID", userId.toString()))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/interests/{id}/subscriptions", interestId)
            .header("MoNew-Request-User-ID", userId.toString()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("DUPLICATE_SUBSCRIPTION"));
  }

  @Test
  @DisplayName("존재하지 않는 관심사 조회/수정/삭제/구독은 404 INTEREST_NOT_FOUND")
  void notFoundCases() throws Exception {
    UUID missing = UUID.randomUUID();

    mockMvc.perform(patch("/api/interests/{id}", missing)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "keywords", List.of("k")))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("INTEREST_NOT_FOUND"));

    mockMvc.perform(delete("/api/interests/{id}", missing))
        .andExpect(status().isNotFound());

    mockMvc.perform(post("/api/interests/{id}/subscriptions", missing)
            .header("MoNew-Request-User-ID", UUID.randomUUID().toString()))
        .andExpect(status().isNotFound());
  }

  private UUID createAndGetId(String name, List<String> keywords) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/interests")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "name", name,
                "keywords", keywords))))
        .andExpect(status().isCreated())
        .andReturn();
    return UUID.fromString(
        objectMapper.readTree(result.getResponse().getContentAsString())
            .get("id").asText());
  }

  private void subscribe(UUID interestId, UUID userId) throws Exception {
    mockMvc.perform(post("/api/interests/{id}/subscriptions", interestId)
            .header("MoNew-Request-User-ID", userId.toString()))
        .andExpect(status().isCreated());
  }
}
