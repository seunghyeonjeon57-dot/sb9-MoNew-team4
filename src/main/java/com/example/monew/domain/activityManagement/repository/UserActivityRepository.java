package com.example.monew.domain.activityManagement.repository;

import com.example.monew.domain.activityManagement.document.UserActivityDocument;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface UserActivityRepository extends MongoRepository<UserActivityDocument, UUID> {

   @Query(value = "{ '_id' :  ?0}", delete = true) //where user_id = :userId와 동일
   void deleteAllByUserId(UUID userId);
}
