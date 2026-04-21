package com.example.monew.domain.user.repository;

import com.example.monew.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryCustom {
  Optional<User> findActiveByEmail(String email);
  Optional<User> findActiveById(UUID id);
  List<User> findExpiredDeletedUsers(LocalDateTime threshold);
}