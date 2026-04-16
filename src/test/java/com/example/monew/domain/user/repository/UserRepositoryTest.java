package com.example.monew.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.example.monew.domain.user.entity.User;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {
  @Autowired UserRepository userRepository;

  @Test
  @DisplayName("이메일로 유저가 존재하는지 확인한다")
  void existsByEmail(){
    User user = User.builder().email("seung@naver.com")
        .nickname("seung").password("password1234").build();
    userRepository.save(user);
    boolean exists = userRepository.existsByEmail("seung@naver.com");
    assertThat(exists).isTrue();
  }
  @Test
  @DisplayName("논리 삭제된 유저는 조회되지 않아야합니다")
  void softDeleteTest(){
    User user = User.builder().nickname("seung").email("seung@naver.com").password("seung12345")
    .build();

    User savedUser = userRepository.save(user);

    userRepository.delete(savedUser);
    userRepository.flush();

    Optional<User> deleteUser = userRepository.findByEmail("seung@naver.com");
    assertThat(deleteUser).isEmpty();
  }

}