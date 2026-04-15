package com.example.monew.global.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(
    classes = {
        GlobalExceptionHandlerTest.TestApp.class,
        GlobalExceptionHandler.class,
        GlobalExceptionHandlerTest.DummyController.class
    },
    properties = {
        "spring.mvc.throw-exception-if-no-handler-found=true",
        "spring.web.resources.add-mappings=false",
        "spring.autoconfigure.exclude=" +
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration," +
            "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration," +
            "org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration"
    }
)
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest {

  @EnableAutoConfiguration
  static class TestApp {}

  @Autowired
  MockMvc mockMvc;

  @Autowired
  ObjectMapper objectMapper;

  @Test
  void monewException_응답_일관성() throws Exception {
    mockMvc.perform(get("/_test/monew"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message").value("해당 유저를 찾을 수 없습니다."));
  }

  @Test
  void methodArgumentNotValid_400_필드_오류() throws Exception {
    DummyRequest body = new DummyRequest();
    body.name = "";
    mockMvc.perform(post("/_test/valid")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.details.name").exists());
  }

  @Test
  void missingRequestHeader_400() throws Exception {
    mockMvc.perform(get("/_test/header"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MISSING_REQUEST_HEADER"))
        .andExpect(jsonPath("$.details.header").value("MoNew-Request-User-ID"));
  }

  @Test
  void methodNotAllowed_405() throws Exception {
    mockMvc.perform(post("/_test/monew"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED_REQUEST"));
  }

  @Test
  void messageNotReadable_400() throws Exception {
    mockMvc.perform(post("/_test/valid")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{broken json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST_BODY"));
  }

  @Test
  void noHandler_404() throws Exception {
    mockMvc.perform(get("/_test/does-not-exist"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void illegalArgument_400() throws Exception {
    mockMvc.perform(get("/_test/illegal"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
  }

  @Test
  void genericException_500() throws Exception {
    mockMvc.perform(get("/_test/boom"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
  }

  static class DummyRequest {
    @NotBlank
    public String name;
  }

  @RestController
  @RequestMapping("/_test")
  static class DummyController {

    @GetMapping("/monew")
    public void monew() {
      throw new TestUserNotFound();
    }

    @PostMapping(value = "/valid", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void valid(@Valid @RequestBody DummyRequest req) {
    }

    @GetMapping("/header")
    public void header(@RequestHeader("MoNew-Request-User-ID") String userId) {
    }

    @GetMapping("/illegal")
    public void illegal() {
      throw new IllegalArgumentException("잘못된 값");
    }

    @GetMapping("/boom")
    public void boom() {
      throw new RuntimeException("의도된 폭발");
    }
  }

  static class TestUserNotFound extends MonewException {
    TestUserNotFound() {
      super(ErrorCode.USER_NOT_FOUND, Map.of());
    }
  }
}
