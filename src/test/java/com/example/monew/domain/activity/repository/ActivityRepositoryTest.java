package com.example.monew.domain.activity.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.monew.domain.activity.document.UserActivityDocument;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

@DataMongoTest
public class ActivityRepositoryTest {

  @Autowired
  private UserActivityRepository userActivityRepository;

  @Test
  @DisplayName("유저 ID로 활동 내역을 삭제하면, 해당 유저의 데이터만 정확히 삭제된다.")
  void deleteAllByUserId_DeletesCorrectData() {
    userActivityRepository.deleteAll(); // 테스트 시작 전에 비우고 시작

    UUID userA = UUID.randomUUID();
    UUID userB = UUID.randomUUID();

    userActivityRepository.insert(UserActivityDocument.builder().userId(userA).build());
    userActivityRepository.insert(UserActivityDocument.builder().userId(userB).build());

    userActivityRepository.findAll().forEach(doc -> System.out.println("ID: " + doc.getUserId()));

    assertThat(userActivityRepository.findById(userA)).isPresent();

    userActivityRepository.deleteAllByUserId(userA);
    assertThat(userActivityRepository.findById(userA)).isEmpty();
  }

}
