package com.example.monew.config;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TimeConfigTest {

  @Test
  @DisplayName("애플리케이션의 기본 타임존이 Asia/Seoul인지 확인한다")
  void 확인_애플리케이션_타임존() {
    // 1. 현재 JVM의 기본 타임존 ID 추출
    String currentZoneId = TimeZone.getDefault().getID();
    System.out.println("=== 검증 결과 ===");
    System.out.println("현재 타임존: " + currentZoneId);
    System.out.println("현재 시간: " + LocalDateTime.now());
    System.out.println("================");

    // 2. Asia/Seoul로 설정되어 있는지 검증
    assertThat(currentZoneId).isEqualTo("Asia/Seoul");
  }
}