package com.example.monew.global.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) 
public class MDCLoggingFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    
    String requestId = UUID.randomUUID().toString().substring(0, 8);

    
    MDC.put("request_id", requestId);
    MDC.put("client_ip", getClientIp(httpRequest));

    
    httpResponse.setHeader("MoNew-Request-ID", requestId);

    try {
      log.info(">>> [Request] {} {}", httpRequest.getMethod(), httpRequest.getRequestURI());
      chain.doFilter(request, response);
    } finally {
      log.info("<<< [Response] Status: {}", httpResponse.getStatus());
      
      MDC.clear();
    }
  }

  private String getClientIp(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (ip == null || ip.isEmpty()) {
      ip = request.getRemoteAddr();
    }
    return ip;
  }
}