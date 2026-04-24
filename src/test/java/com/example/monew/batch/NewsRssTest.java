package com.example.monew.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.monew.domain.article.batch.NewsRss;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NewsRssTest {

  @Test
  @DisplayName("주소 변환 정상 작동")
  void URLChangeTest() {
    Map<String, String> testMap = Map.of(
        "naver", "https://news.naver.com/rss",
        "yonhap", "https://news.yonhap.com/rss"
    );
    NewsRss newsRss = new NewsRss(testMap);

    var urlList = newsRss.getUrlList();

    assertThat(urlList).hasSize(2);
    assertThat(urlList).contains("https://news.naver.com/rss", "https://news.yonhap.com/rss");
  }
}