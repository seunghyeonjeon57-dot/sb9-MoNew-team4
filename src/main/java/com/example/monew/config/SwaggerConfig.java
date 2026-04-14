package com.example.monew.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

  @Bean
  public OpenAPI monewOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("MoNew API")
            .description("모뉴(MoNew) — 뉴스 통합 관리 플랫폼")
            .version("v1"));
  }
}
