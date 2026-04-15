package com.example.monew;

import com.example.monew.domain.user.service.S3TestService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MoNewApplication {
  public static void main(String[] args) {
    ConfigurableApplicationContext context = SpringApplication.run(MoNewApplication.class, args);


  }
}
