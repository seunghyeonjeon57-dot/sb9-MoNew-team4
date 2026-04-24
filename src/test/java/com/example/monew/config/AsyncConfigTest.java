package com.example.monew.config;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AsyncConfigTest {
  @Autowired
  private AsyncConfig asyncConfig;

  @Test
  void asyncConfigContextLoads() {
    assertThat(asyncConfig).isNotNull();
  }
}

