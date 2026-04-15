package com.example.monew.domain.interest.repository;

import com.example.monew.domain.interest.entity.Interest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterestRepository extends JpaRepository<Interest, UUID> {

  Optional<Interest> findByNameAndIsDeletedFalse(String name);

  Optional<Interest> findByIdAndIsDeletedFalse(UUID id);

  List<Interest> findAllByIsDeletedFalse();
}
