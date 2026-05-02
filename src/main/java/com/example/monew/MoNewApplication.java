package com.example.monew;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MoNewApplication {
  public static void main(String[] args) {
    SpringApplication.run(MoNewApplication.class, args);
  }
}
