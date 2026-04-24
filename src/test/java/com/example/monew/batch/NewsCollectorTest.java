package com.example.monew.batch;
import com.example.monew.domain.article.batch.NewsCollector;
import com.example.monew.domain.article.batch.dto.NaverApiResponse;
import com.example.monew.domain.article.entity.ArticleEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class NewsCollectorTest {
  @Mock(answer = Answers.RETURNS_SELF)
  private RestTemplateBuilder restTemplateBuilder;

  @Mock
  private RestTemplate restTemplate;

  private NewsCollector newsCollector;

  @BeforeEach
  void setUp() {
    lenient().when(restTemplateBuilder.build()).thenReturn(restTemplate);

    newsCollector = new NewsCollector(restTemplateBuilder);

    ReflectionTestUtils.setField(newsCollector, "clientId", "test-id");
    ReflectionTestUtils.setField(newsCollector, "clientSecret", "test-secret");
  }

  @Test
  @DisplayName("네이버 뉴스 수집 성공 시 ArticleEntity 변환 테스트")
  void fetchNaverSuccessTest() {
    NaverApiResponse.NaverItem mockItem = new NaverApiResponse.NaverItem(
        "<b>테스트</b> 제목",
        "https://original.com",
        "뉴스 <b>요약</b>입니다.",
        "Thu, 23 Apr 2026 12:00:00 +0900"
    );
    NaverApiResponse mockResponse = new NaverApiResponse(List.of(mockItem));

    given(restTemplate.exchange(
        anyString(),
        eq(HttpMethod.GET),
        any(HttpEntity.class),
        eq(NaverApiResponse.class)
    )).willReturn(ResponseEntity.ok(mockResponse));

    List<ArticleEntity> result = newsCollector.fetchNaver("경제");

    assertThat(result).isNotEmpty();
    assertThat(result.get(0).getTitle()).isEqualTo("테스트 제목"); // cleanHtml 동작 확인
    assertThat(result.get(0).getSourceUrl()).isEqualTo("https://original.com");
    assertThat(result.get(0).getInterest()).isEqualTo("경제");
  }

  @Test
  @DisplayName("HTML 태그 제거")
  void cleanHtmlTest() {
    String dirtyText = "<b>안녕</b>";

    String actual = ReflectionTestUtils.invokeMethod(newsCollector, "cleanHtml", dirtyText);

    assertThat(actual).isEqualTo("안녕");
  }
}