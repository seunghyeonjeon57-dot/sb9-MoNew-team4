package com.example.monew.domain.interest.repository;

import com.example.monew.domain.interest.entity.Interest;
import java.util.Optional;
import java.util.UUID;

public record InterestCursor(String sortValue, UUID id) {

  public static final String SUBSCRIBER_COUNT = "subscriberCount";
  public static final String NAME = "name";

  public static Optional<InterestCursor> decode(String raw) {
    if (raw == null || raw.isBlank()) {
      return Optional.empty();
    }
    int sep = raw.lastIndexOf('|');
    if (sep <= 0 || sep == raw.length() - 1) {
      return Optional.empty();
    }
    try {
      UUID id = UUID.fromString(raw.substring(sep + 1));
      return Optional.of(new InterestCursor(raw.substring(0, sep), id));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  public static String encode(Interest last, String sortField) {
    String value = SUBSCRIBER_COUNT.equals(sortField)
        ? String.valueOf(last.getSubscriberCount())
        : last.getName();
    return value + "|" + last.getId();
  }
}
