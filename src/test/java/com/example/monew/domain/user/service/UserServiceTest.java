package com.example.monew.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.monew.domain.comment.repository.CommentLikeRepository;
import com.example.monew.domain.comment.repository.CommentRepository;
import com.example.monew.domain.interest.repository.InterestRepository;
import com.example.monew.domain.interest.repository.SubscriptionRepository;
import com.example.monew.domain.notification.repository.NotificationRepository;
import com.example.monew.domain.user.dto.UserDto;
import com.example.monew.domain.user.dto.request.UserLoginRequest;
import com.example.monew.domain.user.dto.request.UserRegisterRequest;
import com.example.monew.domain.user.dto.request.UserUpdateRequest;
import com.example.monew.domain.user.entity.User;
import com.example.monew.domain.user.entity.type.UserStatus;
import com.example.monew.domain.user.exception.DuplicateEmailException;
import com.example.monew.domain.user.exception.LoginFailedException;
import com.example.monew.domain.user.exception.UserNotFoundException;
import com.example.monew.domain.user.mapper.UserMapper;
import com.example.monew.domain.user.repository.UserRepository;
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

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private UserMapper userMapper;
  @Mock private SubscriptionRepository subscriptionRepository;
  @Mock private NotificationRepository notificationRepository;
  @Mock private CommentLikeRepository commentLikeRepository;
  @Mock private CommentRepository commentRepository;
  @Mock private InterestRepository interestRepository;

  @InjectMocks private UserService userService;

  @Nested
  @DisplayName("회원 가입 테스트")
  class CreateUser {
    @Test
    @DisplayName("새로운 이메일로 가입 성공")
    void success() {
      UserRegisterRequest request = new UserRegisterRequest("new@email.com", "테스트", "new12345");
      User user = User.builder().email(request.email()).nickname(request.nickname()).build();

      when(userRepository.existsByEmail(request.email())).thenReturn(false);
      when(userMapper.toEntity(any(UserRegisterRequest.class))).thenReturn(user);
      when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");

      userService.create(request);

      verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("중복된 이메일 가입 실패")
    void existed_email_fail() {
      UserRegisterRequest request = new UserRegisterRequest("new@email.com", "테스트", "new12345");
      when(userRepository.existsByEmail(request.email())).thenReturn(true);

      assertThatThrownBy(() -> userService.create(request))
          .isInstanceOf(DuplicateEmailException.class);
    }
  }

  @Nested
  @DisplayName("로그인 테스트")
  class Login {
    @Test
    @DisplayName("로그인 성공")
    void login_success() {
      UserLoginRequest request = new UserLoginRequest("test@test.com", "test1234");
      User user = User.builder().email("test@test.com").password("encoded_pw").build();
      UUID id = UUID.randomUUID();

      
      when(userRepository.findActiveByEmail(request.email())).thenReturn(Optional.of(user));
      when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(true);
      when(userMapper.toDto(any())).thenReturn(new UserDto(id, "test@test.com", "테스트유저", null));

      UserDto result = userService.login(request);

      assertThat(result.nickname()).isEqualTo("테스트유저");
      verify(userRepository).findActiveByEmail(request.email());
    }

    @Test
    @DisplayName("탈퇴한 유저 혹은 없는 이메일 로그인 실패")
    void login_fail_notFound() {
      UserLoginRequest request = new UserLoginRequest("wrong@test.com", "password123");
      
      when(userRepository.findActiveByEmail(anyString())).thenReturn(Optional.empty());

      assertThatThrownBy(() -> userService.login(request))
          .isInstanceOf(LoginFailedException.class);
    }
  }

  @Nested
  @DisplayName("유저 수정 테스트")
  class UpdateUser {
    @Test
    @DisplayName("유저 수정 성공")
    void success_update() {
      UUID userId = UUID.randomUUID();
      User user = User.builder().nickname("기존").email("test@test.com").build();
      UserUpdateRequest request = new UserUpdateRequest("새닉네임");

      
      when(userRepository.findActiveById(userId)).thenReturn(Optional.of(user));
      when(userMapper.toDto(user)).thenReturn(new UserDto(userId, "test@test.com", "새닉네임", null));

      UserDto result = userService.updateUser(userId, request);

      assertThat(user.getNickname()).isEqualTo("새닉네임");
      verify(userRepository).findActiveById(userId);
    }
  }

  @Nested
  @DisplayName("유저 삭제 테스트")
  class DeleteUser {
    @Test
    @DisplayName("소프트 삭제 성공")
    void success_soft_delete() {
      UUID userId = UUID.randomUUID();
      User user = User.builder().status(UserStatus.ACTIVE).build();

      
      when(userRepository.findActiveById(userId)).thenReturn(Optional.of(user));

      userService.softDeleteUser(userId);

      assertThat(user.getDeletedAt()).isNotNull();
      verify(userRepository).findActiveById(userId);
    }

    @Test
    @DisplayName("소프트 삭제 실패 - 이미 탈퇴했거나 없는 유저")
    void soft_delete_fail() {
      UUID userId = UUID.randomUUID();
      when(userRepository.findActiveById(userId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> userService.softDeleteUser(userId))
          .isInstanceOf(UserNotFoundException.class)
          .hasMessageContaining("해당 유저를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("하드 삭제 성공")
    void success_hard_delete() {
      UUID userId = UUID.randomUUID();
      when(userRepository.existsById(userId)).thenReturn(true);

      userService.hardDeleteUser(userId);

      verify(commentLikeRepository).deleteAllByUserId(userId);
      verify(userRepository).deleteById(userId);
    }
  }
}