package com.example.monew.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ActuatorExposureTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  @DisplayName("actuator health 엔드포인트가 노출되고 status UP을 반환한다")
  void healthEndpointExposedWithUpStatus() throws Exception {
    mockMvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.containsString("\"status\":\"UP\"")));
  }

  @Test
  @DisplayName("actuator info 엔드포인트가 노출된다")
  void infoEndpointExposed() throws Exception {
    mockMvc.perform(get("/actuator/info"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("actuator prometheus 엔드포인트가 JVM 표준 메트릭을 노출한다")
  void prometheusEndpointExposesJvmMetrics() throws Exception {
    mockMvc.perform(get("/actuator/prometheus"))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.containsString("jvm_memory_used_bytes")));
  }
}
