package com.example.monew.domain.activityManagement.repository;

import com.example.monew.domain.activityManagement.document.UserActivityDocument;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserActivityRepository extends MongoRepository<UserActivityDocument, UUID>, UserActivityRepositoryCustom {
}
