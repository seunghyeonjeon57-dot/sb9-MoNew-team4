package com.example.monew.global.filter;




import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class MDCLoggingFilterTest {

  private final MDCLoggingFilter filter = new MDCLoggingFilter();

  @Test
  @DisplayName("요청 시 응답 헤더에 MoNew-Request-ID가 포함되어야 한다")
  void shouldAddRequestIdToResponseHeader() throws Exception {
    
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain filterChain = new MockFilterChain();

    
    filter.doFilter(request, response, filterChain);

    
    assertThat(response.getHeader("MoNew-Request-ID")).isNotNull();
    assertThat(MDC.get("request_id")).isNull(); 
  }
}