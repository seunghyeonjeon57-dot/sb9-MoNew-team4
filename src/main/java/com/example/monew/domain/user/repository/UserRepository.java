package com.example.monew.domain.user.repository;

import com.example.monew.domain.user.entity.User;
import com.example.monew.domain.user.entity.type.UserStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> ,UserRepositoryCustom{
  boolean existsByEmail(String email);

  

  long countByStatus(UserStatus status);


}
