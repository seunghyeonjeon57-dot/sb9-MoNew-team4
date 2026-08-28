package com.example.monew.domain.user.service;

import com.example.monew.domain.activity.service.ActivityService;
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
import com.example.monew.domain.user.exception.DuplicateNickNameException;
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
  private final ActivityService activityService;


  @Transactional
  public void create(UserRegisterRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      log.warn("회원가입 실패: 이미 존재하는 이메일 -> {}", request.email());
      throw new DuplicateEmailException("이미 존재하는 이메일입니다.");
    }
    if(userRepository.existsByNickname(request.nickname())){
      log.warn("회원가입 실패: 이미 존재하는 닉네임 -> {}", request.nickname());
      throw new DuplicateNickNameException("이미 존재하는 닉네임입니다.");
    }
    User user = userMapper.toEntity(request);
    user.updatePassword(passwordEncoder.encode(request.password()));

    userRepository.save(user);

    UserDto userDto = new UserDto(user.getId(), user.getEmail(), user.getNickname(), user.getCreatedAt());
    activityService.updateUser(user.getId(), userDto);
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

    String newNickname = request.nickname();
    String oldNickname = user.getNickname();


    if (!oldNickname.equals(newNickname)) {
      
      if (userRepository.existsByNickname(newNickname)) {
        log.error("유저 수정 실패: 이미 존재하는 닉네임 -> {}", newNickname);
        throw new DuplicateNickNameException("이미 존재하는 닉네임입니다.");
      }
      user.updateNickname(newNickname);
    }

    log.info("유저 정보 수정 완료: ID={}, Nickname={} -> {}", id, oldNickname, user.getNickname());
    return userMapper.toDto(user);
  }


  @Transactional
  public void softDeleteUser(UUID id) {

    User user = userRepository.findActiveById(id)
        .orElseThrow(() -> new UserNotFoundException("이미 탈퇴했거나 존재하지 않는 유저입니다."));

    user.withdraw();
    activityService.softDeleteUserActivity(id);
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
    activityService.deleteUserActivity(userId);

    log.info("유저 및 연관 데이터 전체 물리 삭제 완료: ID={}", userId);
  }

  /**
   * 유저 물리삭제 배치용 벌크 버전.
   * hardDeleteUser()를 chunk 크기만큼 반복 호출하던 것을, chunk 전체를 IN절 기반
   * 벌크 쿼리 한 번씩으로 처리하도록 바꾼 것 (테이블당 1쿼리, 유저 수와 무관).
   */
  @Transactional
  public void hardDeleteUsers(List<UUID> userIds) {
    if (userIds.isEmpty()) {
      return;
    }

    List<UUID> interestIds = subscriptionRepository.findInterestIdsByUserIdIn(userIds);

    log.info("유저 일괄 하드 삭제 시작: {}명, 관련 관심사 개수={}", userIds.size(), interestIds.size());

    commentLikeRepository.deleteAllByUserIdIn(userIds);
    notificationRepository.deleteAllByUserIdIn(userIds);

    if (!interestIds.isEmpty()) {
      interestRepository.decrementSubscriberCountAll(interestIds);
    }

    subscriptionRepository.deleteAllByUserIdIn(userIds);
    commentRepository.deleteAllByUserIdIn(userIds);
    userRepository.deleteAllByIdInBatch(userIds);
    activityService.deleteUserActivities(userIds);

    log.info("유저 및 연관 데이터 일괄 물리 삭제 완료: {}명", userIds.size());
  }
}