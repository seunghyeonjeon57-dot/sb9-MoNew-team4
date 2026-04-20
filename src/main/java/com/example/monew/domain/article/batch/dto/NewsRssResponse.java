package com.example.monew.domain.article.batch.dto;

import java.util.List;

public record NewsRssResponse (
    List<RssItem> items
) {
  public record RssItem(
      String title,
      String link,
      String description,
      String pubDate
  ) {}
}
