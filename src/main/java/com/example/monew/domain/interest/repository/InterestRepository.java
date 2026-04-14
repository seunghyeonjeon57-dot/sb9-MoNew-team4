package com.example.monew.domain.interest.repository;

import com.example.monew.domain.interest.entity.Interest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InterestRepository
    extends JpaRepository<Interest, UUID>, JpaSpecificationExecutor<Interest> {

  List<Interest> findAllByIsDeletedFalse();

  boolean existsByNameAndIsDeletedFalse(String name);
}
