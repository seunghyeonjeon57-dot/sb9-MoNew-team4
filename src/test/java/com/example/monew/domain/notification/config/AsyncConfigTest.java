package com.example.monew.domain.notification.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.monew.config.AsyncConfig;
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

