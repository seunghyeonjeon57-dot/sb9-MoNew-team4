package com.example.monew.batch;

import com.example.monew.batch.dto.NaverApiResponse;
import com.example.monew.domain.article.entity.ArticleEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsCollector {

  @Value("${naver.client-id}") private String clientId;
  @Value("${naver.client-secret}") private String clientSecret;

  private final RestTemplate restTemplate = new RestTemplate();

  // 네이버 날짜 포맷 (Tue, 21 Apr 2026 15:00:00 +0900) 대응
  private final DateTimeFormatter naverDateFormatter = DateTimeFormatter.RFC_1123_DATE_TIME;

  public List<ArticleEntity> fetchNaver(String keyword) {
    String url = "https://openapi.naver.com/v1/search/news.json?query=" + keyword + "&display=10&sort=sim";

    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Naver-Client-Id", clientId);
    headers.set("X-Naver-Client-Secret", clientSecret);

    try {
      ResponseEntity<NaverApiResponse> response = restTemplate.exchange(
          url, HttpMethod.GET, new HttpEntity<>(headers), NaverApiResponse.class);

      return response.getBody().items().stream().map(item ->
          ArticleEntity.builder()
              .title(cleanHtml(item.title()))
              .summary(cleanHtml(item.description()))
              .sourceUrl(item.originallink())
              .source("네이버 뉴스")
              .publishDate(ZonedDateTime.parse(item.pubDate(), naverDateFormatter).toLocalDateTime())
              .interest(keyword)
              .build()
      ).toList();
    } catch (Exception e) {
      log.error("네이버 API 수집 실패: {}", e.getMessage());
      return List.of();
    }
  }

  public List<ArticleEntity> fetchRss(String rssUrl, String pressName, String interest) {
    try {
      SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URL(rssUrl)));
      return feed.getEntries().stream().map(entry ->
          ArticleEntity.builder()
              .title(cleanHtml(entry.getTitle()))
              .summary(entry.getDescription() != null ? cleanHtml(entry.getDescription().getValue()) : "")
              .sourceUrl(entry.getLink())
              .source(pressName)
              .publishDate(entry.getPublishedDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime())
              .interest(interest)
              .build()
      ).toList();
    } catch (Exception e) {
      log.error("{} RSS 수집 실패: {}", pressName, e.getMessage());
      return List.of();
    }
  }

  private String cleanHtml(String text) {
    return text == null ? "" : Jsoup.parse(text).text();
  }
}