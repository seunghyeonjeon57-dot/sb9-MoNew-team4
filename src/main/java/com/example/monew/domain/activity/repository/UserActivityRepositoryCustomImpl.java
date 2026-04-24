package com.example.monew.domain.activityManagement.repository;

import com.example.monew.domain.activityManagement.document.UserActivityDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class UserActivityRepositoryCustomImpl implements UserActivityRepositoryCustom{
  private final MongoTemplate mongoTemplate;

  @Override
  public long deleteAllByUserId(UUID userId) {
    Query query = new Query(Criteria.where("_id").is(userId));

    return mongoTemplate.remove(query, UserActivityDocument.class).getDeletedCount();
  }

  @Override
  public long softDeleteAllByUserId(UUID userId) {
    Query query = new Query(Criteria.where("_id").is(userId).and("deletedAt").isNull());

    Update update = new Update()
        .set("deletedAt", LocalDateTime.now());

    return mongoTemplate.updateFirst(query, update, UserActivityDocument.class).getModifiedCount();
  }
}
