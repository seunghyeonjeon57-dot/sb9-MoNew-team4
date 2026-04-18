package com.example.monew.batch;

import com.example.monew.batch.dto.NaverApiResponse;
import com.example.monew.domain.article.entity.ArticleEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NewsCollectorTest {

  @InjectMocks
  private NewsCollector newsCollector;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(newsCollector, "clientId", "test-id");
    ReflectionTestUtils.setField(newsCollector, "clientSecret", "test-secret");
  }

  @Test
  @DisplayName("HTML 태그 제거")
  void cleanHtmlTest() {
    String dirtyText = "<b>안녕하세요</b> &lt;MoNew&gt; 입니다.";
    String expected = "안녕하세요 <MoNew> 입니다.";

    String actual = ReflectionTestUtils.invokeMethod(newsCollector, "cleanHtml", dirtyText);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @DisplayName("RSS 수집하고 ArticleEntity로 변환")
  void fetchRssTest() {
    String invalidUrl = "https://invalid-url.com/rss";

    List<ArticleEntity> result = newsCollector.fetchRss(invalidUrl, "테스트언론사", "경제");

    assertThat(result).isEmpty();
  }
}