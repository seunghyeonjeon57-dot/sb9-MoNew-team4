package com.example.monew;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
public class MoNewApplication {
  public static void main(String[] args) {
    SpringApplication.run(MoNewApplication.class, args);
  }
}
