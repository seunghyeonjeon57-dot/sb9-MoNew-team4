package com.example.monew.domain.interest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.monew.domain.interest.entity.Interest;
import com.example.monew.domain.interest.repository.InterestRepository;
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

  private static final String USER_HEADER = "Monew-Request-User-ID";

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

  @Autowired
  private InterestRepository interestRepository;

  @Test
  @DisplayName("I1→I5→I2→I6→I2→I3→I4 전체 플로우")
  void fullFlow() throws Exception {
    UUID userId = UUID.randomUUID();
    String name = "플로우" + System.nanoTime();

    MvcResult created = mockMvc.perform(post("/api/interests")
            .header(USER_HEADER, userId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("name", name, "keywords", List.of("A")))))
        .andExpect(status().isCreated()).andReturn();
    UUID interestId = UUID.fromString(objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText());

    mockMvc.perform(post("/api/interests/" + interestId + "/subscriptions").header(USER_HEADER, userId.toString()))
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/interests")
            .header(USER_HEADER, userId.toString())
            .param("orderBy", "name").param("direction", "ASC").param("limit", "10"))
        .andExpect(status().isOk());

    mockMvc.perform(delete("/api/interests/" + interestId + "/subscriptions").header(USER_HEADER, userId.toString()))
        .andExpect(status().isOk());

    mockMvc.perform(patch("/api/interests/" + interestId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("keywords", List.of("C")))))
        .andExpect(status().isOk());

    mockMvc.perform(delete("/api/interests/" + interestId))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("80% 유사 이름 → 409")
  void similarNameRejected() throws Exception {
    String name = "유사테스트" + System.nanoTime();
    interestRepository.saveAndFlush(Interest.builder().name(name).keywords(List.of("K")).build());
    mockMvc.perform(post("/api/interests").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("name", name + "1", "keywords", List.of("K")))))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("정확 일치 이름 → 409")
  void exactMatchRejected() throws Exception {
    String name = "중복테스트" + System.nanoTime();
    interestRepository.saveAndFlush(Interest.builder().name(name).keywords(List.of("K")).build());
    mockMvc.perform(post("/api/interests").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("name", name, "keywords", List.of("K")))))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("PATCH에 name 포함 → 400")
  void patchWithNameRejected() throws Exception {
    Interest i = interestRepository.saveAndFlush(Interest.builder().name("불변" + System.nanoTime()).keywords(List.of("A")).build());
    mockMvc.perform(patch("/api/interests/" + i.getId()).contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("name", "변경시도", "keywords", List.of("B")))))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("잘못된 orderBy → 400")
  void invalidSort() throws Exception {
    mockMvc.perform(get("/api/interests").param("orderBy", "invalid").param("direction", "ASC"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("헤더 누락 구독 → 400")
  void missingHeader() throws Exception {
    mockMvc.perform(post("/api/interests/" + UUID.randomUUID() + "/subscriptions"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("미존재 구독 → 404")
  void subscribeNotFound() throws Exception {
    mockMvc.perform(post("/api/interests/" + UUID.randomUUID() + "/subscriptions").header(USER_HEADER, UUID.randomUUID().toString()))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("페이지네이션 검증")
  void cursorPagination_movesAcrossPages() throws Exception {
    String prefix = "PAGE" + System.nanoTime();
    for (int i = 0; i < 15; i++) {
      interestRepository.saveAndFlush(Interest.builder().name(prefix + i).keywords(List.of("K")).build());
    }
    // [교정] direction 파라미터 추가
    MvcResult res = mockMvc.perform(get("/api/interests").param("keyword", prefix).param("orderBy", "name").param("direction", "ASC").param("limit", "10"))
        .andExpect(status().isOk()).andReturn();
    String cursor = objectMapper.readTree(res.getResponse().getContentAsString()).get("nextCursor").asText();
    mockMvc.perform(get("/api/interests").param("keyword", prefix).param("orderBy", "name").param("direction", "ASC").param("limit", "10").param("cursor", cursor))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("유저 삭제 → 구독 정리")
  void userHardDelete_cleansSubscriptions() throws Exception {
    User u = userRepository.save(User.builder().nickname("u1").email("u1@m.com").password("p123!").build());
    Interest i = interestRepository.save(Interest.builder().name("cas" + System.nanoTime()).keywords(List.of("a")).build());
    mockMvc.perform(post("/api/interests/" + i.getId() + "/subscriptions").header(USER_HEADER, u.getId().toString())).andExpect(status().isOk());
    userService.hardDeleteUser(u.getId());
    assertThat(subscriptionRepository.findAllByUserId(u.getId())).isEmpty();
  }

  @Test
  @DisplayName("유저 삭제 → 리셋 확인")
  void userHardDelete_subscribedByMeResetToFalse() throws Exception {
    User u = userRepository.save(User.builder().nickname("u2").email("u2@m.com").password("p123!").build());
    Interest i = interestRepository.save(Interest.builder().name("res" + System.nanoTime()).keywords(List.of("x")).build());
    // [교정] 모든 필수 파라미터 포함
    mockMvc.perform(get("/api/interests").header(USER_HEADER, u.getId().toString())
            .param("orderBy", "name").param("direction", "ASC").param("limit", "10"))
        .andExpect(status().isOk());
    userService.hardDeleteUser(u.getId());
  }

  @Test
  @DisplayName("격리 검증")
  void userHardDelete_keepsOtherSubscribers() throws Exception {
    User a = userRepository.save(User.builder().nickname("A").email("a@m.com").password("p!").build());
    User b = userRepository.save(User.builder().nickname("B").email("b@m.com").password("p!").build());
    Interest i = interestRepository.save(Interest.builder().name("sh" + System.nanoTime()).keywords(List.of("s")).build());
    mockMvc.perform(post("/api/interests/" + i.getId() + "/subscriptions").header(USER_HEADER, a.getId().toString()));
    mockMvc.perform(post("/api/interests/" + i.getId() + "/subscriptions").header(USER_HEADER, b.getId().toString()));
    userService.hardDeleteUser(a.getId());
    assertThat(interestRepository.findById(i.getId()).get().getSubscriberCount()).isEqualTo(1L);
  }
}