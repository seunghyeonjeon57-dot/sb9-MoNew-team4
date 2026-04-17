package com.example.monew.domain.user.service;


import com.example.monew.domain.comment.repository.CommentRepository;
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
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final SubscriptionRepository subscriptionRepository;
  private final CommentRepository commentRepository;

  @Transactional
  public void create(UserRegisterRequest request) {
    if(userRepository.existsByEmail(request.email())){
      log.warn("회원가입 실패: 이미 존재하는 이메일 -> {}", request.email()); // 중복 가입 시도는 경고 수준
      throw new DuplicateEmailException("이미 존재하는 이메일입니다.");
    }
    User user = userMapper.toEntity(request);
    user.updatePassword(passwordEncoder.encode(request.password()));

    userRepository.save(user);
    log.info("새로운 유저 가입 완료: ID={}, Email={}", user.getId(), user.getEmail());
  }

  @Transactional(readOnly = true)
  public UserDto login(UserLoginRequest request){
    User user = userRepository.findByEmail(request.email())
        .orElseThrow(() -> {
          log.warn("로그인 실패: 존재하지 않는 이메일 -> {}", request.email());
          return new LoginFailedException("이메일 또는 비밀번호가 잘못되었습니다.");
        });

    if(!passwordEncoder.matches(request.password(), user.getPassword())){
      log.warn("로그인 실패: 비밀번호 불일치 -> Email={}", request.email());
      throw new LoginFailedException("이메일 또는 비밀번호가 잘못되었습니다.");
    }

    log.info("유저 로그인 성공: ID={}", user.getId());
    return userMapper.toDto(user);
  }

  @Transactional
  public UserDto updateUser(UUID id, UserUpdateRequest request){
    User user = userRepository.findById(id).orElseThrow(() -> {
      log.error("유저 수정 실패: 존재하지 않는 ID -> {}", id);
      return new UserNotFoundException("해당 유저를 찾을 수 없습니다.");
    });

    String oldNickname = user.getNickname();
    user.updateNickname(request.nickname());

    log.info("유저 정보 수정 완료: ID={}, Nickname={} -> {}", id, oldNickname, request.nickname());
    return userMapper.toDto(user);
  }

  @Transactional
  public void softDeleteUser(UUID id){
    User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("해당 유저를 찾을 수 없습니다."));
    userRepository.delete(user);
    log.info("유저 소프트 삭제 완료 (deleted_at 업데이트): ID={}", id);
  }

  @Transactional
  public void hardDeleteUser(UUID id){
    log.info("유저 하드 삭제 시작: ID={}", id);
    subscriptionRepository.deleteAllByUserId(id);
    commentRepository.deleteAllByUserId(id);
    userRepository.deleteById(id);
    log.info("유저 및 연관 데이터 전체 삭제 완료: ID={}", id);
  }
}
