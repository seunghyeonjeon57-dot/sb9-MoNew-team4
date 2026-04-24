package com.example.monew.domain.activityManagement.repository;

import java.util.UUID;

public interface UserActivityRepositoryCustom {
  long deleteAllByUserId(UUID userId);
  long softDeleteAllByUserId(UUID userId);
}
