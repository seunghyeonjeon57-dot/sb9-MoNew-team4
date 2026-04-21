package com.example.monew.domain.user.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.monew.domain.comment.repository.CommentLikeRepository;
import com.example.monew.domain.comment.repository.CommentRepository;
import com.example.monew.domain.interest.repository.InterestRepository; 
import com.example.monew.domain.interest.repository.SubscriptionRepository;
import com.example.monew.domain.notification.repository.NotificationRepository;
import com.example.monew.domain.user.entity.User;
import com.example.monew.domain.user.entity.type.UserStatus;
import com.example.monew.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
class UserPurgeBatchConfigTest {

  @Autowired(required = false)
  private JobLauncherTestUtils jobLauncherTestUtils;

  @Autowired
  @Qualifier("userPurgeJob")
  private Job userPurgeJob;

  @Autowired private UserRepository userRepository;
  @Autowired private CommentRepository commentRepository;
  @Autowired private CommentLikeRepository commentLikeRepository;
  @Autowired private NotificationRepository notificationRepository;
  @Autowired private SubscriptionRepository subscriptionRepository;
  @Autowired private InterestRepository interestRepository; 

  @BeforeEach
  void setUp() {
    if (jobLauncherTestUtils != null) {
      jobLauncherTestUtils.setJob(userPurgeJob);
    }

    commentLikeRepository.deleteAll();
    notificationRepository.deleteAll();
    subscriptionRepository.deleteAll();
    commentRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("탈퇴 후 1일이 경과한 유저와 연관 데이터가 물리 삭제되어야 한다.")
  void 탈퇴_1일_경과_유저_및_연관데이터_물리삭제_테스트() throws Exception {
    
    User targetUser = createDeletedUser("delete_me@test.com", "targetUser");
    UUID targetUserId = targetUser.getId();
    ReflectionTestUtils.setField(targetUser, "deletedAt", LocalDateTime.now().minusDays(2));
    userRepository.saveAndFlush(targetUser);

    User safeUser = createDeletedUser("save_me@test.com", "safeUser");

    
    JobParameters jobParameters = new JobParametersBuilder()
        .addString("requestDate", LocalDateTime.now().toString())
        .addLong("time", System.currentTimeMillis())
        .toJobParameters();

    JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

    
    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    assertThat(userRepository.existsById(targetUserId)).isFalse();
    assertThat(userRepository.existsById(safeUser.getId())).isTrue();
  }

  @Test
  @DisplayName("대량의 탈퇴 유저(101건 이상) 처리 시 오프셋 밀림 없이 전원 삭제되어야 한다.")
  void 대용량_데이터_삭제_누락_방지_테스트() throws Exception {
    
    int targetCount = 105;
    for (int i = 0; i < targetCount; i++) {
      User user = createDeletedUser("bulk_" + i + "@test.com", "bulk_" + i);
      ReflectionTestUtils.setField(user, "deletedAt", LocalDateTime.now().minusDays(2));
      userRepository.save(user);
    }
    userRepository.flush();

    
    JobParameters jobParameters = new JobParametersBuilder()
        .addString("requestDate", LocalDateTime.now().toString())
        .addLong("time", System.currentTimeMillis())
        .toJobParameters();

    JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

    
    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    
    assertThat(userRepository.countByStatus(UserStatus.DELETED)).isEqualTo(0);
  }

  private User createDeletedUser(String email, String nickname) {
    User user = User.builder()
        .email(email)
        .nickname(nickname)
        .password("password123")
        .status(UserStatus.DELETED)
        .build();
    user.withdraw();
    return userRepository.saveAndFlush(user);
  }
}