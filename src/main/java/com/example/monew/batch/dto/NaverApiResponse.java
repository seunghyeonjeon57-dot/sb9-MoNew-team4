package com.example.monew.batch.dto;

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