package com.example.monew.domain.user.batch;

import static org.junit.jupiter.api.Assertions.*;

import com.example.monew.domain.comment.entity.CommentEntity;
import com.example.monew.domain.comment.entity.CommentLikeEntity; 
import com.example.monew.domain.comment.repository.CommentLikeRepository; 
import com.example.monew.domain.comment.repository.CommentRepository;
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
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
class UserPurgeBatchConfigTest {

  @Autowired(required = false)
  private JobLauncherTestUtils jobLauncherTestUtils;

  @Autowired
  private Job userPurgeJob;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private CommentRepository commentRepository;

  @Autowired
  private CommentLikeRepository commentLikeRepository; 

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    if (jobLauncherTestUtils != null) {
      jobLauncherTestUtils.setJob(userPurgeJob);
    }
  }

  @Test
  @DisplayName("탈퇴 후 1일이 경과한 유저와 연관 데이터가 물리 삭제되어야 한다.")
  void 탈퇴_1일_경과_유저_및_연관데이터_물리삭제_테스트() throws Exception {
    
    User targetUser = createDeletedUser("delete_me@test.com", "targetUser");
    UUID targetUserId = targetUser.getId();

    
    UUID mockArticleId = UUID.randomUUID();
    commentRepository.save(new CommentEntity(mockArticleId, targetUserId, "삭제될 댓글"));

    
    commentLikeRepository.save(new CommentLikeEntity(UUID.randomUUID(), targetUserId));

    
    jdbcTemplate.update("UPDATE users SET deleted_at = ? WHERE id = ?",
        LocalDateTime.now().minusDays(2), targetUserId);

    
    User safeDeletedUser = createDeletedUser("save_me@test.com", "safeDeletedUser");
    User activeUser = userRepository.save(User.builder()
        .nickname("activeUser")
        .email("active@test.com")
        .password("password123")
        .status(UserStatus.ACTIVE)
        .build());

    
    JobExecution jobExecution = jobLauncherTestUtils.launchJob();

    
    assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());

    
    Integer userExists = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, targetUserId);
    assertEquals(0, userExists);

    
    Integer commentExists = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM comments WHERE user_id = ?", Integer.class, targetUserId);
    assertEquals(0, commentExists);

    
    Integer likesExists = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM comment_likes WHERE user_id = ?", Integer.class, targetUserId);
    assertEquals(0, likesExists);

    assertTrue(userRepository.existsById(safeDeletedUser.getId()));
    assertTrue(userRepository.existsById(activeUser.getId()));
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