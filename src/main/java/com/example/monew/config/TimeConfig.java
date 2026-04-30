package com.example.monew.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import java.util.TimeZone;

@Configuration
public class TimeConfig {

  @PostConstruct
  public void init() {
    
    
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));

    
    System.out.println("KST TimeZone 정착 완료: " + java.time.LocalDateTime.now());
  }
}