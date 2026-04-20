package com.example.monew.domain.interest.repository;

import com.example.monew.domain.interest.entity.Interest;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class InterestRepositoryImpl implements InterestRepositoryCustom {

  private final EntityManager em;

  public InterestRepositoryImpl(EntityManager em) {
    this.em = em;
  }

  @Override
  public CursorPage findByCursor(
      String keyword, String orderBy, String direction, UUID cursorId, int limit) {
    // Red 단계 스켈레톤 — 아래 슬라이스 테스트가 실패하도록 빈 결과 반환
    return new CursorPage(List.<Interest>of(), 0L, false);
  }
}
