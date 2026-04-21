package com.example.monew.domain.article.batch.dto;

import java.util.List;

public record NaverApiResponse (
    List<NaverItem> items
) {
  public record NaverItem(
      String title,
      String originallink,
      String description,
      String pubDate
  ) {}
}