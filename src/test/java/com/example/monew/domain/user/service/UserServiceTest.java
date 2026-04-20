package com.example.monew.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.monew.domain.comment.repository.CommentRepository;
import com.example.monew.domain.interest.entity.Subscription;
import com.example.monew.domain.interest.repository.InterestRepository;
import com.example.monew.domain.interest.repository.SubscriptionRepository;
import com.example.monew.domain.user.dto.UserDto;
import com.example.monew.domain.user.dto.request.UserLoginRequest;
import com.example.monew.domain.user.dto.request.UserRegisterRequest;
import com.example.monew.domain.user.dto.request.UserUpdateRequest;
import com.example.monew.domain.user.entity.User;
import com.example.monew.domain.user.exception.DuplicateEmailException;
import com.example.monew.domain.user.exception.LoginFailedException;
import com.example.monew.domain.user.exception.UserNotFoundException;
import com.example.monew.domain.user.mapper.UserMapper;
import com.example.monew.domain.user.repository.UserRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;


@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;
  @Mock
  private PasswordEncoder passwordEncoder;
  @Mock
  private UserMapper userMapper;
  @Mock
  private SubscriptionRepository subscriptionRepository;
  @Mock
  private CommentRepository commentRepository;
  @Mock
  private InterestRepository interestRepository;
  @InjectMocks
  private UserService userService;


  @Nested
  @DisplayName("회원 가입 테스트")
  class CreateUser{
    @Test
    @DisplayName("새로운 이메일로 가입 성공")
    void success(){
      UserRegisterRequest request = new UserRegisterRequest("new@email.com","테스트","new12345");
      User user = User.builder().email(request.email()).nickname(request.nickname()).password("encoded_password").build();
      when(userRepository.existsByEmail(user.getEmail())).thenReturn(false);
      when(userMapper.toEntity(any(UserRegisterRequest.class))).thenReturn(user);
      when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
      userService.create(request);
      verify(userRepository).save(any(User.class));
      verify(userRepository).existsByEmail(request.email());
    }
    @Test
    @DisplayName("중복된 이메일로 가입 시도시 가입 실패")
    void existed_email_fail(){
      UserRegisterRequest request = new UserRegisterRequest("new@email.com","테스트","new12345");
      User user = User.builder().email(request.email()).nickname(request.nickname()).password("encoded_password").build();
      when(userRepository.existsByEmail(user.getEmail())).thenReturn(true);
      assertThatThrownBy(() -> userService.create(request))
          .isInstanceOf(DuplicateEmailException.class);
    }
  }

  @Nested
  @DisplayName("로그인 테스트")
  class login {

    @Test
    @DisplayName("로그인 성공 테스트")
    void login_success() {
      UserLoginRequest request = new UserLoginRequest("test@test.com", "test1234");
      User user = User.builder().email("test@test.com").password("test1234").build();
      UUID id = UUID.randomUUID();

      when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
      when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
      when(userMapper.toDto(any())).thenReturn(new UserDto(id, "test.test.com", "테스트유저", null));
      UserDto result = userService.login(request);

      
      assertThat(result).isNotNull();
      assertThat(result.nickname()).isEqualTo("테스트유저");
      verify(userRepository).findByEmail(request.email());
    }

    @Test
    @DisplayName("비밀번호 불일치 시 로그인 실패")
    void login_fail_wrongPassword() {
      UserLoginRequest request = new UserLoginRequest("test@test.com", "wrong_pw");
      User user = User.builder().email("test@test.com").password("encoded_pw").build();

      when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
      when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

      assertThatThrownBy(() -> userService.login(request))
          .isInstanceOf(LoginFailedException.class);
    }
    @Test
    @DisplayName("이메일 불일치 시 로그인 실패")
    void login_fail_wrongEmail() {
      
      UserLoginRequest request = new UserLoginRequest("wrong@test.com", "password123");

      
      when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

      
      assertThatThrownBy(() -> userService.login(request))
          .isInstanceOf(LoginFailedException.class);
    }
  }

  @Nested
  @DisplayName("유저 수정 테스트")
  class updateUser{
    @Test
    @DisplayName("유저 수정 성공")
    void success_update(){
      UUID userId = UUID.randomUUID();
      User user = User.builder().nickname("테스트").email("test@naver.com").password("test1234").build();
      UserUpdateRequest request = new UserUpdateRequest("새닉네임");
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(userMapper.toDto(user)).thenReturn(new UserDto(userId,"test@naver.com","새닉네임",null));
      UserDto result = userService.updateUser(userId,request);

      assertThat(result.nickname()).isEqualTo("새닉네임");
      assertThat(user.getNickname()).isEqualTo("새닉네임");
      verify(userRepository).findById(userId);
    }
    @Test
    @DisplayName("유저 수정 실패 - 닉네임이 공백일 경우")
    void update_fail_blank_nickname() {
      
      UUID userId = UUID.randomUUID();
      User user = User.builder().nickname("기존").build();
      UserUpdateRequest request = new UserUpdateRequest("  "); 

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      
      
      assertThatThrownBy(() -> userService.updateUser(userId, request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("닉네임은 공백일 수 없습니다.");
    }
    @Test
    @DisplayName("유저 수정 실패 - 존재하지 않는 유저 수정 시도")
    void update_fail_nonExisted_id(){
      UUID userId = UUID.randomUUID();
      UserUpdateRequest request = new UserUpdateRequest("새");

      when(userRepository.findById(userId)).thenReturn(Optional.empty());



      assertThatThrownBy(() -> userService.updateUser(userId, request))
          .isInstanceOf(UserNotFoundException.class)
          .hasMessage("유저가 존재하지않습니다.");
    }

  }
  @Nested
  @DisplayName("유저 삭제 테스트")
  class DeleteUser {

    @Test
    @DisplayName("소프트 삭제 성공")
    void success_soft_delete() {
      
      UUID userId = UUID.randomUUID();
      User user = User.builder()
          .nickname("삭제유저")
          .email("delete@test.com")
          .build();

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      
      userService.softDeleteUser(userId);

      
      
      verify(userRepository).findById(userId);
      
      verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("하드 삭제 성공 - 구독이 없을 때 subscriberCount 벌크 감소는 호출되지 않음")
    void success_hard_delete() {
      UUID userId = UUID.randomUUID();
      when(subscriptionRepository.findAllByUserId(userId)).thenReturn(List.of());

      userService.hardDeleteUser(userId);

      verify(subscriptionRepository).findAllByUserId(userId);
      verify(subscriptionRepository).deleteAllByUserId(userId);
      verify(commentRepository).deleteAllByUserId(userId);
      verify(userRepository).deleteById(userId);
      verify(interestRepository, org.mockito.Mockito.never())
          .decrementSubscriberCountAll(org.mockito.ArgumentMatchers.anyCollection());
    }

    @Test
    @DisplayName("하드 삭제 성공 - 구독이 있을 때 관심사 subscriberCount 벌크 감소가 정확히 1회 호출")
    void success_hard_delete_decrementsSubscriberCount() {
      UUID userId = UUID.randomUUID();
      UUID interestAId = UUID.randomUUID();
      UUID interestBId = UUID.randomUUID();
      when(subscriptionRepository.findAllByUserId(userId)).thenReturn(List.of(
          new Subscription(interestAId, userId),
          new Subscription(interestBId, userId)
      ));

      userService.hardDeleteUser(userId);

      verify(subscriptionRepository).findAllByUserId(userId);
      verify(subscriptionRepository).deleteAllByUserId(userId);
      verify(commentRepository).deleteAllByUserId(userId);
      verify(userRepository).deleteById(userId);
      verify(interestRepository).decrementSubscriberCountAll(List.of(interestAId, interestBId));
    }

    @Test
    @DisplayName("소프트 삭제 실패 - 존재하지 않는 사용자")
    void soft_delete_fail_notFound() {
      
      UUID userId = UUID.randomUUID();
      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      
      assertThatThrownBy(() -> userService.softDeleteUser(userId))
          .isInstanceOf(UserNotFoundException.class)
          .hasMessage("존재하지 않는 사용자입니다.");

      
      verify(userRepository, org.mockito.Mockito.never()).delete(any());
    }
  }
}





