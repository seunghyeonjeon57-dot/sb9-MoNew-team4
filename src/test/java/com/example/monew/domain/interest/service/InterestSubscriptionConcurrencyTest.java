package com.example.monew.domain.interest.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.monew.domain.interest.entity.Interest;
import com.example.monew.domain.interest.repository.InterestRepository;
import com.example.monew.domain.interest.repository.InterestSubscriptionRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class InterestSubscriptionConcurrencyTest {

  @Autowired
  private InterestSubscriptionService subscriptionService;

  @Autowired
  private InterestRepository interestRepository;

  @Autowired
  private InterestSubscriptionRepository subscriptionRepository;

  @Test
  @DisplayName("10개 스레드가 동시에 구독해도 subscriberCount는 정확히 10이다")
  void concurrentSubscribeIsAtomic() throws Exception {
    Interest interest = interestRepository.save(new Interest("Concurrent", List.of("k")));
    int threads = 10;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch ready = new CountDownLatch(threads);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(threads);
    AtomicInteger success = new AtomicInteger();

    try {
      for (int i = 0; i < threads; i++) {
        UUID userId = UUID.randomUUID();
        pool.submit(() -> {
          ready.countDown();
          try {
            start.await();
            subscriptionService.subscribe(interest.getId(), userId);
            success.incrementAndGet();
          } catch (Exception ignored) {
          } finally {
            done.countDown();
          }
        });
      }
      ready.await();
      start.countDown();
      done.await(10, TimeUnit.SECONDS);
    } finally {
      pool.shutdownNow();
    }

    Interest reloaded = interestRepository.findById(interest.getId()).orElseThrow();
    assertThat(success.get()).isEqualTo(threads);
    assertThat(reloaded.getSubscriberCount()).isEqualTo(threads);
    assertThat(subscriptionRepository.findAll()).hasSizeGreaterThanOrEqualTo(threads);
  }
}
