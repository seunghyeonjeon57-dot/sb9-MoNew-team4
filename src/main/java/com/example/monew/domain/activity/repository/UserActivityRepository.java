package com.example.monew.domain.activity.repository;

import com.example.monew.domain.activity.document.UserActivityDocument;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserActivityRepository extends MongoRepository<UserActivityDocument, UUID>, UserActivityRepositoryCustom {
}
