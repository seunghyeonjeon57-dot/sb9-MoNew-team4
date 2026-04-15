package com.example.monew;

import com.example.monew.domain.user.service.S3TestService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class MoNewApplication {
  public static void main(String[] args) {
    ConfigurableApplicationContext context = SpringApplication.run(MoNewApplication.class, args);

    // 실행되자마자 S3 테스트 호출
    S3TestService s3TestService = context.getBean(S3TestService.class);
    s3TestService.uploadTestFile();
  }
}
