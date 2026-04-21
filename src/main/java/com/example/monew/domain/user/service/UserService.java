package com.example.monew.domain.user.service;

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
import com.example.monew.domain.user.exception.DuplicateEmailException;
import com.example.monew.domain.user.exception.LoginFailedException;
import com.example.monew.domain.user.exception.UserNotFoundException;
import com.example.monew.domain.user.mapper.UserMapper;
import com.example.monew.domain.user.repository.UserRepository;
import java.util.List;
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
  private final CommentLikeRepository commentLikeRepository;
  private final NotificationRepository notificationRepository;
  private final InterestRepository interestRepository;


  @Transactional
  public void create(UserRegisterRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      log.warn("회원가입 실패: 이미 존재하는 이메일 -> {}", request.email());
      throw new DuplicateEmailException("이미 존재하는 이메일입니다.");
    }
    User user = userMapper.toEntity(request);
    user.updatePassword(passwordEncoder.encode(request.password()));

    userRepository.save(user);
    log.info("새로운 유저 가입 완료: ID={}, Email={}", user.getId(), user.getEmail());
  }


  @Transactional(readOnly = true)
  public UserDto login(UserLoginRequest request) {

    User user = userRepository.findActiveByEmail(request.email())
        .orElseThrow(() -> {
          log.warn("로그인 실패: 존재하지 않거나 탈퇴한 이메일 -> {}", request.email());
          return new LoginFailedException("이메일 또는 비밀번호가 잘못되었습니다.");
        });

    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      log.warn("로그인 실패: 비밀번호 불일치 -> Email={}", request.email());
      throw new LoginFailedException("이메일 또는 비밀번호가 잘못되었습니다.");
    }

    log.info("유저 로그인 성공: ID={}", user.getId());
    return userMapper.toDto(user);
  }


  @Transactional
  public UserDto updateUser(UUID id, UserUpdateRequest request) {

    User user = userRepository.findActiveById(id).orElseThrow(() -> {
      log.error("유저 수정 실패: 존재하지 않거나 유효하지 않은 ID -> {}", id);
      return new UserNotFoundException("해당 유저를 찾을 수 없습니다.");
    });

    String oldNickname = user.getNickname();
    user.updateNickname(request.nickname());

    log.info("유저 정보 수정 완료: ID={}, Nickname={} -> {}", id, oldNickname, request.nickname());
    return userMapper.toDto(user);
  }


  @Transactional
  public void softDeleteUser(UUID id) {

    User user = userRepository.findActiveById(id)
        .orElseThrow(() -> new UserNotFoundException("이미 탈퇴했거나 존재하지 않는 유저입니다."));

    user.withdraw();
    log.info("유저 논리 삭제 완료 (ID={}): 탈퇴 시점={}", id, user.getDeletedAt());
  }


  @Transactional
  public void hardDeleteUser(UUID userId) {
    
    if (!userRepository.existsById(userId)) {
      throw new UserNotFoundException("삭제하려는 유저가 존재하지 않습니다.");
    }

    
    
    List<UUID> interestIds = subscriptionRepository.findInterestIdsByUserId(userId);

    log.info("유저 하드 삭제 시작: ID={}, 관련 관심사 개수={}", userId, interestIds.size());

    
    commentLikeRepository.deleteAllByUserId(userId);
    notificationRepository.deleteAllByUserId(userId);

    
    if (!interestIds.isEmpty()) {
      interestRepository.decrementSubscriberCountAll(interestIds);
    }

    
    subscriptionRepository.deleteAllByUserId(userId);
    commentRepository.deleteAllByUserId(userId);
    userRepository.deleteById(userId);

    log.info("유저 및 연관 데이터 전체 물리 삭제 완료: ID={}", userId);
  }
}