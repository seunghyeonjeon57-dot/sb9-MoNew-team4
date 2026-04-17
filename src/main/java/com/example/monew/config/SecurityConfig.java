package com.example.monew.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            // 1. 팀원들이 만든 기존 API는 배포 서버에서도 누구나 접근 가능하게 '화이트리스트' 등록
            .requestMatchers("/api/articles/**", "/api/comments/**", "/api/interests/**","/api/notification/**")
            .permitAll()

            // 2. 승현님이 만든 유저 관련 API (로그인, 회원가입 등)도 일단은 열어둠
            .requestMatchers("/api/users/**").permitAll()

            // 3. 나중에 정말로 권한이 필요한 API가 생기면 그때 하나씩 잠금
            .anyRequest().authenticated()
        )
        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable());

    return http.build();
  }
}