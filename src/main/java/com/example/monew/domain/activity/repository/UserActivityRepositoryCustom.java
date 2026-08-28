package com.example.monew.domain.activity.repository;

import java.util.Collection;
import java.util.UUID;

public interface UserActivityRepositoryCustom {
  long deleteAllByUserId(UUID userId);
  long deleteAllByUserIdIn(Collection<UUID> userIds);
  long softDeleteAllByUserId(UUID userId);
}
