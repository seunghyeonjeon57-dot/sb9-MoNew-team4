package com.example.monew.batch;
import com.example.monew.domain.article.batch.NewsCollector;
import com.example.monew.domain.article.batch.dto.NaverApiResponse;
import com.example.monew.domain.article.entity.ArticleEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
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
    // 1. 빌더가 Mock restTemplate을 뱉도록 먼저 훈련
    lenient().when(restTemplateBuilder.build()).thenReturn(restTemplate);

    // 2. 직접 생성자를 호출해서 주입합니다. (이래야 restTemplate이 null이 안 됩니다)
    newsCollector = new NewsCollector(restTemplateBuilder);

    // 3. 필드 주입
    ReflectionTestUtils.setField(newsCollector, "clientId", "test-id");
    ReflectionTestUtils.setField(newsCollector, "clientSecret", "test-secret");
  }

  @Test
  @DisplayName("네이버 뉴스 수집 성공 시 ArticleEntity 변환 테스트")
  void fetchNaverSuccessTest() {
    // 1. Given: 선우님의 NaverApiResponse 레코드 구조에 딱 맞춘 Mock 데이터
    NaverApiResponse.NaverItem mockItem = new NaverApiResponse.NaverItem(
        "<b>테스트</b> 제목",     // title
        "https://original.com", // originallink
        "뉴스 <b>요약</b>입니다.", // description
        "Thu, 23 Apr 2026 12:00:00 +0900" // pubDate (naverDateFormatter 대응)
    );
    NaverApiResponse mockResponse = new NaverApiResponse(List.of(mockItem));

    // restTemplate.exchange 호출 시 mockResponse를 반환하도록 설정
    // NaverApiResponse.class 타입을 정확히 명시해야 합니다.
    given(restTemplate.exchange(
        anyString(),            // 1. URL
        eq(HttpMethod.GET),     // 2. Method
        any(HttpEntity.class),  // 3. Request Entity (헤더 등)
        eq(NaverApiResponse.class) // 4. Response Type
    )).willReturn(ResponseEntity.ok(mockResponse));

    // 2. When
    List<ArticleEntity> result = newsCollector.fetchNaver("경제");

    // 3. Then: 데이터 변환 로직 검증
    assertThat(result).isNotEmpty();
    assertThat(result.get(0).getTitle()).isEqualTo("테스트 제목"); // cleanHtml 동작 확인
    assertThat(result.get(0).getSourceUrl()).isEqualTo("https://original.com");
    assertThat(result.get(0).getInterest()).isEqualTo("경제");
  }

  @Test
  @DisplayName("HTML 태그 제거")
  void cleanHtmlTest() {
    String dirtyText = "<b>안녕</b>";

    // 만약 cleanHtml이 인자를 2개 받는다면 뒤에 하나 더 추가해야 합니다.
    // 예: invokeMethod(대상, "메서드명", 인자1, 인자2);
    String actual = ReflectionTestUtils.invokeMethod(newsCollector, "cleanHtml", dirtyText);

    assertThat(actual).isEqualTo("안녕");
  }
}