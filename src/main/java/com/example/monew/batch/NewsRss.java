package com.example.monew.batch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;

@ConfigurationProperties(prefix = "news") // 'news' 레벨에서 시작
@ConfigurationPropertiesBinding
@RequiredArgsConstructor
@Getter
public class NewsRss {
  private final Map<String, String> rss;

  public List<String> getUrlList() {
    return new ArrayList<>(rss.values());
  }
}