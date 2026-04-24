package com.example.monew.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.monew.config.QueryDslTestConfig;
import com.example.monew.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslTestConfig.class)
class UserRepositoryTest {

  @Autowired
  private UserRepository userRepository;

  @Test
  @DisplayName("이메일 중복 체크: 탈퇴 여부와 상관없이 이메일 존재 여부를 확인한다")
  void existsByEmail() {
    
    User user = User.builder().email("test@naver.com")
        .nickname("tester").password("password1234").build();
    userRepository.save(user);

    
    boolean exists = userRepository.existsByEmail("test@naver.com");

    
    assertThat(exists).isTrue();
  }

  @Test
  @DisplayName("Active 조회 테스트: 논리 삭제된 유저는 findActiveByEmail로 조회되지 않아야 한다")
  void findActiveByEmailTest() {
    
    User user = User.builder().email("active@naver.com")
        .nickname("active").password("password1234").build();
    userRepository.save(user);

    User deletedUser = User.builder().email("deleted@naver.com")
        .nickname("deleted").password("password1234").build();
    userRepository.save(deletedUser);

    
    deletedUser.withdraw();
    userRepository.saveAndFlush(deletedUser);

    
    Optional<User> activeResult = userRepository.findActiveByEmail("active@naver.com");
    Optional<User> deletedResult = userRepository.findActiveByEmail("deleted@naver.com");

    
    assertThat(activeResult).isPresent();
    assertThat(deletedResult).isEmpty(); 
  }

  @Test
  @DisplayName("배치용 조회 테스트: 삭제된 유저만 정확히 찾아오는지 확인한다")
  void findExpiredDeletedUsersTest() {
    
    User deletedUser = User.builder().email("expired@naver.com")
        .nickname("expired").password("password1234").build();
    deletedUser.withdraw();
    userRepository.saveAndFlush(deletedUser);

    
    
    List<User> expiredUsers = userRepository.findExpiredDeletedUsers(LocalDateTime.now().plusMinutes(1));

    
    assertThat(expiredUsers).hasSize(1);
    assertThat(expiredUsers.get(0).getEmail()).isEqualTo("expired@naver.com");
  }
  @Test
  @DisplayName("ID로 활성 유저 조회 테스트")
  void findActiveByIdTest() {
    
    User user = User.builder().email("id-test@naver.com")
        .nickname("id-tester").password("password").build();
    User savedUser = userRepository.save(user);

    
    Optional<User> result = userRepository.findActiveById(savedUser.getId());

    
    assertThat(result).isPresent();
  }
}