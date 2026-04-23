package com.example.monew.domain.article.articleconfig;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.context.ApplicationContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class NewsConfigTest {

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  @DisplayName("설정 정상 작동 확인")
  void newsConfigLoadTest() {
    Object configBean = applicationContext.getBean(NewsConfig.class);
    assertThat(configBean).isNotNull();
  }
}